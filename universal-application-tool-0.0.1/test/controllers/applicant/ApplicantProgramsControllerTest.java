package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import controllers.WithMockedProfiles;
import java.util.Locale;
import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class ApplicantProgramsControllerTest extends WithMockedProfiles {

  private Applicant currentApplicant;
  private ApplicantProgramsController controller;

  @Before
  public void setUp() {
    resetDatabase();
    controller = instanceOf(ApplicantProgramsController.class);
    currentApplicant = createApplicantWithMockedProfile();
  }

  @Test
  public void index_differentApplicant_returnsUnauthorizedResult() {
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.index(request, currentApplicant.id + 1).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void index_withNoPrograms_returnsEmptyResult() {
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).doesNotContain("program-card");
  }

  @Test
  public void index_withPrograms_returnsOnlyRelevantPrograms() {
    resourceCreator().insertActiveProgram("one");
    resourceCreator().insertActiveProgram("two");
    resourceCreator().insertDraftProgram("three");

    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
    assertThat(contentAsString(result)).doesNotContain("three");
  }

  @Test
  public void index_withProgram_includesApplyButtonWithRedirect() {
    Program program = resourceCreator().insertActiveProgram("program");

    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.view(currentApplicant.id, program.id).url());
  }

  @Test
  public void index_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(fakeRequest())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Obtener beneficios");
  }

  @Test
  public void index_missingTranslationForProgram_defaultsToEnglish() {
    resourceCreator().insertActiveProgram(Locale.forLanguageTag("es-US"), "A different language!");
    resourceCreator().insertActiveProgram("English program"); // Missing translation

    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(fakeRequest())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A different language!");
    assertThat(contentAsString(result)).contains("English program");
  }

  @Test
  public void view_includesApplyButton() {
    Program program = resourceCreator().insertActiveProgram("program");

    Request request = addCSRFToken(fakeRequest()).build();
    Result result =
        controller.view(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            routes.ApplicantProgramReviewController.preview(currentApplicant.id, program.id).url());
  }

  @Test
  public void view_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .view(fakeRequest().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_differentApplicant_returnsUnauthorizedResult() {
    Request request = addCSRFToken(fakeRequest()).build();
    Result result =
        controller.edit(request, currentApplicant.id + 1, 1L).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_withNewProgram_redirectsToFirstBlock() {
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withQuestion(testQuestionBank().applicantName())
            .build();

    Request request = addCSRFToken(fakeRequest()).build();
    Result result =
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "1")
                .url());
  }

  @Test
  public void edit_redirectsToFirstIncompleteBlock() {
    QuestionDefinition colorQuestion =
        testQuestionBank().applicantFavoriteColor().getQuestionDefinition();
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withQuestionDefinition(colorQuestion)
            .withBlock()
            .withQuestion(testQuestionBank().applicantAddress())
            .build();
    // Answer the color question
    Path colorPath = ApplicantData.APPLICANT_PATH.join("applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(
        currentApplicant.getApplicantData(), colorPath, "forest green");
    QuestionAnswerer.addMetadata(currentApplicant.getApplicantData(), colorPath, 456L, 12345L);
    currentApplicant.save();

    Request request = addCSRFToken(fakeRequest()).build();
    Result result =
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "2")
                .url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void edit_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    Program program = resourceCreator().insertActiveProgram("My Program");

    Request request = addCSRFToken(fakeRequest()).build();
    Result result =
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index(currentApplicant.id).url());
  }
}

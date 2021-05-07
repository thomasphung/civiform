package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramSummaryView;

/**
 * Controller for reviewing program responses for an applicant.
 *
 * <p>CAUTION: You must explicitly check the current profile so that an unauthorized user cannot
 * access another applicant's data!
 */
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      ApplicationRepository applicationRepository,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    Optional<String> banner = request.flash().get("banner");
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryData();
              String programTitle = roApplicantProgramService.getProgramTitle();
              return ok(
                  summaryView.render(
                      request,
                      applicantId,
                      programId,
                      programTitle,
                      summaryData,
                      messagesApi.preferred(request),
                      banner));
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> submit(Request request, long applicantId, long programId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> {
              return submit(applicantId, programId);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private CompletionStage<Result> submit(long applicantId, long programId) {
    CompletionStage<java.util.Optional<Application>> submitApp =
        applicationRepository.submitApplication(applicantId, programId);
    CompletionStage<ReadOnlyApplicantProgramService> service =
        applicantService.getReadOnlyApplicantProgramService(applicantId, programId);

    return submitApp.thenCombineAsync(
        service,
        (applicationMaybe, roApplicantProgramService) -> {
          if (applicationMaybe.isEmpty()) {
            Call reviewPage =
                routes.ApplicantProgramReviewController.review(applicantId, programId);
            return found(reviewPage).flashing("banner", "Error saving application.");
          }
          Call endOfProgramSubmission = routes.ApplicantProgramsController.index(applicantId);
          String programTitle = roApplicantProgramService.getProgramTitle();
          return found(endOfProgramSubmission)
              .flashing(
                  "banner", String.format("Successfully saved application: %s", programTitle));
        });
  }
}

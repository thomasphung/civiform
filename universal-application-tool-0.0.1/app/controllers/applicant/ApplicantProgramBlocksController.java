package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFile;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.AmazonS3Client;
import repository.StoredFileRepository;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ProgramBlockNotFoundException;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramNotFoundException;
import services.question.types.QuestionType;
import views.applicant.ApplicantProgramBlockEditView;

/**
 * Controller for handling an applicant filling out a single program. CAUTION: you must explicitly
 * check the current profile so that an unauthorized user cannot access another applicant's data!
 */
public final class ApplicantProgramBlocksController extends CiviFormController {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final FormFactory formFactory;
  private final AmazonS3Client amazonS3Client;
  private final StoredFileRepository storedFileRepository;
  private final ProfileUtils profileUtils;
  private final String baseUrl;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView,
      FormFactory formFactory,
      AmazonS3Client amazonS3Client,
      StoredFileRepository storedFileRepository,
      ProfileUtils profileUtils,
      Config configuration) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.amazonS3Client = checkNotNull(amazonS3Client);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.profileUtils = checkNotNull(profileUtils);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  @Secure
  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, false);
  }

  @Secure
  public CompletionStage<Result> review(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, true);
  }

  @Secure
  private CompletionStage<Result> editOrReview(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (block.isPresent()) {
                return ok(
                    editView.render(
                        ApplicantProgramBlockEditView.Params.builder()
                            .setRequest(request)
                            .setMessages(messagesApi.preferred(request))
                            .setApplicantId(applicantId)
                            .setProgramId(programId)
                            .setBlock(block.get())
                            .setInReview(inReview)
                            .setPreferredLanguageSupported(
                                roApplicantProgramService.preferredLanguageSupported())
                            .setAmazonS3Client(amazonS3Client)
                            .setBaseUrl(baseUrl)
                            .build()));
              } else {
                return notFound();
              }
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

  /**
   * This method is used by the file upload question. We let users directly upload files to S3
   * bucket from browsers. On success, users are redirected to this method. The redirect is a GET
   * method with file key in the query string. We parse and store them in the database for record
   * and redirect users to the next block or review page.
   */
  @Secure
  public CompletionStage<Result> updateFile(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (!block.isPresent() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              Optional<String> bucket = request.queryString("bucket");
              Optional<String> key = request.queryString("key");
              if (!bucket.isPresent() || !key.isPresent()) {
                return failedFuture(
                    new IllegalArgumentException("missing file key and bucket names"));
              }

              String applicantFileUploadQuestionKeyPath =
                  block.get().getQuestions().stream()
                      .filter(question -> question.getType().equals(QuestionType.FILEUPLOAD))
                      .findAny()
                      .get()
                      .createFileUploadQuestion()
                      .getFileKeyPath()
                      .toString();
              ImmutableMap<String, String> formData =
                  ImmutableMap.of(applicantFileUploadQuestionKeyPath, key.get());

              updateFileRecord(key.get());
              return applicantService.stageAndUpdateIfValid(
                  applicantId, programId, blockId, formData);
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            (errorAndROApplicantProgramService) -> {
              if (errorAndROApplicantProgramService.isError()) {
                errorAndROApplicantProgramService
                    .getErrors()
                    .forEach(e -> logger.error("Exception while updating applicant data", e));
                return supplyAsync(() -> badRequest("Unable to process this request"));
              }
              ReadOnlyApplicantProgramService roApplicantProgramService =
                  errorAndROApplicantProgramService.getResult();

              return update(
                  request, applicantId, programId, blockId, inReview, roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                } else if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                } else if (cause instanceof ProgramBlockNotFoundException) {
                  logger.error("Exception while updating applicant data", cause);
                  return badRequest("Unable to process this request");
                } else if (cause instanceof IllegalArgumentException) {
                  logger.error(cause.getMessage());
                  return badRequest();
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> {
              DynamicForm form = formFactory.form().bindFromRequest(request);
              ImmutableMap<String, String> formData = cleanForm(form.rawData());

              return applicantService.stageAndUpdateIfValid(
                  applicantId, programId, blockId, formData);
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            (errorAndROApplicantProgramService) -> {
              if (errorAndROApplicantProgramService.isError()) {
                errorAndROApplicantProgramService
                    .getErrors()
                    .forEach(e -> logger.error("Exception while updating applicant data", e));
                return supplyAsync(() -> badRequest("Unable to process this request"));
              }
              ReadOnlyApplicantProgramService roApplicantProgramService =
                  errorAndROApplicantProgramService.getResult();

              return update(
                  request, applicantId, programId, blockId, inReview, roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                } else if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                } else if (cause instanceof ProgramBlockNotFoundException) {
                  logger.error("Exception while updating applicant data", cause);
                  return badRequest("Unable to process this request");
                } else if (cause instanceof IllegalArgumentException) {
                  logger.error(cause.getMessage());
                  return badRequest();
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private CompletionStage<Result> update(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Optional<Block> thisBlockUpdatedMaybe = roApplicantProgramService.getBlock(blockId);
    if (thisBlockUpdatedMaybe.isEmpty()) {
      return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
    }
    Block thisBlockUpdated = thisBlockUpdatedMaybe.get();
    Messages applicantMessages = messagesApi.preferred(request);

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()) {
      return supplyAsync(
          () ->
              ok(
                  editView.render(
                      ApplicantProgramBlockEditView.Params.builder()
                          .setRequest(request)
                          .setMessages(applicantMessages)
                          .setApplicantId(applicantId)
                          .setProgramId(programId)
                          .setBlock(thisBlockUpdated)
                          .setInReview(inReview)
                          .setPreferredLanguageSupported(
                              roApplicantProgramService.preferredLanguageSupported())
                          .setAmazonS3Client(amazonS3Client)
                          .setBaseUrl(baseUrl)
                          .build())));
    }

    Optional<String> nextBlockIdMaybe =
        roApplicantProgramService.getBlockAfter(blockId).map(Block::getId);
    return nextBlockIdMaybe.isEmpty() || inReview
        ? supplyAsync(
            () -> redirect(routes.ApplicantProgramReviewController.review(applicantId, programId)))
        : supplyAsync(
            () ->
                redirect(
                    routes.ApplicantProgramBlocksController.edit(
                        applicantId, programId, nextBlockIdMaybe.get())));
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private void updateFileRecord(String key) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(key);
    storedFileRepository.insert(storedFile);
  }
}

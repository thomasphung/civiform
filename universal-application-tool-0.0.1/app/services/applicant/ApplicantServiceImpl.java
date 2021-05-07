package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import play.libs.concurrent.HttpExecutionContext;
import repository.UserRepository;
import services.ErrorAnd;
import services.Path;
import services.applicant.question.Scalar;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.ScalarType;

public class ApplicantServiceImpl implements ApplicantService {

  private final UserRepository userRepository;
  private final ProgramService programService;
  private final Clock clock;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ApplicantServiceImpl(
      UserRepository userRepository,
      ProgramService programService,
      Clock clock,
      HttpExecutionContext httpExecutionContext) {
    this.userRepository = checkNotNull(userRepository);
    this.programService = checkNotNull(programService);
    this.clock = checkNotNull(clock);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  @Override
  public CompletionStage<Applicant> createApplicant(long userId) {
    Applicant applicant = new Applicant();
    return userRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        userRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();

              return new ReadOnlyApplicantProgramServiceImpl(
                  applicant.getApplicantData(), programDefinition);
            },
            httpExecutionContext.current());
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      Application application) {
    return CompletableFuture.completedFuture(
        new ReadOnlyApplicantProgramServiceImpl(
            application.getApplicantData(), application.getProgram().getProgramDefinition()));
  }

  @Override
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId,
          long programId,
          String blockId,
          ImmutableMap<String, String> updateMap) {
    ImmutableSet<Update> updates =
        updateMap.entrySet().stream()
            .map(entry -> Update.create(Path.create(entry.getKey()), entry.getValue()))
            .collect(ImmutableSet.toImmutableSet());

    // Ensures updates do not collide with metadata scalars. "keyName[]" collides with "keyName".
    boolean updatePathsContainReservedKeys =
        updates.stream()
            .map(Update::path)
            .map(path -> path.isArrayElement() ? path.withoutArrayReference() : path)
            .anyMatch(path -> Scalar.getMetadataScalarKeys().contains(path.keyName()));
    if (updatePathsContainReservedKeys) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Path contained reserved scalar key"));
    }

    return stageAndUpdateIfValid(applicantId, programId, blockId, updates);
  }

  protected CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId, long programId, String blockId, ImmutableSet<Update> updates) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        userRepository.lookupApplicant(applicantId).toCompletableFuture();

    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<Applicant> applicantMaybe = applicantCompletableFuture.join();
              if (applicantMaybe.isEmpty()) {
                return CompletableFuture.completedFuture(
                    ErrorAnd.error(ImmutableSet.of(new ApplicantNotFoundException(applicantId))));
              }
              Applicant applicant = applicantMaybe.get();

              // Create a ReadOnlyApplicantProgramService and get the current block.
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();
              ReadOnlyApplicantProgramService readOnlyApplicantProgramServiceBeforeUpdate =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);
              Optional<Block> maybeBlockBeforeUpdate =
                  readOnlyApplicantProgramServiceBeforeUpdate.getBlock(blockId);
              if (maybeBlockBeforeUpdate.isEmpty()) {
                return CompletableFuture.completedFuture(
                    ErrorAnd.error(
                        ImmutableSet.of(new ProgramBlockNotFoundException(programId, blockId))));
              }
              Block blockBeforeUpdate = maybeBlockBeforeUpdate.get();

              UpdateMetadata updateMetadata = UpdateMetadata.create(programId, clock.millis());
              try {
                stageUpdates(
                    applicant.getApplicantData(), blockBeforeUpdate, updateMetadata, updates);
              } catch (UnsupportedScalarTypeException | PathNotInBlockException e) {
                return CompletableFuture.completedFuture(ErrorAnd.error(ImmutableSet.of(e)));
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);

              Optional<Block> blockMaybe = roApplicantProgramService.getBlock(blockId);
              if (blockMaybe.isPresent() && !blockMaybe.get().hasErrors()) {
                return userRepository
                    .updateApplicant(applicant)
                    .thenApplyAsync(
                        (finishedSaving) -> ErrorAnd.of(roApplicantProgramService),
                        httpExecutionContext.current());
              }

              return CompletableFuture.completedFuture(ErrorAnd.of(roApplicantProgramService));
            },
            httpExecutionContext.current());
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> relevantPrograms(long applicantId) {
    return userRepository.programsForApplicant(applicantId);
  }

  /**
   * In-place update of {@link ApplicantData}. Adds program id and timestamp metadata with updates.
   *
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   */
  private void stageUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    if (block.isEnumerator()) {
      stageEnumeratorUpdates(applicantData, block, updateMetadata, updates);
    } else {
      stageNormalUpdates(applicantData, block, updateMetadata, updates);
    }
  }

  /**
   * Stage updates for an enumerator.
   *
   * @throws PathNotInBlockException for updates that aren't {@link Scalar#ENTITY_NAME}, or {@link
   *     Scalar#DELETE_ENTITY}.
   */
  private void stageEnumeratorUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws PathNotInBlockException {
    ImmutableSet<Update> addsAndChanges = validateEnumeratorAddsAndChanges(block, updates);
    ImmutableSet<Update> deletes =
        updates.stream()
            .filter(
                update ->
                    update
                        .path()
                        .withoutArrayReference()
                        .equals(Path.empty().join(Scalar.DELETE_ENTITY)))
            .collect(ImmutableSet.toImmutableSet());

    // If there are more updates than there are adds/changes and deletes, throw
    Set<Update> unknownUpdates = Sets.difference(updates, Sets.union(addsAndChanges, deletes));
    if (!unknownUpdates.isEmpty()) {
      throw new PathNotInBlockException(block.getId(), unknownUpdates.iterator().next().path());
    }

    // Add and change entity names BEFORE deleting, because if deletes happened first, then changed
    // entity names
    // may not match the intended entities.
    for (Update update : addsAndChanges) {
      applicantData.putString(update.path().join(Scalar.ENTITY_NAME), update.value());
      writeMetadataForPath(update.path(), applicantData, updateMetadata);
    }

    ImmutableList<Integer> deleteIndices =
        deletes.stream()
            .map(Update::value)
            .map(Integer::valueOf)
            .collect(ImmutableList.toImmutableList());
    applicantData.deleteRepeatedEntities(
        block.getEnumeratorQuestion().getContextualizedPath(), deleteIndices);
  }

  /**
   * Validate that the updates to add or change enumerated entity names have the correct paths with
   * the right indices.
   */
  private ImmutableSet<Update> validateEnumeratorAddsAndChanges(
      Block block, ImmutableSet<Update> updates) {
    ImmutableSet<Update> entityUpdates =
        updates.stream()
            .filter(
                update ->
                    update
                        .path()
                        .withoutArrayReference()
                        .equals(
                            block
                                .getEnumeratorQuestion()
                                .getContextualizedPath()
                                .withoutArrayReference()))
            .collect(ImmutableSet.toImmutableSet());

    // Early return if it is empty.
    if (entityUpdates.isEmpty()) {
      return entityUpdates;
    }

    // Check that the entity updates have unique and consecutive indices. The indices should be
    // 0,...N-1 where N is entityUpdates.size() because all entity names are submitted in the form,
    // whether or not they actually changed.
    ImmutableSet<Integer> indices =
        entityUpdates.stream()
            .map(update -> update.path().arrayIndex())
            .collect(ImmutableSet.toImmutableSet());
    assert indices.size() == entityUpdates.size();
    assert indices.stream().min(Comparator.naturalOrder()).get() == 0;
    assert indices.stream().max(Comparator.naturalOrder()).get() == entityUpdates.size() - 1;

    return entityUpdates;
  }

  /**
   * In-place update of {@link ApplicantData}. Adds program id and timestamp metadata with updates.
   *
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   */
  private void stageNormalUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    ImmutableSet.Builder<Path> questionPaths = ImmutableSet.builder();
    ArrayList<Path> visitedPaths = new ArrayList<>();
    for (Update update : updates) {
      Path currentPath = update.path();

      // If we're updating an array we need to clear it first.
      if (currentPath.isArrayElement()
          && !visitedPaths.contains(currentPath.withoutArrayReference())) {
        visitedPaths.add(currentPath.withoutArrayReference());
        applicantData.maybeClearArray(currentPath);
      }

      ScalarType type =
          block
              .getScalarType(currentPath)
              .orElseThrow(() -> new PathNotInBlockException(block.getId(), currentPath));
      questionPaths.add(currentPath.parentPath());
      switch (type) {
        case STRING:
          applicantData.putString(currentPath, update.value());
          break;
        case LONG:
          applicantData.putLong(currentPath, update.value());
          break;
        default:
          throw new UnsupportedScalarTypeException(type);
      }
    }

    questionPaths
        .build()
        .forEach(path -> writeMetadataForPath(path, applicantData, updateMetadata));
  }

  private void writeMetadataForPath(Path path, ApplicantData data, UpdateMetadata updateMetadata) {
    data.putLong(path.join(Scalar.PROGRAM_UPDATED_IN), updateMetadata.programId());
    data.putLong(path.join(Scalar.UPDATED_AT), updateMetadata.updatedAt());
  }

  @AutoValue
  abstract static class UpdateMetadata {
    abstract long programId();

    abstract long updatedAt();

    static UpdateMetadata create(long programId, long updatedAt) {
      return new AutoValue_ApplicantServiceImpl_UpdateMetadata(programId, updatedAt);
    }
  }
}

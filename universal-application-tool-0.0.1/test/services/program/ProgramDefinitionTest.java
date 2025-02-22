package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Optional;
import models.Question;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;
import support.TestQuestionBank;

public class ProgramDefinitionTest extends WithPostgresContainer {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition.builder()
        .setId(123L)
        .setAdminName("Admin name")
        .setAdminDescription("Admin description")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
        .setExternalLink("")
        .addBlockDefinition(blockA)
        .build();
  }

  @Test
  public void getBlockDefinition_hasValue() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .addBlockDefinition(blockA)
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).hasValue(blockA);
  }

  @Test
  public void getBlockDefinition_outOfBounds_isEmpty() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).isEmpty();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .build();

    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
    assertThat(program.hasQuestion(questionC)).isFalse();
  }

  @Test
  public void localizedNameAndDescription() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "Applicant friendly name"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "English description"))
            .setExternalLink("")
            .build();

    assertThat(program.adminName()).isEqualTo("Admin name");
    assertThat(program.localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "Applicant friendly name"));
    assertThat(program.localizedDescription())
        .isEqualTo(LocalizedStrings.of(Locale.US, "English description"));

    assertThatThrownBy(() -> program.localizedName().get(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThatThrownBy(() -> program.localizedDescription().get(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThat(program.localizedName().getOrDefault(Locale.FRANCE))
        .isEqualTo("Applicant friendly name");
    assertThat(program.localizedDescription().getOrDefault(Locale.FRANCE))
        .isEqualTo("English description");
  }

  @Test
  public void updateNameAndDescription_replacesExistingValue() throws Exception {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "existing name"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "existing description"))
            .setExternalLink("")
            .build();

    program =
        program.toBuilder()
            .setLocalizedName(program.localizedName().updateTranslation(Locale.US, "new name"))
            .build();
    assertThat(program.localizedName().get(Locale.US)).isEqualTo("new name");

    program =
        program.toBuilder()
            .setLocalizedDescription(
                program.localizedDescription().updateTranslation(Locale.US, "new description"))
            .build();
    assertThat(program.localizedDescription().get(Locale.US)).isEqualTo("new description");
  }

  @Test
  public void getSupportedLocales_noQuestions_returnsOnlyLocalesSupportedByDisplayText() {
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(Locale.US, "English description", Locale.GERMAN, "test"))
            .setExternalLink("")
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_returnsLocalesSupportedByQuestionsAndText() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .build();
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .setExternalLink("")
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getAvailablePredicateQuestionDefinitions()
      throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition questionD = testQuestionBank.applicantSeason().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionD))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .addBlockDefinition(blockC)
            .setExternalLink("")
            .build();

    // blockA
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(1L)).isEmpty();
    // blockB
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(2L))
        .containsExactly(questionA);
    // blockC
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(3L))
        .containsExactly(questionA, questionB, questionC);
  }

  @Test
  public void
      getAvailablePredicateQuestionDefinitions_withRepeatedBlocks_onlyIncludesQuestionsWithSameEnumeratorId()
          throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition questionD =
        testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition questionE =
        testQuestionBank.applicantHouseholdMemberJobIncome().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockD =
        BlockDefinition.builder()
            .setId(4L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionD))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockE =
        BlockDefinition.builder()
            .setId(5L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionE))
            .setEnumeratorId(Optional.of(4L))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .setExternalLink("")
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .addBlockDefinition(blockC)
            .addBlockDefinition(blockD)
            .addBlockDefinition(blockE)
            .build();

    // blockA (applicantName)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(1L)).isEmpty();
    // blockB (applicantHouseholdMembers)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(2L))
        .containsExactly(questionA);
    // blockC (applicantHouseholdMembers.householdMemberName)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(3L))
        .containsExactly(questionA);
    // blockD (applicantHouseholdMembers.householdMemberJobs)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(4L))
        .containsExactly(questionA, questionC);
    // blockE (applicantHouseholdMembers.householdMemberJobs.householdMemberJobIncome)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(5L))
        .containsExactly(questionA, questionC);
  }

  @Test
  public void insertBlockDefinitionInTheRightPlace_repeatedBlock() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setName("new block")
            .setDescription("new block")
            .setId(100L)
            .setEnumeratorId(Optional.of(1L))
            .build();

    ProgramDefinition result =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition);

    assertThat(result.blockDefinitions()).hasSize(4);
    assertThat(result.getBlockDefinitionByIndex(0).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionCount()).isEqualTo(0);
    assertThat(result.getBlockDefinitionByIndex(3).get().isRepeated()).isFalse();
  }

  @Test
  public void moveBlock_up() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
  }

  @Test
  public void moveBlock_down() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(1L, ProgramDefinition.Direction.DOWN);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
  }

  @Test
  public void moveBlockUp_throwsForIllegalMove() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(predicateQuestion)
            .withBlock()
            .withPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThatExceptionOfType(IllegalBlockMoveException.class)
        .isThrownBy(() -> programDefinition.moveBlock(2L, ProgramDefinition.Direction.UP))
        .withMessage(
            "This move is not possible - it would move a block condition before the question it"
                + " depends on");
  }

  @Test
  public void moveBlockDown_throwsForIllegalMove() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block after a block that depends on it throws.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(predicateQuestion)
            .withBlock()
            .withPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThatExceptionOfType(IllegalBlockMoveException.class)
        .isThrownBy(() -> programDefinition.moveBlock(1L, ProgramDefinition.Direction.DOWN))
        .withMessage(
            "This move is not possible - it would move a block condition before the question it"
                + " depends on");
  }

  @Test
  public void hasValidPredicateOrdering() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(predicateQuestion)
            .withBlock()
            .withPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isTrue();

    programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withPredicate(predicate)
            .withBlock()
            .withQuestion(predicateQuestion)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isFalse();
  }

  @Test
  public void hasValidPredicateOrdering_returnsFalseIfQuestionsAreInSameBlockAsPredicate() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(predicateQuestion)
            .withPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isFalse();
  }
}

package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ProgramTest extends WithPostgresContainer {

  private ProgramRepository repo;

  @Before
  public void setupProgramRepository() {
    repo = instanceOf(ProgramRepository.class);
  }

  @Test
  public void canSaveProgram() throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(123L)
            .setName("question")
            .setDescription("applicant's name")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
            .build();

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setProgramQuestionDefinitions(
                ImmutableList.of(ProgramQuestionDefinition.create(questionDefinition)))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .build();
    Program program = new Program(definition);

    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Admin name");
    assertThat(found.getProgramDefinition().localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "ProgramTest"));
    assertThat(found.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("First Block");

    assertThat(
            found
                .getProgramDefinition()
                .blockDefinitions()
                .get(0)
                .programQuestionDefinitions()
                .get(0)
                .id())
        .isEqualTo(questionDefinition.getId());
  }

  @Test
  public void correctlySerializesDifferentQuestionTypes() throws UnsupportedQuestionTypeException {
    AddressQuestionDefinition addressQuestionDefinition =
        (AddressQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.ADDRESS)
                .setId(456L)
                .setName("address question")
                .setDescription("applicant's address")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your address?"))
                .build();
    NameQuestionDefinition nameQuestionDefinition =
        (NameQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.NAME)
                .setId(789L)
                .setName("name question")
                .setDescription("applicant's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
                .build();

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setProgramQuestionDefinitions(
                ImmutableList.of(
                    ProgramQuestionDefinition.create(addressQuestionDefinition),
                    ProgramQuestionDefinition.create(nameQuestionDefinition)))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .build();
    Program program = new Program(definition);
    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    ProgramQuestionDefinition addressQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(addressQuestion.id()).isEqualTo(addressQuestionDefinition.getId());
    ProgramQuestionDefinition nameQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(1);
    assertThat(nameQuestion.id()).isEqualTo(nameQuestionDefinition.getId());
  }

  @Test
  public void blockPredicates_serializedCorrectly() throws Exception {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Test predicates")
            .setDescription("set hide and deprecated optional")
            .setProgramQuestionDefinitions(ImmutableList.of())
            .setVisibilityPredicate(predicate)
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .build();
    Program program = new Program(definition);
    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().blockDefinitions()).hasSize(1);
    BlockDefinition block = found.getProgramDefinition().getBlockDefinition(1L);
    assertThat(block.visibilityPredicate()).hasValue(predicate);
    assertThat(block.optionalPredicate()).isEmpty();
  }

  @Test
  public void unorderedBlockDefinitions_getOrderedBlockDefinitionsOnSave() {
    ImmutableList<BlockDefinition> unorderedBlocks =
        ImmutableList.<BlockDefinition>builder()
            .add(
                BlockDefinition.builder()
                    .setId(1L)
                    .setName("enumerator")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantHouseholdMembers().getQuestionDefinition()))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(2L)
                    .setName("top level")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantEmail().getQuestionDefinition()))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(3L)
                    .setName("nested enumerator")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .applicantHouseholdMemberJobs()
                                .getQuestionDefinition()))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(4L)
                    .setName("repeated")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .applicantHouseholdMemberName()
                                .getQuestionDefinition()))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(5L)
                    .setName("nested repeated")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(3L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .applicantHouseholdMemberJobIncome()
                                .getQuestionDefinition()))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(6L)
                    .setName("top level 2")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantName().getQuestionDefinition()))
                    .build())
            .build();

    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(45832L)
            .setAdminName("test program")
            .setAdminDescription("test description")
            .setExternalLink("")
            .setLocalizedName(LocalizedStrings.withDefaultValue("test name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("test description"))
            .setBlockDefinitions(unorderedBlocks)
            .build();

    assertThat(programDefinition.hasOrderedBlockDefinitions()).isFalse();

    Program program = programDefinition.toProgram();
    program.save();

    assertThat(program.getProgramDefinition().hasOrderedBlockDefinitions()).isTrue();
  }
}

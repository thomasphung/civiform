package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import services.Path;

public class QuestionDefinitionTest {
  QuestionDefinitionBuilder builder;

  @Before
  public void setup() {
    builder =
        new QuestionDefinitionBuilder()
            .setVersion(1L)
            .setName("my name")
            .setPath(Path.create("my.path.name"))
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(ImmutableMap.of(Locale.US, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "help text"));
  }

  @Test
  public void testEquality_true() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    assertThat(question.equals(question)).isTrue();
    QuestionDefinition sameQuestion = new QuestionDefinitionBuilder(question).build();
    assertThat(question.equals(sameQuestion)).isTrue();
  }

  @Test
  public void testEquality_nullReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    assertThat(question.equals(null)).isFalse();
  }

  @Test
  public void testEquality_differentHelpTextReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionHelpText =
        new QuestionDefinitionBuilder(question)
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "different help text"))
            .build();
    assertThat(question.equals(differentQuestionHelpText)).isFalse();
  }

  @Test
  public void testEquality_differentTextReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionText =
        new QuestionDefinitionBuilder(question)
            .setQuestionText(ImmutableMap.of(Locale.US, "question text?"))
            .build();
    assertThat(question.equals(differentQuestionText)).isFalse();
  }

  @Test
  public void testEquality_differentQuestionTypeReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionType =
        new QuestionDefinitionBuilder(question).setQuestionType(QuestionType.ADDRESS).build();
    assertThat(question.equals(differentQuestionType)).isFalse();
  }

  @Test
  public void testEquality_differentIdReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition unpersistedQuestion = builder.clearId().build();
    assertThat(question.equals(unpersistedQuestion)).isFalse();

    QuestionDefinition differentId = builder.setId(456L).build();
    assertThat(question.equals(differentId)).isFalse();
  }

  @Test
  public void testEquality_differentVersionReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentVersion = builder.setVersion(2L).build();
    assertThat(question.equals(differentVersion)).isFalse();
  }

  @Test
  public void testEquality_differentNameReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    QuestionDefinition differentName = builder.setName("Different name").build();
    assertThat(question.equals(differentName)).isFalse();
  }

  @Test
  public void testEquality_differentDescriptionReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    QuestionDefinition differentDescription =
        builder.setDescription("Different description").build();
    assertThat(question.equals(differentDescription)).isFalse();
  }

  @Test
  public void newQuestionHasCorrectFields() throws TranslationNotFoundException {
    QuestionDefinition question =
        new TextQuestionDefinition(
            OptionalLong.of(123L),
            1L,
            "my name",
            Path.create("my.path.name"),
            "description",
            ImmutableMap.of(Locale.US, "question?"),
            ImmutableMap.of(Locale.US, "help text"));

    assertThat(question.getId()).isEqualTo(123L);
    assertThat(question.getVersion()).isEqualTo(1L);
    assertThat(question.getName()).isEqualTo("my name");
    assertThat(question.getPath().path()).isEqualTo("my.path.name");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText(Locale.US)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText(Locale.US)).isEqualTo("help text");
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    String questionPath = "question.path";
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L, "", Path.create(questionPath), "", ImmutableMap.of(), ImmutableMap.of());

    Throwable thrown = catchThrowable(() -> question.getQuestionText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown)
        .hasMessage(
            "Translation not found for Question at path: " + questionPath + "\n\tLocale: fr_FR");
  }

  @Test
  public void getQuestionHelpTextForUnknownLocale_throwsException() {
    String questionPath = "question.path";
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L,
            "",
            Path.create(questionPath),
            "",
            ImmutableMap.of(),
            ImmutableMap.of(Locale.US, "help text"));

    Throwable thrown = catchThrowable(() -> question.getQuestionHelpText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown)
        .hasMessage(
            "Translation not found for Question at path: " + questionPath + "\n\tLocale: fr_FR");
  }

  @Test
  public void getEmptyHelpTextForUnknownLocale_succeeds() throws TranslationNotFoundException {
    QuestionDefinition question =
        new TextQuestionDefinition(1L, "", Path.empty(), "", ImmutableMap.of(), ImmutableMap.of());
    assertThat(question.getQuestionHelpText(Locale.FRANCE)).isEqualTo("");
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question =
        new TextQuestionDefinition(1L, "", Path.empty(), "", ImmutableMap.of(), ImmutableMap.of());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void newQuestionHasStringScalar() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("path.to.question"),
            "description",
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(question.getScalars())
        .containsOnly(entry(Path.create("path.to.question"), ScalarType.STRING));
    assertThat(question.getScalarType(Path.create("path.to.question")).get())
        .isEqualTo(ScalarType.STRING);
    assertThat(question.getScalarType(Path.create("path.to.question")).get().getClassFor().get())
        .isEqualTo(String.class);
  }

  @Test
  public void newQuestionMissingScalar_returnsOptionalEmpty() {
    QuestionDefinition question =
        new TextQuestionDefinition(1L, "", Path.empty(), "", ImmutableMap.of(), ImmutableMap.of());
    assertThat(question.getScalarType(Path.create("notPresent"))).isEqualTo(Optional.empty());
  }

  @Test
  public void validateWellFormedQuestion_returnsNoErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("path"),
            "description",
            ImmutableMap.of(Locale.US, "question?"),
            ImmutableMap.of());
    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validateBadQuestion_returnsErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(-1L, "", Path.empty(), "", ImmutableMap.of(), ImmutableMap.of());
    assertThat(question.validate())
        .containsOnly(
            QuestionServiceError.of("invalid version: -1"),
            QuestionServiceError.of("blank name"),
            QuestionServiceError.of("invalid path pattern: ''"),
            QuestionServiceError.of("blank description"),
            QuestionServiceError.of("no question text"));
  }
}

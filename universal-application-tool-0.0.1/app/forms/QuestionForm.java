package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalInt;

import services.Path;
import services.question.InvalidQuestionTypeException;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;

public class QuestionForm {
  private String questionName;
  private String questionDescription;
  private Path questionPath;
  private String questionType;
  private String questionText;
  private String questionHelpText;
  private OptionalInt textMinLength;
  private OptionalInt textMaxLength;

  public QuestionForm() {
    questionName = "";
    questionDescription = "";
    questionPath = Path.empty();
    questionType = "TEXT";
    questionText = "";
    questionHelpText = "";
  }

  public QuestionForm(QuestionDefinition qd) {
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    questionPath = qd.getPath();
    questionType = qd.getQuestionType().name();

    try {
      questionText = qd.getQuestionText(Locale.US);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText(Locale.US);
    } catch (TranslationNotFoundException e) {
      questionHelpText = "Missing Text";
    }
  }

  public String getQuestionName() {
    return questionName;
  }

  public void setQuestionName(String questionName) {
    this.questionName = checkNotNull(questionName);
  }

  public String getQuestionDescription() {
    return questionDescription;
  }

  public void setQuestionDescription(String questionDescription) {
    this.questionDescription = checkNotNull(questionDescription);
  }

  public Path getQuestionPath() {
    return questionPath;
  }

  public void setQuestionPath(String questionPath) {
    this.questionPath = Path.create(checkNotNull(questionPath));
  }

  public String getQuestionType() {
    return questionType;
  }

  public void setQuestionType(String questionType) {
    this.questionType = checkNotNull(questionType);
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = checkNotNull(questionText);
  }

  public String getQuestionHelpText() {
    return questionHelpText;
  }

  public void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = checkNotNull(questionHelpText);
  }

  public OptionalInt getTextMinLength() {
    return textMinLength;
  }

  public void setTextMinLength(int textMinLength) {
    this.textMinLength = OptionalInt.of(textMinLength);
  }

  public OptionalInt getTextMaxLength() {
    return textMaxLength;
  }

  public void setTextMaxLength(int textMaxLength) {
    this.textMaxLength = OptionalInt.of(textMaxLength);
  }

  public QuestionDefinitionBuilder getBuilder() throws InvalidQuestionTypeException {
    ImmutableMap<Locale, String> questionTextMap =
        questionText.isEmpty() ? ImmutableMap.of() : ImmutableMap.of(Locale.US, questionText);
    ImmutableMap<Locale, String> questionHelpTextMap =
        questionHelpText.isEmpty()
            ? ImmutableMap.of()
            : ImmutableMap.of(Locale.US, questionHelpText);
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.of(questionType))
            .setName(questionName)
            .setPath(questionPath)
            .setDescription(questionDescription)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }
}

package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionType;

public class FileUploadQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> fileKeyValue;

  public FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // We can't predict ahead of time what the path will be.
    return ImmutableList.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a file upload question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getFileKeyPath());
  }

  public Optional<String> getFileKeyValue() {
    if (fileKeyValue != null) {
      return fileKeyValue;
    }

    fileKeyValue = applicantQuestion.getApplicantData().readString(getFileKeyPath());

    return fileKeyValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.FILEUPLOAD)) {
      throw new RuntimeException(
          String.format(
              "Question is not a FILEUPLOAD question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public FileUploadQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (FileUploadQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getFileKeyPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.FILE_KEY);
  }

  @Override
  public String getAnswerString() {
    return isAnswered() ? "-- FILE UPLOADED (click to download) --" : "-- NO FILE SELECTED --";
  }
}

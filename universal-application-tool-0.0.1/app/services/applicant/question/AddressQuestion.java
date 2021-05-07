package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionType;

public class AddressQuestion implements PresentsErrors {
  private static final String PO_BOX_REGEX =
      "(?i)(.*(P(OST|.)?\\s*((O(FF(ICE)?)?)?.?\\s*(B(IN|OX|.?)))+)).*";

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> streetValue;
  private Optional<String> line2Value;
  private Optional<String> cityValue;
  private Optional<String> stateValue;
  private Optional<String> zipValue;

  public AddressQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    AddressQuestionDefinition definition = getQuestionDefinition();
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getDisallowPoBox()) {
      Pattern poBoxPattern = Pattern.compile(PO_BOX_REGEX);
      Matcher poBoxMatcher1 = poBoxPattern.matcher(getStreetValue().orElse(""));
      Matcher poBoxMatcher2 = poBoxPattern.matcher(getLine2Value().orElse(""));

      if (poBoxMatcher1.matches() || poBoxMatcher2.matches()) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_NO_PO_BOX));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder()
        .addAll(getAddressErrors())
        .addAll(getStreetErrors())
        .addAll(getCityErrors())
        .addAll(getStateErrors())
        .addAll(getZipErrors())
        .build();
  }

  public ImmutableSet<ValidationErrorMessage> getAddressErrors() {
    // TODO: Implement address validation.
    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStreetErrors() {
    if (isStreetAnswered() && getStreetValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getCityErrors() {
    if (isCityAnswered() && getCityValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStateErrors() {
    // TODO: Validate state further.
    if (isStateAnswered() && getStateValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getZipErrors() {
    if (isZipAnswered()) {
      Optional<String> zipValue = getZipValue();
      if (zipValue.isEmpty()) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_ZIPCODE_REQUIRED));
      }

      Pattern pattern = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
      Matcher matcher = pattern.matcher(zipValue.get());
      if (!matcher.matches()) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE));
      }
    }

    return ImmutableSet.of();
  }

  public Optional<String> getStreetValue() {
    if (streetValue != null) {
      return streetValue;
    }

    streetValue = applicantQuestion.getApplicantData().readString(getStreetPath());
    return streetValue;
  }

  public Optional<String> getLine2Value() {
    if (line2Value != null) {
      return line2Value;
    }

    line2Value = applicantQuestion.getApplicantData().readString(getLine2Path());
    return line2Value;
  }

  public Optional<String> getCityValue() {
    if (cityValue != null) {
      return cityValue;
    }

    cityValue = applicantQuestion.getApplicantData().readString(getCityPath());
    return cityValue;
  }

  public Optional<String> getStateValue() {
    if (stateValue != null) {
      return stateValue;
    }

    stateValue = applicantQuestion.getApplicantData().readString(getStatePath());
    return stateValue;
  }

  public Optional<String> getZipValue() {
    if (zipValue != null) {
      return zipValue;
    }

    zipValue = applicantQuestion.getApplicantData().readString(getZipPath());
    return zipValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.ADDRESS)) {
      throw new RuntimeException(
          String.format(
              "Question is not an ADDRESS question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public AddressQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (AddressQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getStreetPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.STREET);
  }

  public Path getLine2Path() {
    return applicantQuestion.getContextualizedPath().join(Scalar.LINE2);
  }

  public Path getCityPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.CITY);
  }

  public Path getStatePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.STATE);
  }

  public Path getZipPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ZIP);
  }

  private boolean isStreetAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getStreetPath());
  }

  private boolean isLine2Answered() {
    return applicantQuestion.getApplicantData().hasPath(getLine2Path());
  }

  private boolean isCityAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getCityPath());
  }

  private boolean isStateAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getStatePath());
  }

  private boolean isZipAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getZipPath());
  }

  /**
   * Returns true if any one of the address fields is answered. Returns false if all are not
   * answered.
   */
  @Override
  public boolean isAnswered() {
    return isStreetAnswered()
        || isLine2Answered()
        || isCityAnswered()
        || isStateAnswered()
        || isZipAnswered();
  }

  @Override
  public String getAnswerString() {
    String line1 = getStreetValue().orElse("");
    String[] parts = {
      getCityValue().orElse(""), getStateValue().orElse(""), getZipValue().orElse("")
    };
    String line2 =
        Arrays.stream(parts).filter(part -> part.length() > 0).collect(Collectors.joining(", "));

    String[] ret = {line1, line2};
    return Arrays.stream(ret).filter(part -> part.length() > 0).collect(Collectors.joining("\n"));
  }
}

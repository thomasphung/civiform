package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class AddressQuestionTest {

  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final AddressQuestionDefinition noPoBoxAddressQuestionDefinition =
      new AddressQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(true));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            addressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    AddressQuestion addressQuestion = new AddressQuestion(applicantQuestion);

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withValidApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            addressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "PO Box 123",
        "Line 2",
        "Seattle",
        "WA",
        "98101");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("PO Box 123");
    assertThat(addressQuestion.getLine2Value().get()).isEqualTo("Line 2");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }

  @Test
  public void withInvalidApplicantData_missingRequiredFields() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "", "", "", "", "");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isTrue();
    assertThat(addressQuestion.getStreetErrors())
        .contains(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED));
    assertThat(addressQuestion.getCityErrors())
        .contains(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED));
    assertThat(addressQuestion.getStateErrors())
        .contains(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED));
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_ZIPCODE_REQUIRED));
  }

  @Test
  @Parameters({"not a zip code", "123456789", "123ab"})
  public void withInvalidApplicantData_invalidZipCode(String zipValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "123 A St",
        "Unit B",
        "Seattle",
        "WA",
        zipValue);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isTrue();
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE));
    assertThat(addressQuestion.getStreetErrors()).isEmpty();
    assertThat(addressQuestion.getCityErrors()).isEmpty();
    assertThat(addressQuestion.getStateErrors()).isEmpty();
  }

  @Test
  @Parameters({"123 A St", "123 Boxpo Ave", "12345", "1 Box Blvd"})
  public void withNoPoBoxAllowed_withValidApplicantData_passesValidation(String streetValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        "Unit B",
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({
    "PO Box 123,Line 2",
    "PO box 123,Line 2",
    "pO Box 123,Line 2",
    "po box 123,Line 2",
    "P.O. Box 123,Line 2",
    "p.o. box 123,Line 2",
    "My P.O. Box ABC,Line 2",
    "po-box 555,Line 2",
    "Sesame St,PO Box 123"
  })
  public void withNoPoBoxAllowed_withInvalidApplicantData_failsValidation(
      String streetValue, String line2Value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        line2Value,
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_NO_PO_BOX));
  }
}

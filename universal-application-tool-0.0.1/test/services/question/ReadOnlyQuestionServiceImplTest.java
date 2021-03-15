package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import org.junit.Test;
import services.Path;

public class ReadOnlyQuestionServiceImplTest {
  // The tests mimic that the persisted questions are read into ReadOnlyQuestionService.
  // Therefore, question ids cannot be empty.
  NameQuestionDefinition nameQuestion =
      new NameQuestionDefinition(
          OptionalLong.of(123L),
          1L,
          "applicant name",
          Path.create("applicant.name"),
          "The name of the applicant",
          ImmutableMap.of(Locale.US, "What is your name?"),
          ImmutableMap.of());
  AddressQuestionDefinition addressQuestion =
      new AddressQuestionDefinition(
          OptionalLong.of(456L),
          1L,
          "applicant addresss",
          Path.create("applicant.address"),
          "The address of the applicant",
          ImmutableMap.of(Locale.US, "What is your address?"),
          ImmutableMap.of());
  QuestionDefinition basicQuestion =
      new TextQuestionDefinition(
          OptionalLong.of(789L),
          1L,
          "applicant's favorite color",
          Path.create("applicant.favoriteColor"),
          "The favorite color of the applicant",
          ImmutableMap.of(Locale.US, "What is your favorite color?"),
          ImmutableMap.of());

  private Path invalidPath = Path.create("invalid.path");

  private ImmutableList<QuestionDefinition> questions =
      ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);

  private ReadOnlyQuestionService service = new ReadOnlyQuestionServiceImpl(questions);

  private ReadOnlyQuestionService emptyService =
      new ReadOnlyQuestionServiceImpl(ImmutableList.of());

  @Test
  public void getAll_returnsEmpty() {
    assertThat(emptyService.getAllQuestions()).isEmpty();
    assertThat(emptyService.getAllScalars()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getAllScalars() {
    ImmutableMap.Builder<Path, ScalarType> scalars = ImmutableMap.builder();
    scalars.putAll(nameQuestion.getScalars());
    scalars.putAll(addressQuestion.getScalars());
    scalars.putAll(basicQuestion.getScalars());
    assertThat(service.getAllScalars()).isEqualTo(scalars.build());
  }

  @Test
  public void getPathScalars_forQuestion() throws InvalidPathException {
    ImmutableMap<Path, ScalarType> result =
        service.getPathScalars(Path.create("applicant.address"));
    ImmutableMap<Path, ScalarType> expected = addressQuestion.getScalars();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forScalar() throws InvalidPathException {
    ImmutableMap<Path, ScalarType> result =
        service.getPathScalars(Path.create("applicant.address.city"));
    ImmutableMap<Path, ScalarType> expected =
        ImmutableMap.of(Path.create("applicant.address.city"), ScalarType.STRING);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forInvalidPath() {
    assertThatThrownBy(() -> service.getPathScalars(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath.path());
  }

  @Test
  public void getPathType_forInvalidPath() {
    assertThat(service.getPathType(invalidPath)).isEqualTo(PathType.NONE);
  }

  @Test
  public void getPathType_forQuestion() {
    assertThat(service.getPathType(Path.create("applicant.favoriteColor")))
        .isEqualTo(PathType.QUESTION);
  }

  @Test
  public void getPathType_forScalar() {
    assertThat(service.getPathType(Path.create("applicant.name.first"))).isEqualTo(PathType.SCALAR);
  }

  @Test
  public void getQuestionDefinition_forInvalidPath() {
    assertThatThrownBy(() -> service.getQuestionDefinition(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath.path());
  }

  @Test
  public void getQuestionDefinition_forQuestion() throws InvalidPathException {
    assertThat(service.getQuestionDefinition("applicant.address")).isEqualTo(addressQuestion);
  }

  @Test
  public void getQuestionDefinition_forScalar() throws InvalidPathException {
    assertThat(service.getQuestionDefinition("applicant.name.first")).isEqualTo(nameQuestion);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    assertThat(service.getQuestionDefinition(123L)).isEqualTo(nameQuestion);
  }

  @Test
  public void isValid_returnsFalseForInvalid() {
    assertThat(service.isValid(Path.create("invalidPath"))).isFalse();
  }

  @Test
  public void isValid_returnsFalseWhenEmpty() {
    assertThat(emptyService.isValid(Path.create("invalidPath"))).isFalse();
  }

  @Test
  public void isValid_returnsTrueForQuestion() {
    assertThat(service.isValid(Path.create("applicant.name"))).isTrue();
  }

  @Test
  public void isValid_returnsTrueForScalar() {
    assertThat(service.isValid(Path.create("applicant.name.first"))).isTrue();
  }
}

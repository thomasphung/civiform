package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@JsonDeserialize(builder = AutoValue_QuestionOption.Builder.class)
@AutoValue
public abstract class QuestionOption {

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("localizedOptionText")
  public abstract LocalizedStrings optionText();

  /**
   * Create a QuestionOption, used for JSON mapping to account for the legacy `optionText`.
   *
   * <p>Legacy QuestionOptions from before early May 2021 will not have `localizedOptionText`.
   */
  @JsonCreator
  public static QuestionOption jsonCreator(
      @JsonProperty("id") long id,
      @JsonProperty("localizedOptionText") LocalizedStrings localizedOptionText,
      @JsonProperty("optionText") ImmutableMap<Locale, String> legacyOptionText) {
    if (localizedOptionText != null) {
      return QuestionOption.create(id, localizedOptionText);
    }
    return QuestionOption.create(id, LocalizedStrings.create(legacyOptionText));
  }

  /** Create a QuestionOption. */
  public static QuestionOption create(long id, LocalizedStrings optionText) {
    return QuestionOption.builder().setId(id).setOptionText(optionText).build();
  }

  public LocalizedQuestionOption localize(Locale locale) {
    if (!optionText().hasTranslationFor(locale)) {
      throw new RuntimeException(
          String.format("Locale %s not supported for question option %s", locale, this));
    }

    return localizeOrDefault(locale);
  }

  /**
   * Localize this question option for the given locale. If we cannot localize, use the default
   * locale.
   */
  public LocalizedQuestionOption localizeOrDefault(Locale locale) {
    try {
      String localizedText = optionText().get(locale);
      return LocalizedQuestionOption.create(id(), localizedText, locale);
    } catch (TranslationNotFoundException e) {
      return LocalizedQuestionOption.create(
          id(), optionText().getDefault(), LocalizedStrings.DEFAULT_LOCALE);
    }
  }

  public abstract Builder toBuilder();

  public static QuestionOption.Builder builder() {
    return new AutoValue_QuestionOption.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("localizedOptionText")
    public abstract Builder setOptionText(LocalizedStrings optionText);

    public abstract QuestionOption build();
  }
}

package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;

public interface PresentsErrors {
  /** Returns true if values do not meet conditions defined by admins. */
  boolean hasQuestionErrors();

  /** Returns a set of {@link ValidationErrorMessage}s related to conditions defined by admins. */
  ImmutableSet<ValidationErrorMessage> getQuestionErrors();

  /**
   * Returns true if there is any type specific errors. The validation does not consider
   * admin-defined conditions.
   */
  boolean hasTypeSpecificErrors();

  /**
   * Returns a set of {@link ValidationErrorMessage}s to be shown to the applicant. These errors are
   * inherent to the question type itself - for example, an applicant providing a zip code with
   * letters would be a type error for an address question.
   */
  ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors();

  /**
   * Returns true if the question has been answered by the applicant, even if that answer was blank.
   * In general, if a question is not answered, it cannot have errors associated with it.
   */
  boolean isAnswered();

  /** Returns the answer as a text string. */
  String getAnswerString();

  /** Return every path used by this question. */
  ImmutableList<Path> getAllPaths();
}

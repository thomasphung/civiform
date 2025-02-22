package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;

@AutoValue
public abstract class PredicateDefinition {

  @JsonCreator
  public static PredicateDefinition create(
      @JsonProperty("rootNode") PredicateExpressionNode rootNode,
      @JsonProperty("action") PredicateAction action) {
    return new AutoValue_PredicateDefinition(rootNode, action);
  }

  @JsonProperty("rootNode")
  public abstract PredicateExpressionNode rootNode();

  @JsonProperty("action")
  public abstract PredicateAction action();

  @JsonIgnore
  @Memoized
  public ImmutableSet<Long> getQuestions() {
    return rootNode().getQuestions();
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/322): Override toString method and/or add a
  //  different getDisplayString method to pretty print a predicate definition for an admin.
}

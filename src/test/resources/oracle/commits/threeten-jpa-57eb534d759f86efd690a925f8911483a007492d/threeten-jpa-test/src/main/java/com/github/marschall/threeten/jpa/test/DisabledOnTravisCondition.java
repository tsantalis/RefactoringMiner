package com.github.marschall.threeten.jpa.test;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class DisabledOnTravisCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult NOT_ANNOTATED =
          ConditionEvaluationResult.enabled("@DisabledOnTravis is not present");

  private static final ConditionEvaluationResult NOT_TRAVIS_CI =
          ConditionEvaluationResult.enabled("Not on TravisCI");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<AnnotatedElement> element = context.getElement();
    Optional<DisabledOnTravis> disabled = findAnnotation(element, DisabledOnTravis.class);
    if (disabled.isPresent()) {
      if (Travis.isTravis()) {
        String reason = element.get() + " is disabled on TravisCI";
        return ConditionEvaluationResult.disabled(reason);
      } else {
        return NOT_TRAVIS_CI;
      }
    }

    return NOT_ANNOTATED;
  }

}

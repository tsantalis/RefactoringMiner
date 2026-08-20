package com.github.marschall.threeten.jpa.test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Prevents a test from running on travis.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(DisabledOnTravisCondition.class)
public @interface DisabledOnTravis {

}

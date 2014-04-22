package org.apache.sling.models.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Same meaning as {@link javax.inject.Inject} but only allowed on annotations.
 */
@Target({ ANNOTATION_TYPE })
@Retention(RUNTIME)
@Documented
public @interface ModelInject {

}

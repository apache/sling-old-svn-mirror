package org.apache.sling.validation.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author karolis.mackevicius@netcentric.biz
 * @since 02/04/17
 */
@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface ChildrenValidator {

    String[] properties();

    String nameRegex();
}

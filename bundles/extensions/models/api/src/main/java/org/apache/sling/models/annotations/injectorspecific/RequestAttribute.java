package org.apache.sling.models.annotations.injectorspecific;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.sling.models.annotations.ModelInject;
import org.apache.sling.models.annotations.Source;

/**
 * Annotation to be used on either methods or fields to let Sling Models inject a request attribute)
 *
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@ModelInject
@Source("request-attributes")
public @interface RequestAttribute {
    
    /**
     * Specifies the name of the request attribute. If empty or not set, then the name is derived from the method or field.
     */
    public String name() default "";
    
    /**
     * If set to true, the model can be instantiated even if there is no request attribute with the given name found. Default
     * = false.
     */
    public boolean optional() default false;
}

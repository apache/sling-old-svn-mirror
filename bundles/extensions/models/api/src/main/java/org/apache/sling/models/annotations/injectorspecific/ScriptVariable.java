package org.apache.sling.models.annotations.injectorspecific;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.sling.models.annotations.ModelInject;
import org.apache.sling.models.annotations.Source;

/**
 * Annotation to be used on either methods or fields to let Sling Models inject a script variable (from the {@link org.apache.sling.api.scripting.SlingBindings})
 *
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@ModelInject
@Source("script-bindings")
public @interface ScriptVariable {
    /**
     * Specifies the name of the script variable. If empty or not set, then the name is derived from the method or field.
     */
    public String name() default "";
    
    /**
     * If set to true, the model can be instantiated even if there is no OSGi service implementation available. Default
     * = false.
     */
    public boolean optional() default false;
}

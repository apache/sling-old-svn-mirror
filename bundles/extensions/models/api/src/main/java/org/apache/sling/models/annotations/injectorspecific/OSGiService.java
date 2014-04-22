package org.apache.sling.models.annotations.injectorspecific;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.sling.models.annotations.ModelInject;
import org.apache.sling.models.annotations.Source;

/**
 * Annotation to be used on either methods or fields to let Sling Models inject an OSGi service
 *
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@ModelInject
@Source("osgi-services")
public @interface OSGiService {
    /**
     * specifies the RFC 1960-based filter string, which is evaluated when retrieving the service. If empty string or left out, then no filtering is being performed.
     * 
     * @see "Core Specification, section 5.5, for a description of the filter string
     * @see <a href="http://www.ietf.org/rfc/rfc1960.txt">RFC 1960</a>
     */
    public String filter() default "";

    /**
     * If set to true, the model can be instantiated even if there is no OSGi service implementation available. Default
     * = false.
     */
    public boolean optional() default false;
}

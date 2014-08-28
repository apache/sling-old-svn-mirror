package org.apache.sling.models.factory;

import java.util.Collection;


/**
 * Exception which is triggered whenever a Sling Model cannot be instanciated due to some validation errors (i.e. required fields/methods could not be injected).
 * @see ModelFactory
 *
 */
public class NoInjectorFoundException extends RuntimeException {
    private static final long serialVersionUID = 7870762030809272254L;
    
    private final Collection<InjectionContext> contexts;
    
    public NoInjectorFoundException(String message, Collection<InjectionContext> contexts) {
        super(message);
        this.contexts = contexts;
    }

    public Collection<InjectionContext> getContexts() {
        return contexts;
    }
}

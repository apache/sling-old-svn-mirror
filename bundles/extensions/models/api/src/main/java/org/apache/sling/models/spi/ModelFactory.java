package org.apache.sling.models.spi;

/**
 * The ModelFactory instantiates Sling Model classes similar to adaptTo but is allowed to throw an exception in case
 * instantiation fails for some reason.
 *
 */
public interface ModelFactory {
    /**
     * Tries to instantiate the given Sling Model class from the given adaptable
     * @param adaptable the adaptable to use to instantiate the Sling Model Class
     * @param type the class to instantiate
     * @return a new instance for the required model
     * @throws ValidationException in case required fields/methods could not be injected
     * @throws InvalidAdaptableException in case that given class cannot be instantiated from the given adaptable (different adaptable on the model annotation)
     * @throws IllegalStateException in case the instantiation fails for some other reason (exception in post-construct method, invalid constructor)
     * @throws IllegalArgumentException if the given class does not carry a model annotation at all
     */
    public <ModelType> ModelType createModel(Object adaptable, Class<ModelType> type) 
            throws ValidationException, InvalidAdaptableException, IllegalStateException;
}

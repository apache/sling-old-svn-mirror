package org.apache.sling.models.factory;


/**
 * The ModelFactory instantiates Sling Model classes similar to adaptTo but is allowed to throw an exception in case
 * instantiation fails for some reason.
 *
 */
public interface ModelFactory {
    /**
     * Instantiates the given Sling Model class from the given adaptable
     * @param adaptable the adaptable to use to instantiate the Sling Model Class
     * @param type the class to instantiate
     * @return a new instance for the required model (never null)
     * @throws NoInjectorFoundException in case no injector was able to inject some required values with the given types
     * @throws InvalidAdaptableException in case the given class cannot be instantiated from the given adaptable (different adaptable on the model annotation)
     * @throws InvalidModelException in case the model could not be instanciated because reflection failed, no valid constructor was found or post-construct has thrown an error
     */
    public <ModelType> ModelType createModel(Object adaptable, Class<ModelType> type) throws NoInjectorFoundException, InvalidAdaptableException, InvalidModelException; 
}

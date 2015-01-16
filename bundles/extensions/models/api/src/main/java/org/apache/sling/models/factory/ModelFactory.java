/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.models.factory;


/**
 * The ModelFactory instantiates Sling Model classes similar to adaptTo but is allowed to throw an exception in case
 * instantiation fails for some reason.
 *
 */
public interface ModelFactory {
    /**
     * Instantiates the given Sling Model class from the given adaptable.
     * @param adaptable the adaptable to use to instantiate the Sling Model Class
     * @param type the class to instantiate
     * @return a new instance for the required model (never null)
     * @throws MissingElementsException in case no injector was able to inject some required values with the given types
     * @throws InvalidAdaptableException in case the given class cannot be instantiated from the given adaptable (different adaptable on the model annotation)
     * @throws ModelClassException in case the model could not be instanciated because model annotation was missing, reflection failed, no valid constructor was found or post-construct has thrown an error
     * @throws InvalidValidationModelException in case an invalid validation model was found
     * @throws InvalidResourceException in case the resource (for the Sling Model) could not be validated through Sling Validation
     */
    public <ModelType> ModelType createModel(Object adaptable, Class<ModelType> type) throws MissingElementsException,
            InvalidAdaptableException, ModelClassException, InvalidValidationModelException, InvalidResourceException;

    /**
     * 
     * @param adaptable the adaptable to check
     * @param type the class to check
     * @return false in case the given class can not be created from the given adaptable
     * @throws ModelClassException in case no class with the Model annotation adapts to the requested type
     */
    public boolean canCreateFromAdaptable(Object adaptable, Class<?> type) throws ModelClassException;

    /**
     * 
     * @param adaptable the adaptable to check
     * @param type the class to check
     * @return false in case no class with the Model annotation adapts to the requested type
     * 
     * @see org.apache.sling.models.annotations.Model
     */
    public boolean isModelClass(Object adaptable, Class<?> type);
}

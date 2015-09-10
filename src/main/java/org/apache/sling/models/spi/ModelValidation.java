/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.spi;

import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.models.factory.ValidationException;

import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface ModelValidation {

    /**
     * Triggers validation for the given model on the given adaptable
     * @param adaptable the adaptable about to be used instantiate the Sling Model Class
     * @param modelClass the class of the model which is about to be instantiated
     * @param required if {@code true} validation fails even if validation model can't be found.
     * @return {@code null} if validation was successful, otherwise either {@link ValidationException} 
     * in case validation could not be performed for some reason (e.g. no validation information available)
     * or {@link InvalidModelException} in case the given model type could not be validated through the {@link ModelValidation}.
     */
    public abstract <ModelType> RuntimeException validate(Object adaptable, Class<ModelType> modelClass, boolean required) throws ValidationException, InvalidModelException;

}

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
package org.apache.sling.models.impl.validation;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.impl.Result;

public interface ModelValidation {

    /**
     * Calls the Sling Validation for the given resource.
     * @param resource
     * @param required if {@code true} validation fails even if no appropriate validation model could be found.
     * @return {@code true} if validation was successful, otherwise {@code false}
     */
    public abstract <ModelType> boolean validate(Resource resource, boolean required, Result<ModelType> result);

}

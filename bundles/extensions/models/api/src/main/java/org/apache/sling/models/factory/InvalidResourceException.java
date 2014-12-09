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
package org.apache.sling.models.factory;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.validation.api.ValidationResult;

/**
 * Thrown in case Sling Validation detected an invalid resource upon which the model should be instanciated.
 * @see <a href="http://sling.apache.org/documentation/bundles/validation.html">Sling Validation</a>
 * @see ModelFactory
 */
public class InvalidResourceException extends RuntimeException {
    private static final long serialVersionUID = 366657841414210438L;
    private final ValidationResult result;
    private final String path;

    public InvalidResourceException(ValidationResult result, String path) {
        if (result.isValid()) {
            throw new IllegalArgumentException("Could not create a validator exception from a valid validation result!");
        }
        this.path = path;
        this.result = result;
    }
    
    /**
     * 
     * @return the underlying {@link ValidationResult}
     */
    public ValidationResult getResult() {
        return result;
    }
    
    /**
     * 
     * @return the path of the resource which was considered invalid
     */
    public String getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder("Validation errors for ");
        builder.append("'" + path +"':");
        for (Entry<String, List<String>> entry : result.getFailureMessages().entrySet()) {
            builder.append("\n" + entry.getKey() + ":" + StringUtils.join(entry.getValue(), "\n\t"));
        }
        return builder.toString();
    }
}

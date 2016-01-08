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
package org.apache.sling.models.validation;

import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;

/**
 * Exception embedding a {@link ValidationResult} from Sling Validation.
 *
 */
public class InvalidResourceException extends InvalidModelException {

    /**
     * 
     */
    private static final long serialVersionUID = -5134512515466089688L;
    private final ValidationResult result;
    private final String path;
    
    public InvalidResourceException(String message, ValidationResult result, String path) {
        super(message);
        this.result = result;
        this.path = path;
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

    /**
     * This is not the regular {@link Exception#getMessage()} as it requires an additional resourceBundle parameter to look up the localized message.
     * @param resourceBundle
     * @return the localized validation messages bound to the {@link ValidationResult} wrapped by this exception
     */
    public String getMessage(@Nonnull ResourceBundle resourceBundle) {
        StringBuilder builder = new StringBuilder("Validation errors for ");
        builder.append("'" + path +"':");
        for (ValidationFailure failure : result.getFailures()) {
            builder.append("\n" + failure.getLocation() + ":" + failure.getMessage(resourceBundle) + "\n\t");
        }
        return builder.toString();
    }
}

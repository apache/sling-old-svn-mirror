/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.postprocessor;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;

/** Exception embedding a {@link ValidationResult} from Sling Validation. */
public class InvalidResourcePostProcessorException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 213928457248325245L;
    private final @Nonnull ValidationResult result;
    private final @Nonnull ResourceBundle resourceBundle;
    
    private static final String KEY_MESSAGE= "sling.validator.invalid-resource-post-processor-exception";
    
    public InvalidResourcePostProcessorException(@Nonnull ValidationResult result, ResourceBundle resourceBundle) {
        super();
        this.result = result;
        this.resourceBundle = resourceBundle;
    }

    public String getMessage() {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (ValidationFailure failure : result.getFailures()) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            if (StringUtils.isNotEmpty(failure.getLocation())) {
                builder.append(failure.getLocation() + " : ");
            }
            builder.append(failure.getMessage(resourceBundle));
        }
        return MessageFormat.format(resourceBundle.getString(KEY_MESSAGE), builder.toString());
    }
}

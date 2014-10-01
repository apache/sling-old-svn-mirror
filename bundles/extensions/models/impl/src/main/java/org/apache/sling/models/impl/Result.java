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
package org.apache.sling.models.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.models.factory.MissingElementsException;
import org.slf4j.Logger;

public class Result<ModelType> {

    enum FailureType {
        ADAPTABLE_DOES_NOT_MATCH("Adaptable is not acceptable for the model class"), FAILED_CALLING_POST_CONSTRUCT(
                "Failure calling post-construct method"), NO_MODEL_ANNOTATION(
                "Provided Adapter class does not have a Model annotation"), NO_USABLE_CONSTRUCTOR(
                "Unable to find a useable constructor"), OTHER("Unclassified problem"), MISSING_METHODS(
                "Required methods %s on model interface %s were not able to be injected."), MISSING_FIELDS(
                "Required fields %s on model interface %s were not able to be injected."), MISSING_CONSTRUCTOR_PARAMS(
                "Required constructor parameteres %s on model interface %s were not able to be injected.");

        private String message;

        private FailureType(String msg) {
            this.message = msg;
        }
    }

    private Exception failureException;

    private String failureMessage;

    private FailureType failureType;

    private ModelType model;

    private Class<? extends ModelType> type;

    private Set<? extends AnnotatedElement> missingElements;

    public ModelType getModel() {
        return model;
    }

    public void logFailure(Logger log) {
        if (failureType != null) {
            switch (failureType) {
            case MISSING_CONSTRUCTOR_PARAMS:
            case MISSING_FIELDS:
            case MISSING_METHODS:
                log.error(String.format(failureType.message, missingElements, type));
                break;
            default:
                log.error(getMessage(), failureException);
                break;
            }
        }
    }

    public void setFailure(FailureType type) {
        this.failureType = type;
    }

    public void setFailure(FailureType type, Exception e) {
        this.failureType = type;
        this.failureException = e;
    }

    public void setFailure(FailureType type, Set<? extends AnnotatedElement> requiredElements) {
        this.failureType = type;
        this.missingElements = requiredElements;
    }

    public void setFailure(FailureType type, String msg) {
        this.failureType = type;
        this.failureMessage = msg;
    }

    public void setFailure(FailureType type, String msg, Exception e) {
        this.failureType = type;
        this.failureMessage = msg;
        this.failureException = e;
    }

    public void setModel(ModelType model) {
        this.model = model;
    }

    public void setType(Class<? extends ModelType> type) {
        this.type = type;
    }

    public void throwException() {
        if (failureType != null) {
            final String msg = getMessage();
            switch (failureType) {
            case ADAPTABLE_DOES_NOT_MATCH:
                throw new InvalidAdaptableException(msg);
            case FAILED_CALLING_POST_CONSTRUCT:
            case NO_MODEL_ANNOTATION:
            case NO_USABLE_CONSTRUCTOR:
                throw new InvalidModelException(msg);
            case MISSING_CONSTRUCTOR_PARAMS:
            case MISSING_FIELDS:
            case MISSING_METHODS:
                throw new MissingElementsException(failureType.message, missingElements, type);
            case OTHER:
                throw new RuntimeException(msg);
            }
        }
    }

    private String getMessage() {
        if (failureMessage != null) {
            return failureMessage;
        } else if (failureType != null) {
            return failureType.message;
        } else {
            return null;
        }
    }
}

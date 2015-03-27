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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.MissingElementsException;
import org.slf4j.Logger;

public class Result<ModelType> {

    public enum FailureType {
        ADAPTABLE_DOES_NOT_MATCH("Adaptable is not acceptable for the model class: %s"),
        FAILED_CALLING_POST_CONSTRUCT("Failure calling post-construct method"),
        NO_MODEL_ANNOTATION("Provided Adapter class does not have a Model annotation: %s"),
        NO_USABLE_CONSTRUCTOR("Unable to find a useable constructor: %s"),
        OTHER("Unclassified problem: %s"),
        MISSING_METHODS("Required methods %s on model %s were not able to be injected."),
        MISSING_FIELDS("Required fields %s on model %s were not able to be injected."),
        MISSING_CONSTRUCTOR_PARAMS("Required constructor parameters %s on model %s were not able to be injected."), 
        ADAPTABLE_NOT_USABLE_FOR_VALIDATION("Can not call Sling Validation in case the adaptable is neiter SlingHttpServletRequest or Resource (adaptable is'%s')"),
        VALIDATION_NOT_AVAILABLE("Sling Validation Bundle is not there, therefore no validation can be performed."),
        VALIDATION_MODEL_NOT_FOUND("Sling Validation model could not be found"),
        VALIDATION_MODEL_INVALID("Sling Validation model is invalid"),
        VALIDATION_RESULT_RESOURCE_INVALID("Model is invalid according to Sling Validation");

        private String message;

        private FailureType(String msg) {
            this.message = msg;
        }
    }
    
    private static class Failure {
        private FailureType failureType;

        private Throwable failureException;

        private String failureMessage;

        private Set<? extends AnnotatedElement> missingElements;

        private Class<?> clazz;

        private String getMessage() {
            if (failureMessage != null) {
                return failureMessage;
            } else if (failureType != null) {
                return failureType.message;
            } else {
                return null;
            }
        }

        public void log(Logger log) {

            if (failureType != null) {
                switch (failureType) {
                case MISSING_CONSTRUCTOR_PARAMS:
                case MISSING_FIELDS:
                case MISSING_METHODS:
                    log.error(String.format(failureType.message, missingElements, clazz));
                    break;
                case ADAPTABLE_DOES_NOT_MATCH:
                case NO_MODEL_ANNOTATION:
                case NO_USABLE_CONSTRUCTOR:
                    log.error(String.format(failureType.message, clazz));
                    break;
                case OTHER:
                default:
                    log.error(getMessage(), failureException);
                    break;
                }
            }
        }

        public void throwException() {
            RuntimeException e = null;
            if (failureType != null) {
                final String msg = getMessage();
                switch (failureType) {
                case ADAPTABLE_DOES_NOT_MATCH:
                    e = new InvalidAdaptableException(msg);
                    break;
                case FAILED_CALLING_POST_CONSTRUCT:
                case NO_MODEL_ANNOTATION:
                case NO_USABLE_CONSTRUCTOR:
                case VALIDATION_NOT_AVAILABLE:
                case ADAPTABLE_NOT_USABLE_FOR_VALIDATION:
                    e = new ModelClassException(msg);
                    break;
                case MISSING_CONSTRUCTOR_PARAMS:
                case MISSING_FIELDS:
                case MISSING_METHODS:
                    e = new MissingElementsException(failureType.message, missingElements, clazz);
                    break;
                case VALIDATION_MODEL_NOT_FOUND:
                case VALIDATION_RESULT_RESOURCE_INVALID:
                case VALIDATION_MODEL_INVALID:
                    throw (RuntimeException)failureException;
                default:
                    e = new RuntimeException(msg);
                    break;
                }
            }
            if (e != null) {
                if (failureException != null) {
                    e.initCause(failureException);
                }
                throw e;
            }
        }
        
    }

    private ModelType model;
    
    private List<Failure> failures = new ArrayList<Failure>();

    public ModelType getModel() {
        return model;
    }

    public void logFailures(Logger log) {
        for (Failure failure : failures) {
            failure.log(log);
        }
    }
    
    public void addFailure(FailureType type) {
        Failure f = new Failure();
        f.failureType = type;
        failures.add(f);
    }

    public void addFailure(FailureType type, Class<?> clazz) {
        Failure f = new Failure();
        f.failureType = type;
        f.clazz = clazz;
        failures.add(f);
    }

    public void addFailure(FailureType type, Throwable e) {
        Failure f = new Failure();
        f.failureType = type;
        f.failureException = e;
        failures.add(f);
    }

    public void addFailure(FailureType type, Set<? extends AnnotatedElement> requiredElements, Class<?> clazz) {
        Failure f = new Failure();
        f.failureType = type;
        f.missingElements = requiredElements;
        f.clazz = clazz;
        failures.add(f);
    }
    
    public void addFailureWithParameters(FailureType type, Object... parameters) {
        Failure f = new Failure();
        f.failureType = type;
        f.failureMessage = String.format(type.message, parameters);
        failures.add(f);
    }

    public void addFailure(FailureType type, String msg) {
        Failure f = new Failure();
        f.failureType = type;
        f.failureMessage = msg;
        failures.add(f);
    }

    public void addFailure(FailureType type, String msg, Throwable e) {
        Failure f = new Failure();
        f.failureType = type;
        f.failureMessage = msg;
        f.failureException = e;
        failures.add(f);
    }

    public void setModel(ModelType model) {
        this.model = model;
    }

    public void throwException(Logger log) {
        for (int i = 0; i < failures.size() - 1; i++) {
            failures.get(i).log(log);
        }
        if (failures.size() >= 1) {
            failures.get(failures.size() - 1).throwException();
        }
    }

    public void appendFailures(Result<?> result) {
        failures.addAll(result.failures);
    }
}

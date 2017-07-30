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
package org.apache.sling.validation.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The Validate annotation to be used on Sling Model class fields to provide extra validation arguments.
 */
@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Validate {

    /**
     * Validate the annotated element with a Validator having the given id.
     *
     * @return the string
     */
    String validatorId();

    /**
     * Parametrisation being passed to the validator given by the id. Each string entry must have the format <key>=<value>
     *
     * @return validator arguments
     */
    String[] arguments() default {};

    /**
     * Validation Error Severity.
     *
     * @return the Severity
     */
    int severity() default 0;
}

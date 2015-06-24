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
package org.apache.sling.models.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as adaptable via Sling Models.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Model {

    /**
     * @return List of classes from which can be adapted.
     */
    public Class<?>[] adaptables();

    /**
     * @return List of classes to which can be adapted. If missing, the class that is annotated is used.
     *   If classes are given, they have to be either the annotated class itself, or interfaces or super classes of the class.
     */
    public Class<?>[] adapters() default {};

    /**
     * @return Default injection strategy (optional or required)
     */
    public DefaultInjectionStrategy defaultInjectionStrategy() default DefaultInjectionStrategy.REQUIRED;

    /**
     * @return Condition that is displayed in the felix console adapter plugin
     */
    public String condition() default "";
    
    /**
     * 
     * @return {@link ValidationStrategy#DISABLED} in case the model should not be validated through Sling Validation (default),
     *  {@link ValidationStrategy#REQUIRED} in case the model should be validated and if no appropriate Sling Validation Model exists it is considered invalid or
     *  {@link ValidationStrategy#OPTIONAL} in case the model should be validated only in case an appropriate Sling Validation Model is found.
     * @see <a href="http://sling.apache.org/documentation/bundles/validation.html">Sling Validation</a>
     */
    public ValidationStrategy validation() default ValidationStrategy.DISABLED;

}

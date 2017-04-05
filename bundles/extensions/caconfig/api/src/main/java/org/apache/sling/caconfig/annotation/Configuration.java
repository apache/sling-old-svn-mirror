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
package org.apache.sling.caconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an annotation class to be useable with Sling context-aware configuration. 
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {

    /**
     * @return Allows to overwrite the configuration name. If not set the class name of the annotation is used.
     */
    String name() default "";
    
    /**
     * @return Label for the resource (e.g. for configuration editor GUIs).
     */
    String label() default "";
    
    /**
     * @return Description for the resource (e.g. for configuration editor GUIs).
     */
    String description() default "";
    
    /**
     * @return Further properties e.g. for configuration editor GUIs.
     */
    String[] property() default {};
    
    /**
     * @return Indicates that this definition should be used for configuration collections.
     */
    boolean collection() default false;

}

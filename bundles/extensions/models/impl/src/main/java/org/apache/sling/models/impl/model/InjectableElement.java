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
package org.apache.sling.models.impl.model;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;

@SuppressWarnings("deprecation")
public interface InjectableElement {
    
    /**
     * @return Underlying annotated element
     */
    AnnotatedElement getAnnotatedElement();

    /**
     * @return Type of injectable mapped to wrapper class
     */
    Type getType();
    
    /**
     * @return true if original type of injectable is a primitive type
     */
    boolean isPrimitive();
    
    /**
     * @return Name for injection
     */
    String getName();
    
    /**
     * @return Via annotation or null
     */
    String getSource();

    /**
     * @return Via annotation or null
     */
    String getVia();
    
    /**
     * @return true, if a default value is set
     */
    boolean hasDefaultValue();

    /**
     * @return Default value or null
     */
    Object getDefaultValue();
    
    /**
     * @return {@code true} if the element is optional otherwise {@code false}
     */
    boolean isOptional(InjectAnnotationProcessor annotationProcessor);

}

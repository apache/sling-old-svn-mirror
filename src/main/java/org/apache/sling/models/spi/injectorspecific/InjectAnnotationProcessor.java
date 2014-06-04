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
package org.apache.sling.models.spi.injectorspecific;

/**
 * Processor for injector-specific annotations.
 */
public interface InjectAnnotationProcessor {

    /**
     * Tries to get the name value from the annotation.
     * 
     * @return the value to be used for the name or null, in which case 
     *         the standard annotation or name derived from method/field
     *         should be used
     */
    String getName();

    /**
     * Tries to get the via value from the annotation.
     * 
     * @return the value to be used for the via or null, in
     *         which case the standard annotation should be used
     */
    String getVia();

    /**
     * 
     * @return true, if a default value is set
     */
    boolean hasDefault();

    /**
     * Tries to get the default value from the annotation. Only used if {@link hasDefaultValue()} is set to true.
     * 
     * @return the value to be used if nothing can be injected
     */
    Object getDefault();

    /**
     * Tries to get the information whether the injection is optional.
     * 
     * @return the value to be used for the default or null, in
     *         which case the standard annotation should be used.
     */
    Boolean isOptional();

}

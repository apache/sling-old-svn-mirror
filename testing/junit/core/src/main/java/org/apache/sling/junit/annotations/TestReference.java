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
package org.apache.sling.junit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Annotation used to inject services in test classes. Similar
 *  to the Felix @Reference annotation, but we need RetentionPolicy.RUNTIME
 *  so we cannot use that one.
 *   
 *  @deprecated - the {#link TeleporterRule} is a much simpler way of executing
 *      server-side tests, including OSGi service injection.
 */
@Target( { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface TestReference {
    /**
     * The local name of the reference.
     * Default value is the name of the field to
     * which the annotation applies.
     */
    String name() default "";

    /**
     * The name of the service interface. This name is used by the Service
     * Component Runtime to access the service on behalf of the component. 
     * The default value for is the type of the field to which
     * the annotation applies.
     */
    Class<?> referenceInterface() default AutoDetect.class;
}

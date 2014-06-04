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
package org.apache.sling.models.annotations.injectorspecific;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

/**
 * Annotation to be used on either methods or fields to let Sling Models inject a value from the ValueMap of the current resource.
 *
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@InjectAnnotation
@Source("valuemap")
public @interface ValueMapValue {
    /**
     * Specifies the name of the value from the value map to take.
     * If empty, then the name is derived from the method or field.
     */
    String name() default "";

    /**
     * If set to true, the model can be instantiated even if that value is missing.
     * Only considered if default is not set, because any default value implicitly
     * sets optional to true
     */
    boolean optional() default false;

    /**
     * If set, then the child resource can be obtained via a projection of the given
     * property of the adaptable.
     */
    String via() default "";
}

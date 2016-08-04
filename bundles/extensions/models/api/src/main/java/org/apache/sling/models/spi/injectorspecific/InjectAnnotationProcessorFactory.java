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

import java.lang.reflect.AnnotatedElement;

import aQute.bnd.annotation.ConsumerType;

/**
 * Factory for {@link InjectAnnotationProcessor} that is evaluated at runtime for each
 * sling model adaption and may depend on the adaptable. 
 * @deprecated Use {@link StaticInjectAnnotationProcessorFactory} preferably or in case of a dependency to the adaptable {@link InjectAnnotationProcessorFactory2}
 */
@Deprecated
@ConsumerType
public interface InjectAnnotationProcessorFactory {

    /**
     * 
     * @param adaptable the object from which this model is adapted
     * @param element the field or method which is annotated
     * @return a ModelAnnotationProcessor in case there is a known
     *         injector-specific annotation on the given element found otherwise
     *         null. This method should return a {@link InjectAnnotationProcessor2} preferably.
     */
    InjectAnnotationProcessor createAnnotationProcessor(Object adaptable, AnnotatedElement element);

}

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
package org.apache.sling.models.spi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;

/**
 * Service interface for pluggable value injectors.
 */
@ConsumerType
public interface Injector {

    /**
     * Return a logical name for the injector. Used in resolving <pre>@Source</pre> annotations.
     *
     * @return the injector's name
     */
    @Nonnull String getName();

    /**
     * Produce a value for an injection point.
     *
     * @param adaptable the object which should be used as the basis for value resolution.
     * @param name the injection point name (may be only {@code null} if injector implements the {@link AcceptsNullName} interface, otherwise never {@code null})
     * @param declaredType the declared type of the injection point
     * @param element the injection point itself
     * @param callbackRegistry a registry object to register a callback object which will be
     *                         invoked when the adapted object is disposed.
     *
     * @return the value to be injected or null if no value could be resolved
     */
    @CheckForNull Object getValue(@Nonnull Object adaptable, String name, @Nonnull Type declaredType, @Nonnull AnnotatedElement element, @Nonnull DisposalCallbackRegistry callbackRegistry);
}

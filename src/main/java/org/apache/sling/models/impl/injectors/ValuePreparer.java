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
package org.apache.sling.models.impl.injectors;

import javax.annotation.Nonnull;

/**
 * Optimization interface for Injectors which wish to avoid repeated accessing of some object
 * based on the adaptable. If an Injector implements this interface, it must also be prepared
 * to handle the case where ObjectUtils.NULL is passed as the adaptable.
 */
public interface ValuePreparer {

    /**
     * Prepare a value from the adaptable.
     *
     * @param adaptable the adaptable
     * @return a prepared value or ObjectUtils.NULL if a value is not preparable
     */
    @Nonnull Object prepareValue(@Nonnull Object adaptable);
}

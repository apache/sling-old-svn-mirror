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
package org.apache.sling.models.spi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;

/**
 * Defines a strategy to choose an implementation for a model if multiple are registered 
 * for the same interface or super class.
 * <p>
 * With using the @Model.adapters attribute it is possible to define interfaces or super 
 * classes to which the model implementation is an adaption target. It is possible that 
 * multiple models implement the same type.
 * </p>
 * <p>
 * In this case services implementing the {@link ImplementationPicker} interface are 
 * queried to decide which implementation should be chosen. If multiple implementations 
 * of this interface exists they are queried one after another by service ranking. 
 * The first that picks an implementation is the winner.
 * </p>
 */
@ConsumerType
public interface ImplementationPicker {
    
    /**
     * Select an implementation for the adapter class to which the adaptable should be adapted to.
     * @param adapterType Adapter type. Never null.
     * @param implementationsTypes Available implementations. It is guaranteed that they can be assigned to the adapter type.
     *     Never null and has always at least one entry.
     * @param adaptable For reference: the adaptable. May be enquired to detect the context of the adaption. Never null.
     * @return If an implementation is chosen the class is returned, otherwise null.
     */
    @CheckForNull Class<?> pick(@Nonnull Class<?> adapterType, @Nonnull Class<?>[] implementationsTypes, @Nonnull Object adaptable);

}

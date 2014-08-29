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
package org.apache.sling.models.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.models.spi.ImplementationPicker;
import org.osgi.framework.Constants;

/**
 * Picks first implementation.
 * This is the default implementation which is not very intelligent because it just picks
 * the first implementation from the list that is alphabetically ordered by class name.
 * But at least it gives a consistent behavior.
 * It's service ranking is set to the highest value to allow more intelligent implementations to step in.
 */
@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = Integer.MAX_VALUE)
public class FirstImplementationPicker implements ImplementationPicker {

    @Override
    public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
        // implementations is never null or empty
        return implementationsTypes[0];
    }

}
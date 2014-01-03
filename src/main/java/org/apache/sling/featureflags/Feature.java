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
package org.apache.sling.featureflags;

import org.apache.sling.api.adapter.Adaptable;

import aQute.bnd.annotation.ConsumerType;

/**
 * A feature is defined by its name.
 * Depending on the functionality the feature implements it can
 * be adapted to different services, like
 * <ul>
 *   <li>{@link ResourceHiding}</li>
 *   <li>{@link ResourceTypeMapping}</li>
 * </ul>
 */
@ConsumerType
public interface Feature extends Adaptable {

    /**
     * The name of the feature.
     */
    String getName();

    /**
     * The description of the feature.
     */
    String getDescription();
}

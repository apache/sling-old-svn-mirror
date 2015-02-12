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
package org.apache.sling.installer.factories.configuration;

public abstract class ConfigurationConstants {

    /**
     * This property defines if a configuration should be persisted by the
     * installer. This property is a boolean value defaulting to true.
     *
     * The property should be used, if a configuration should not be persisted
     * by clients creating the configuration.
     */
    public static final String PROPERTY_PERSISTENCE = "org.apache.sling.installer.configuration.persist";

    /**
     * This property defines the value to be used as a bundle location if a configuration
     * is created by the installer. This property is a string value defaulting either
     * to {@code null} or "?".
     * If this property contains the empty string, {@code null} is used as the value.
     *
     * The property should be used, if a configuration should be bound to a specific client.
     * @since 1.1
     */
    public static final String PROPERTY_BUNDLE_LOCATION = "org.apache.sling.installer.configuration.bundlelocation";
}

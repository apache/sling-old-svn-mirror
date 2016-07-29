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
package org.apache.sling.config.spi;

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Provides configuration persistence implementations.
 */
@ConsumerType
public interface ConfigurationPersistenceProvider {

    /**
     * Get all configuration values stored for a context path.
     * @param resolver Resource resolver
     * @param contextPath Context path
     * @param configNames If a set of config names is given only configuration for these names is returned.
     *      Otherwise all are returned.
     * @return Configuration names with value map containing the configuration set properties.
     *      Returns null if no parameters stored for this context paths, allowing other 
     *      persistence providers to step in.
     */
    Map<String, Map<String, Object>> get(@Nonnull ResourceResolver resolver, @Nonnull String contextPath, String... configNames);

    /**
     * Writes configuration values for a context paths. For each configuration name contained in the values map all existing 
     * parameter values are erased before writing the new ones.
     * @param resolver Resource resolver
     * @param contextPath Context path
     * @param values Configuration names with value map containing the configuration set properties.
     * @return true if configurations are accepted. false if this provider does not accept storing 
     *          the configurations and the next provider should be asked to store them.
     * @throws PersistenceException Persistence exception is thrown when storing configurations failed.
     */
    boolean store(@Nonnull ResourceResolver resolver, @Nonnull String contextPath, @Nonnull String configName,
            @Nonnull Map<String, Map<String, Object>> values) throws PersistenceException;

}

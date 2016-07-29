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
package org.apache.sling.contextaware.config.spi;

import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Provides configuration override values (default or forced).
 */
@ConsumerType
public interface ConfigurationOverrideProvider {

    /**
     * Returns a map with key value pairs for configuration parameter override.
     * <p>
     * Key:
     * </p>
     * <ul>
     * <li>Syntax: <code>[{scope}[:locked]]{configName}/{propertyName}</code></li>
     * <li><code>{scope}</code>: if "default", the system default parameter is overridden. Otherwise <code>{scope}</code>
     * may define a context path, in this case the configuration set's property value is overwritten by force for this
     * context path. If the [{scope}] part is missing or [locked], the parameter is overridden for all context paths.</li>
     * <li><code>locked</code>: If the scope value is suffixed with the string &quot;:locked&quot; this configuration
     * parameter cannot be overridden in nested configuration scopes.</li>
     * <li><code>{configName}</code>: Configuration name</li>
     * <li><code>{propertyName}</code>: Configuration property name</li>
     * </ul>
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li><code>[default]set1/param1</code> - Override default value for property "param1" from set "set1"</li>
     * <li><code>set1/param1</code> - Override value for property "param1" from set "set1" for all context paths</li>
     * <li><code>[/content/region1/site1]set1/param1</code> - Override value for property "param1" from set "set1" for the context path
     * <code>/content/region1/site1</code>. This has higher precedence than the other variants.</li>
     * </ul>
     * <p>
     * Value:
     * </p>
     * <ul>
     *  <li>Override value</li>
     * <li>Has to be convertible to the property's type</li>
     * </ul>
     * @return Map
     */
    @Nonnull Map<String, String> getOverrideMap();
    
}

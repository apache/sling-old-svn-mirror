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
package org.apache.sling.contextaware.config.spi.metadata;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Part of configuration set.
 */
@ProviderType
public interface ConfigurationPart {

    /**
     * Pattern for allowed configuration part names.
     */
    Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.]+(/[a-zA-Z0-9\\-\\_\\.]+)*$");
    
    /**
     * Configuration part name
     * @return
     */
    @Nonnull String getName();
    
}

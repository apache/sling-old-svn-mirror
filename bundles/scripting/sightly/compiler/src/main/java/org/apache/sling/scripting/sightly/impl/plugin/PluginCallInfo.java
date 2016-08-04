/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.plugin;

import java.util.Arrays;

/**
 * Data related to a plugin call
 */
public class PluginCallInfo {

    private final String name;
    private final String[] arguments;

    public PluginCallInfo(String name, String[] arguments) {
        this.name = name;
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }

    /**
     * Get the name of the called plugin
     * @return a string with the name of the called plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Get the plugin arguments
     * @return a possibly empty array of args
     */
    public String[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }
}

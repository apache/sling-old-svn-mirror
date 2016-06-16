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
package org.apache.sling.scripting.sightly.impl.compiler.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.impl.compiler.CompilerFrontend;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.html.dom.DocumentParser;
import org.apache.sling.scripting.sightly.impl.html.dom.MarkupHandler;
import org.apache.sling.scripting.sightly.impl.plugin.Plugin;

/**
 * DOM-based compiler implementation
 */
public class SimpleFrontend implements CompilerFrontend {

    private final Map<String, Plugin> plugins;
    private final List<Filter> filters;

    public SimpleFrontend(List<Plugin> plugins, List<Filter> filters) {
        this.plugins = new HashMap<>();
        this.filters = filters;
        for (Plugin plugin : plugins) {
            this.plugins.put(plugin.name(), plugin);
        }
    }

    @Override
    public void compile(PushStream stream, String source) {
        MarkupHandler markupHandler = new MarkupHandler(stream, plugins, filters);
        DocumentParser.parse(source, markupHandler);
    }
}

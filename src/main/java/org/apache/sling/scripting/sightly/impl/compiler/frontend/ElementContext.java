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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.html.dom.MarkupHandler;
import org.apache.sling.scripting.sightly.impl.plugin.PluginCallInfo;
import org.apache.sling.scripting.sightly.impl.plugin.PluginInvoke;

/**
 * Data structure used by {@link MarkupHandler}.
 */
public class ElementContext {
    private final String tagName;
    private final String openTagStartMarkup;

    private final List<PrioritizedInvoke> invokeList = new ArrayList<PrioritizedInvoke>();
    private final List<Map.Entry<String, Object>> attributes = new ArrayList<Map.Entry<String, Object>>();
    private PluginInvoke aggregateInvoke;

    public ElementContext(String tagName, String openTagStartMarkup) {
        this.tagName = tagName;
        this.openTagStartMarkup = openTagStartMarkup;
    }

    public String getTagName() {
        return tagName;
    }

    public String getOpenTagStartMarkup() {
        return openTagStartMarkup;
    }

    public void addPlugin(PluginInvoke invoke, int priority) {
        invokeList.add(new PrioritizedInvoke(invoke, priority));
    }

    public void addAttribute(String name, String value) {
        attributes.add(new AbstractMap.SimpleEntry<String, Object>(name, value));
    }

    public void addPluginCall(String name, PluginCallInfo info, Expression expression) {
        attributes.add(new AbstractMap.SimpleEntry<String, Object>(name,
                new AbstractMap.SimpleEntry<PluginCallInfo, Expression>(info, expression)));
    }

    public Iterable<Map.Entry<String, Object>> getAttributes() {
        return attributes;
    }

    public PluginInvoke pluginInvoke() {
        if (aggregateInvoke == null) {
            Collections.sort(invokeList);
            ArrayList<PluginInvoke> result = new ArrayList<PluginInvoke>();
            for (PrioritizedInvoke prioritizedInvoke : invokeList) {
                result.add(prioritizedInvoke.invoke);
            }
            aggregateInvoke = new AggregatePluginInvoke(result);
        }
        return aggregateInvoke;
    }


    private static final class PrioritizedInvoke implements Comparable<PrioritizedInvoke> {

        private final PluginInvoke invoke;
        private final int priority;

        private PrioritizedInvoke(PluginInvoke invoke, int priority) {
            this.invoke = invoke;
            this.priority = priority;
        }

        @Override
        public int compareTo(PrioritizedInvoke o) {
            if (this.priority < o.priority) {
                return -1;
            } else if (this.priority == o.priority) {
                return  0;
            }
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PrioritizedInvoke)) {
                return false;
            }
            PrioritizedInvoke that = (PrioritizedInvoke) obj;
            if (this.priority == that.priority) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            assert false : "hashCode not designed";
            return 42;
        }
    }
}

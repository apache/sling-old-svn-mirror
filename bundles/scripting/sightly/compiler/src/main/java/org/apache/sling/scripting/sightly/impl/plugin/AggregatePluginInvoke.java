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

import java.util.List;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;

/**
 * Plugin invoke which aggregates the behavior of several plugin invokes.
 */
public class AggregatePluginInvoke implements PluginInvoke {

    private final List<PluginInvoke> invokes;

    public AggregatePluginInvoke(List<PluginInvoke> invokes) {
        this.invokes = invokes;
    }

    @Override
    public void beforeElement(PushStream stream, String tagName) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeElement(stream, tagName);
        }
    }

    @Override
    public void beforeTagOpen(PushStream stream) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeTagOpen(stream);
        }
    }

    @Override
    public void beforeAttributes(PushStream stream) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeAttributes(stream);
        }
    }

    @Override
    public void beforeAttribute(PushStream stream, String attributeName) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeAttribute(stream, attributeName);
        }
    }

    @Override
    public void beforeAttributeValue(PushStream stream, String attributeName, ExpressionNode attributeValue) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeAttributeValue(stream, attributeName, attributeValue);
        }
    }

    @Override
    public void afterAttributeValue(PushStream stream, String attributeName) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterAttributeValue(stream, attributeName);
        }
    }

    @Override
    public void afterAttribute(PushStream stream, String attributeName) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterAttribute(stream, attributeName);
        }
    }

    @Override
    public void onPluginCall(PushStream stream, PluginCallInfo callInfo, Expression expression) {
        for (PluginInvoke invoke : invokes) {
            invoke.onPluginCall(stream, callInfo, expression);
        }
    }

    @Override
    public void afterAttributes(PushStream stream) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterAttributes(stream);
        }
    }

    @Override
    public void afterTagOpen(PushStream stream) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterTagOpen(stream);
        }
    }

    @Override
    public void beforeChildren(PushStream stream) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeChildren(stream);
        }
    }

    @Override
    public void afterChildren(PushStream stream) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterChildren(stream);
        }
    }

    @Override
    public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
        for (PluginInvoke invoke : invokes) {
            invoke.beforeTagClose(stream, isSelfClosing);
        }
    }

    @Override
    public void afterTagClose(PushStream stream, boolean isSelfClosing) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterTagClose(stream, isSelfClosing);
        }
    }

    @Override
    public void afterElement(PushStream stream) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            PluginInvoke invoke = invokes.get(i);
            invoke.afterElement(stream);
        }
    }
}

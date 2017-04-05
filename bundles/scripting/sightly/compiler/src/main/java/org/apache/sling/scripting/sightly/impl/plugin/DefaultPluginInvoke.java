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

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;

/**
 * Empty implementation for plugin invocation. Use this to implement methods selectively.
 *
 * @see org.apache.sling.scripting.sightly.impl.plugin.Plugin
 */
public class DefaultPluginInvoke implements PluginInvoke {

    @Override
    public void beforeElement(PushStream stream, String tagName) {
        
    }

    @Override
    public void beforeTagOpen(PushStream stream) {

    }

    @Override
    public void beforeAttributes(PushStream stream) {

    }

    @Override
    public void beforeAttribute(PushStream stream, String attributeName) {

    }

    @Override
    public void beforeAttributeValue(PushStream stream, String attributeName, ExpressionNode attributeValue) {

    }

    @Override
    public void afterAttributeValue(PushStream stream, String attributeName) {

    }

    @Override
    public void afterAttribute(PushStream stream, String attributeName) {

    }

    @Override
    public void onPluginCall(PushStream stream, PluginCallInfo callInfo, Expression expression) {

    }

    @Override
    public void afterAttributes(PushStream stream) {

    }

    @Override
    public void afterTagOpen(PushStream stream) {

    }

    @Override
    public void beforeChildren(PushStream stream) {

    }

    @Override
    public void afterChildren(PushStream stream) {

    }

    @Override
    public void beforeTagClose(PushStream stream, boolean isSelfClosing) {

    }

    @Override
    public void afterTagClose(PushStream stream, boolean isSelfClosing) {

    }

    @Override
    public void afterElement(PushStream stream) {

    }
}

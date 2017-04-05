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
package org.apache.sling.scripting.thymeleaf.it.app;

import java.util.Map;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.ILinkBuilder;

public class FooBarLinkBuilder implements ILinkBuilder {

    public FooBarLinkBuilder() {
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public String buildLink(final IExpressionContext expressionContext, final String base, final Map<String, Object> parameters) {
        if ("foo".equals(base)) {
            return "foobar";
        }
        return null;
    }

}

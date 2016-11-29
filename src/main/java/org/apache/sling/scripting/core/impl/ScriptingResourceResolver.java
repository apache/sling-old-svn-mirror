/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptingResourceResolver extends ResourceResolverWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingResourceResolver.class);

    private ResourceResolver delegate;
    private boolean shouldLogClosing = false;

    public ScriptingResourceResolver(boolean shouldLogClosing, ResourceResolver delegate) {
        super(delegate);
        this.shouldLogClosing = shouldLogClosing;
        this.delegate = delegate;
    }

    @Nonnull
    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        return new ScriptingResourceResolver(shouldLogClosing, delegate.clone(null));
    }

    @Override
    public void close() {
        LOGGER.warn("Attempted to call close on the scripting per-request resource resolver. This is handled automatically by the {}.",
                ScriptingResourceResolverProvider.class.getName());
        if (shouldLogClosing) {
            StringWriter writer = new StringWriter();
            Throwable t = new Throwable();
            t.printStackTrace(new PrintWriter(writer));
            LOGGER.warn("The following code attempted to close the per-request resource resolver: {}", writer.toString());
        }
    }

    void _close() {
        delegate.close();
    }

}

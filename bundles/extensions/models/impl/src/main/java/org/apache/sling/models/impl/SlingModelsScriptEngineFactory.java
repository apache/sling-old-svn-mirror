/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.core.impl.helper.ProtectedBindings;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.sling.api.scripting.SlingBindings.*;

/**
 * Essentially fake ScriptEngineFactory needed for accessing BindingsValuesProviders in the ExportServlet
 */
class SlingModelsScriptEngineFactory extends AbstractScriptEngineFactory implements ScriptEngineFactory {

    /** The context string to use to select BindingsValuesProviders */
    private static final String BINDINGS_CONTEXT = BindingsValuesProvider.DEFAULT_CONTEXT;

    /** embed this value so as to avoid a dependency on a newer Sling API than otherwise necessary. */
    static final String RESOLVER = "resolver";

    /** The set of protected keys. */
    private static final Set<String> PROTECTED_KEYS =
            new HashSet<String>(Arrays.asList(REQUEST, RESPONSE, READER, SLING, RESOURCE, RESOLVER, OUT, LOG));

    SlingModelsScriptEngineFactory(Bundle bundle) {
        super();
        setEngineName("Apache Sling Models");
        // really the only time this is null is during testing
        if (bundle != null && bundle.getHeaders() != null && bundle.getHeaders().get(Constants.BUNDLE_VERSION) != null) {
            setEngineVersion(bundle.getHeaders().get(Constants.BUNDLE_VERSION).toString());
        }
        setNames("sling-models-exporter", "sling-models");
    }

    @Override
    public String getLanguageName() {
        return null;
    }

    @Override
    public String getLanguageVersion() {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return null;
    }

    void invokeBindingsValuesProviders(BindingsValuesProvidersByContext bindingsValuesProvidersByContext, SimpleBindings bindings) {
        final Collection<BindingsValuesProvider> bindingsValuesProviders =
                bindingsValuesProvidersByContext.getBindingsValuesProviders(this, SlingModelsScriptEngineFactory.BINDINGS_CONTEXT);

        if (!bindingsValuesProviders.isEmpty()) {
            Set<String> protectedKeys = new HashSet<String>();
            protectedKeys.addAll(SlingModelsScriptEngineFactory.PROTECTED_KEYS);

            ProtectedBindings protectedBindings = new ProtectedBindings(bindings, protectedKeys);
            for (BindingsValuesProvider provider : bindingsValuesProviders) {
                provider.addBindings(protectedBindings);
            }

        }
    }
}

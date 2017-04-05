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
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Essentially fake ScriptEngineFactory needed for accessing BindingsValuesProviders in the ExportServlet
 */
class ExporterScriptEngineFactory extends AbstractScriptEngineFactory implements ScriptEngineFactory {

    ExporterScriptEngineFactory(Bundle bundle) {
        super();
        setEngineName("Apache Sling Models Exporter");
        // really the only time this is null is during testing
        if (bundle != null && bundle.getHeaders() != null && bundle.getHeaders().get(Constants.BUNDLE_VERSION) != null) {
            setEngineVersion(bundle.getHeaders().get(Constants.BUNDLE_VERSION).toString());
        }
        setNames("sling-models-exporter");
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
}

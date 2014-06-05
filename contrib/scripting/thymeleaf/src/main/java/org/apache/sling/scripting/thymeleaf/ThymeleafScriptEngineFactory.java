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
package org.apache.sling.scripting.thymeleaf;

import javax.script.ScriptEngine;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.Constants;
import org.thymeleaf.TemplateEngine;

@Component(
    name = "org.apache.sling.scripting.thymeleaf.ThymeleafScriptEngineFactory",
    label = "%org.apache.sling.scripting.thymeleaf.ThymeleafScriptEngineFactory.label",
    description = "%org.apache.sling.scripting.thymeleaf.ThymeleafScriptEngineFactory.description",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "scripting engine for Thymeleaf templates"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
public class ThymeleafScriptEngineFactory extends AbstractScriptEngineFactory {

    private SlingResourceResolver resourceResolver;

    private SlingTemplateResolver templateResolver;

    private SlingMessageResolver messageResolver;

    private TemplateEngine templateEngine;

    public ThymeleafScriptEngineFactory() {
        // TODO make configurable
        setExtensions("html");
        setMimeTypes("text/html");
        setNames("thymeleaf");
        setupThymeleaf();
    }

    // TODO make configurable
    protected void setupThymeleaf() {
        resourceResolver = new SlingResourceResolver();
        templateResolver = new SlingTemplateResolver(resourceResolver);
        messageResolver = new SlingMessageResolver();
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setMessageResolver(messageResolver);
    }

    @Override
    public String getLanguageName() {
        return "Thymeleaf";
    }

    @Override
    public String getLanguageVersion() {
        return "2.1"; // TODO get version from Thymeleaf
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new ThymeleafScriptEngine(this, templateEngine);
    }

}

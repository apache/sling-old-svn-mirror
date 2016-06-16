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
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.script.Bindings;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtension.NAME, value = RuntimeFunction.I18N)
})
public class I18nRuntimeExtension implements RuntimeExtension {

    private static final Logger LOG = LoggerFactory.getLogger(I18nRuntimeExtension.class);

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.I18N, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String text = runtimeObjectModel.toString(arguments[0]);
        Map<String, Object> options = (Map<String, Object>) arguments[1];
        String locale = runtimeObjectModel.toString(options.get("locale"));
        String hint = runtimeObjectModel.toString(options.get("hint"));
        final Bindings bindings = renderContext.getBindings();
        return get(bindings, text, locale, hint);
    }

    private String get(final Bindings bindings, String text, String locale, String hint) {

        final SlingScriptHelper slingScriptHelper = BindingsUtils.getHelper(bindings);
        final SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
        final ResourceBundleProvider resourceBundleProvider = slingScriptHelper.getService(ResourceBundleProvider.class);
        if (resourceBundleProvider != null) {
            String key = text;
            if (StringUtils.isNotEmpty(hint)) {
                key += " ((" + hint + "))";
            }
            if (StringUtils.isEmpty(locale)) {
                Enumeration<Locale> requestLocales = request.getLocales();
                while (requestLocales.hasMoreElements()) {
                    Locale l = requestLocales.nextElement();
                    ResourceBundle resourceBundle = resourceBundleProvider.getResourceBundle(l);
                    if (resourceBundle != null && resourceBundle.containsKey(key)) {
                        return resourceBundle.getString(key);
                    }
                }
            } else {
                try {
                    Locale l = LocaleUtils.toLocale(locale);
                    ResourceBundle resourceBundle = resourceBundleProvider.getResourceBundle(l);
                    if (resourceBundle != null && resourceBundle.containsKey(key)) {
                        return resourceBundle.getString(key);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("Invalid locale detected: {}.", locale);
                    return text;
                }

            }
        }
        LOG.warn("No translation found for string '{}' using expression provided locale '{}' or default locale '{}'",
                new String[] {text, locale, request.getLocale().getLanguage()});
        return text;
    }
}

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
import java.util.ResourceBundle;

import javax.script.Bindings;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.scripting.sightly.extension.ExtensionInstance;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.filter.I18nFilter;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtension.NAME, value = I18nFilter.FUNCTION)
})
public class I18nRuntimeExtension implements RuntimeExtension {

    private static final Logger LOG = LoggerFactory.getLogger(I18nRuntimeExtension.class);

    @Override
    public ExtensionInstance provide(final RenderContext renderContext) {

        return new ExtensionInstance() {
            @Override
            public Object call(Object... arguments) {
                ExtensionUtils.checkArgumentCount(I18nFilter.FUNCTION, arguments, 3);
                String text = renderContext.toString(arguments[0]);
                String locale = renderContext.toString(arguments[1]);
                String hint = renderContext.toString(arguments[2]);
                return get(text, locale, hint);
            }

            private String get(String text, String locale, String hint) {
                final Bindings bindings = renderContext.getBindings();
                final SlingScriptHelper slingScriptHelper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
                final SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
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
                        Locale l = new Locale(locale);
                        ResourceBundle resourceBundle = resourceBundleProvider.getResourceBundle(l);
                        if (resourceBundle != null && resourceBundle.containsKey(key)) {
                            return resourceBundle.getString(key);
                        }
                    }
                }
                LOG.warn("No translation found for string '{}' using expression provided locale '{}' or default locale '{}'",
                        new String[] {text, locale, resourceBundleProvider.getDefaultLocale().getLanguage()});
                return text;
            }

        };
    }
}

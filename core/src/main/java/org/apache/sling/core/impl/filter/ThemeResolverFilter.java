/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.filter;

import java.io.IOException;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.impl.RequestData;
import org.apache.sling.theme.Theme;
import org.apache.sling.theme.ThemeResolver;


/**
 * The <code>LocaleResolverFilter</code> is an internal global request filter
 * which is used by Sling to resolve the <code>Locale</code> to use during
 * request processing.
 * <p>
 * This filter is always installed by Sling and needs no configuration. It
 * receives the currently used {@link ThemeResolver} from Sling.
 * 
 * @scr.component immediate="true" label="%theme.name"
 *      description="%theme.description"
 * @scr.property name="service.description"
 *      value="Default AuthenticationService implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-600" type="Integer" private="true"
 * @scr.service
 */
public class ThemeResolverFilter implements ComponentFilter {

    /**
     * The current {@link ThemeResolver} to use or <code>null</code> if the
     * {@link Theme} is not currently resolved.
     * 
     * @scr.reference target="" cardinality="0..1" policy="dynamic"
     */
    private ThemeResolver themeResolver;

    /**
     * Calls the {@link ThemeResolver#resolveTheme(RenderRequest)} method if a
     * theme resolver is assigned to set the {@link Theme} for the current
     * request. If no theme resolver is assigned, this method just forwards the
     * request to the next filter.
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        if (themeResolver != null) {
            Theme theme = themeResolver.resolveTheme(request);
            RequestData.getRequestData(request).setTheme(theme);
        }

        // continue request processing without any more intervention
        filterChain.doFilter(request, response);
    }

    public void init(ComponentContext context) {
    }

    public void destroy() {
    }

    // ---------- Configuration

    /**
     * Sets the {@link ThemeResolver} to use. This may be <code>null</code> in
     * which case, themes will not be resolved any more.
     */
    protected void bindThemeResolver(ThemeResolver themeResolver) {
        this.themeResolver = themeResolver;
    }

    protected void unbindThemeResolver(ThemeResolver themeResolver) {
        this.themeResolver = null;
    }
}

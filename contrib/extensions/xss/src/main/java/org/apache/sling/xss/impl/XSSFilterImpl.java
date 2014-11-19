/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class implements the <code>XSSFilter</code> using the Antisamy
 * XSS protection library found at
 * <a href="http://code.google.com/p/owaspantisamy/">http://code.google.com/p/owaspantisamy/</a>.
 */
@Component(immediate = true)
@Service(value = {EventHandler.class, XSSFilter.class})
@Property(name = EventConstants.EVENT_TOPIC, value = {"org/apache/sling/api/resource/Resource/*",
        "org/apache/sling/api/resource/ResourceProvider/*"})
public class XSSFilterImpl implements XSSFilter, EventHandler {

    @Reference
    private ResourceResolverFactory resourceResolverFactory = null;

    /**
     * available contexts
     */
    private final XSSFilterRule htmlHtmlContext = new HtmlToHtmlContentContext();

    private final XSSFilterRule plainHtmlContext = new PlainTextToHtmlContentContext();

    /**
     * The paths to check for changes.
     */
    private Set<String> checkPaths = new HashSet<String>();

    /**
     * A cache for the policies.
     */
    private Map<String, PolicyHandler> policies = new ConcurrentHashMap<String, PolicyHandler>();

    /**
     * Maximum size for policy cache.
     */
    private static final int DEFAULT_POLICY_CACHE_SIZE = 128;

    private void updateCheckPaths(final String policyPath) {
        final Set<String> newCheckPaths = new HashSet<String>(checkPaths);
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            for (final String path : resolver.getSearchPath()) {
                newCheckPaths.add(path + policyPath);
                newCheckPaths.add(path + policyPath + "/jcr:content");
            }
            this.checkPaths = newCheckPaths;
        } catch (final LoginException le) {
            throw new RuntimeException("Unable to get administrative login.", le);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate() {
        // load default handler
        this.getPolicyHandler(null);
    }


    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        boolean reload = false;
        for (final String checkPath : this.checkPaths) {
            if (path.equals(checkPath)) {
                reload = true;
                break;
            }
        }
        if (reload) {
            this.policies.clear();
        }
    }

    /**
     * Get the filter rule context.
     */
    private XSSFilterRule getFilterRule(final ProtectionContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        if (context == ProtectionContext.HTML_HTML_CONTENT) {
            return this.htmlHtmlContext;
        }
        return this.plainHtmlContext;
    }

    /**
     * Get the policy handler.
     */
    private PolicyHandler getPolicyHandler(final String policyPath) {
        final String policyName = (policyPath == null ? XSSFilterRule.DEFAULT_POLICY_PATH : policyPath);
        PolicyHandler handler = this.policies.get(policyName);
        if (handler == null) {
            synchronized (this) {
                try {
                    handler = new PolicyHandler(this.resourceResolverFactory, policyName);
                    if (this.policies.size() < DEFAULT_POLICY_CACHE_SIZE) {
                        this.policies.put(policyName, handler);
                        this.updateCheckPaths(policyName);
                    }
                } catch (final Exception e) {
                    throw new RuntimeException("Unable to load policy " + policyName, e);
                }
            }
        }
        return handler;
    }

    /**
     * @see org.apache.sling.xss.XSSFilter#check(org.apache.sling.xss.ProtectionContext, String)
     */
    public boolean check(final ProtectionContext context, final String src) {
        return this.check(context, src, null);
    }

    /**
     * @see org.apache.sling.xss.XSSFilter#check(org.apache.sling.xss.ProtectionContext, String, String)
     */
    public boolean check(final ProtectionContext context, final String src, final String policy) {
        final XSSFilterRule ctx = this.getFilterRule(context);
        final PolicyHandler handler = ctx.supportsPolicy() ? this.getPolicyHandler(policy) : null;
        return ctx.check(handler, src);
    }

    /**
     * @see org.apache.sling.xss.XSSFilter#filter(java.lang.String)
     */
    public String filter(final String src) {
        return this.filter(XSSFilter.DEFAULT_CONTEXT, src);
    }

    /**
     * @see org.apache.sling.xss.XSSFilter#filter(ProtectionContext, java.lang.String)
     */
    public String filter(final ProtectionContext context, final String src) {
        return this.filter(context, src, null);
    }

    /**
     * @see org.apache.sling.xss.XSSFilter#filter(ProtectionContext, java.lang.String, java.lang.String)
     */
    public String filter(final ProtectionContext context, final String src, final String policy) {
        if (src == null) {
            return "";
        }
        final XSSFilterRule ctx = this.getFilterRule(context);
        final PolicyHandler handler = ctx.supportsPolicy() ? this.getPolicyHandler(policy) : null;
        return ctx.filter(handler, src);
    }
}

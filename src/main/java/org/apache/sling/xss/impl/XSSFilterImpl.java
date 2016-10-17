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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSFilter;
import org.owasp.validator.html.model.Attribute;
import org.owasp.validator.html.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the <code>XSSFilter</code> using the Antisamy XSS protection library found at
 * <a href="http://code.google.com/p/owaspantisamy/">http://code.google.com/p/owaspantisamy/</a>.
 */
@Component(immediate = true)
@Service(value = {ResourceChangeListener.class, XSSFilter.class})
@Properties({
    @Property(name = ResourceChangeListener.CHANGES, value = {"ADDED", "CHANGED", "REMOVED"}),
    @Property(name = ResourceChangeListener.PATHS, value = XSSFilterImpl.DEFAULT_POLICY_PATH)
})
public class XSSFilterImpl implements XSSFilter, ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(XSSFilterImpl.class);

    // Default href configuration copied from the config.xml supplied with AntiSamy
    static final Attribute DEFAULT_HREF_ATTRIBUTE = new Attribute(
            "href",
            Arrays.asList(
                    Pattern.compile("([\\p{L}\\p{M}*+\\p{N}\\\\\\.\\#@\\$%\\+&;\\-_~,\\?=/!\\*\\(\\)]*|\\#(\\w)+)"),
                    Pattern.compile("(\\s)*((ht|f)tp(s?)://|mailto:)[\\p{L}\\p{M}*+\\p{N}]+[\\p{L}\\p{M}*+\\p{N}\\p{Zs}\\.\\#@\\$%\\+&;:\\-_~,\\?=/!\\*\\(\\)]*(\\s)*")
            ),
            Collections.<String>emptyList(),
            "removeAttribute", ""
    );

    public static final String DEFAULT_POLICY_PATH = "sling/xss/config.xml";
    private static final String EMBEDDED_POLICY_PATH = "SLING-INF/content/config.xml";
    private static final String SLING_XSS_USER = "sling-xss";
    private static final int DEFAULT_POLICY_CACHE_SIZE = 128;
    private PolicyHandler defaultHandler;
    private Attribute hrefAttribute;

    // available contexts
    private final XSSFilterRule htmlHtmlContext = new HtmlToHtmlContentContext();
    private final XSSFilterRule plainHtmlContext = new PlainTextToHtmlContentContext();

    // policies cache
    private Map<String, PolicyHandler> policies = new ConcurrentHashMap<String, PolicyHandler>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory = null;

    @Override
    public void onChange(List<ResourceChange> resourceChanges) {
        for (ResourceChange change : resourceChanges) {
            if (change.getPath().endsWith(DEFAULT_POLICY_PATH)) {
                LOGGER.info("Detected policy file change ({}) at {}. Updating default handler.", change.getType().name(), change.getPath());
                updateDefaultHandler();
            }
        }
    }

    @Override
    public boolean check(final ProtectionContext context, final String src) {
        return this.check(context, src, null);
    }

    @Override
    public String filter(final String src) {
        return this.filter(XSSFilter.DEFAULT_CONTEXT, src);
    }

    @Override
    public String filter(final ProtectionContext context, final String src) {
        return this.filter(context, src, null);
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate() {
        // load default handler
        updateDefaultHandler();
    }

    private synchronized void updateDefaultHandler() {
        this.defaultHandler = null;
        ResourceResolver xssResourceResolver = null;
        try {
            Map<String, Object> authenticationInfo = new HashMap<String, Object>() {{
                put(ResourceResolverFactory.SUBSERVICE, SLING_XSS_USER);
            }};
            xssResourceResolver = resourceResolverFactory.getServiceResourceResolver(authenticationInfo);
            Resource policyResource = xssResourceResolver.getResource(DEFAULT_POLICY_PATH);
            if (policyResource != null) {
                try (InputStream policyStream = policyResource.adaptTo(InputStream.class)) {
                    setDefaultHandler(new PolicyHandler(policyStream));
                    LOGGER.info("Installed default policy from {}.", policyResource.getPath());
                } catch (Exception e) {
                    Throwable[] suppressed = e.getSuppressed();
                    if (suppressed.length > 0) {
                        for (Throwable t : suppressed) {
                            LOGGER.error("Unable to load policy from " + policyResource.getPath(), t);
                        }
                    }
                    LOGGER.error("Unable to load policy from " + policyResource.getPath(), e);
                }
            } else {
                // the content was not installed but the service is active; let's use the embedded file for the default handler
                LOGGER.warn("Could not find a policy file at the default location {}. Attempting to use the default resource embedded in" +
                        " the bundle.", DEFAULT_POLICY_PATH);
                try (InputStream policyStream = this.getClass().getClassLoader().getResourceAsStream(EMBEDDED_POLICY_PATH)) {
                    setDefaultHandler(new PolicyHandler(policyStream));
                    LOGGER.info("Installed default policy from the embedded {} file from the bundle.", EMBEDDED_POLICY_PATH);
                } catch (Exception e) {
                    Throwable[] suppressed = e.getSuppressed();
                    if (suppressed.length > 0) {
                        for (Throwable t : suppressed) {
                            LOGGER.error("Unable to load policy from embedded policy file.", t);
                        }
                    }
                    LOGGER.error("Unable to load policy from embedded policy file.", e);
                }
            }
            if (defaultHandler == null) {
                throw new IllegalStateException("Cannot load a default policy handler.");
            }
        } catch (LoginException e) {
            LOGGER.error("Unable to load the default policy file.", e);
        } finally {
            if (xssResourceResolver != null) {
                xssResourceResolver.close();
            }
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

    /*
        The following methods are not part of the API. Client-code dependency to these methods is risky as they can be removed at any
        point in time from the implementation.
     */

    public boolean check(final ProtectionContext context, final String src, final String policy) {
        final XSSFilterRule ctx = this.getFilterRule(context);
        PolicyHandler handler = null;
        if (ctx.supportsPolicy()) {
            if (policy == null || (handler = policies.get(policy)) == null) {
                handler = defaultHandler;
            }
        }
        return ctx.check(handler, src);
    }

    public String filter(final ProtectionContext context, final String src, final String policy) {
        if (src == null) {
            return "";
        }
        final XSSFilterRule ctx = this.getFilterRule(context);
        PolicyHandler handler = null;
        if (ctx.supportsPolicy()) {
            if (policy == null || (handler = policies.get(policy)) == null) {
                handler = defaultHandler;
            }
        }
        return ctx.filter(handler, src);
    }

    @SuppressWarnings("unused")
    public void setDefaultPolicy(InputStream policyStream) throws Exception {
        setDefaultHandler(new PolicyHandler(policyStream));
    }

    private void setDefaultHandler(PolicyHandler defaultHandler) {
        Tag linkTag = defaultHandler.getPolicy().getTagByLowercaseName("a");
        Attribute hrefAttribute = (linkTag != null) ? linkTag.getAttributeByName("href") : null;
        if (hrefAttribute == null) {
            // Fallback to default configuration
            hrefAttribute = DEFAULT_HREF_ATTRIBUTE;
        }

        this.defaultHandler = defaultHandler;
        this.hrefAttribute = hrefAttribute;
    }

    @SuppressWarnings("unused")
    public void resetDefaultPolicy() {
        updateDefaultHandler();
    }

    @SuppressWarnings("unused")
    public void loadPolicy(String policyName, InputStream policyStream) throws Exception {
        if (policies.size() < DEFAULT_POLICY_CACHE_SIZE) {
            PolicyHandler policyHandler = new PolicyHandler(policyStream);
            policies.put(policyName, policyHandler);
        }
    }

    @SuppressWarnings("unused")
    public void unloadPolicy(String policyName) {
        policies.remove(policyName);
    }

    @SuppressWarnings("unused")
    public boolean hasPolicy(String policyName) {
        return policies.containsKey(policyName);
    }

    @Override
    public boolean isValidHref(String url) {
        // Same logic as in org.owasp.validator.html.scan.MagicSAXFilter.startElement()
        boolean isValid = hrefAttribute.containsAllowedValue(url.toLowerCase());
        if (!isValid) {
            isValid = hrefAttribute.matchesAllowedExpression(url);
        }
        return isValid;
    }
}

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSFilter;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.owasp.validator.html.model.Attribute;
import org.owasp.validator.html.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the <code>XSSFilter</code> using the Antisamy XSS protection library found at
 * <a href="http://code.google.com/p/owaspantisamy/">http://code.google.com/p/owaspantisamy/</a>.
 */
@Component(
        service = {ResourceChangeListener.class, XSSFilter.class},
        property = {
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED",
                ResourceChangeListener.PATHS + "=" + XSSFilterImpl.DEFAULT_POLICY_PATH
        }
)
public class XSSFilterImpl implements XSSFilter, ResourceChangeListener, ExternalResourceChangeListener {

    private final Logger logger = LoggerFactory.getLogger(XSSFilterImpl.class);

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

    static final String DEFAULT_POLICY_PATH = "sling/xss/config.xml";
    private static final String EMBEDDED_POLICY_PATH = "SLING-INF/content/config.xml";
    private static final int DEFAULT_POLICY_CACHE_SIZE = 128;
    private PolicyHandler defaultHandler;
    private Attribute hrefAttribute;

    // available contexts
    private final XSSFilterRule htmlHtmlContext = new HtmlToHtmlContentContext();
    private final XSSFilterRule plainHtmlContext = new PlainTextToHtmlContentContext();

    // policies cache
    private final Map<String, PolicyHandler> policies = new ConcurrentHashMap<>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ServiceUserMapped serviceUserMapped;

    @Override
    public void onChange(@Nonnull List<ResourceChange> resourceChanges) {
        for (ResourceChange change : resourceChanges) {
            if (change.getPath().endsWith(DEFAULT_POLICY_PATH)) {
                logger.info("Detected policy file change ({}) at {}. Updating default handler.", change.getType().name(), change.getPath());
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

    @Override
    public boolean isValidHref(String url) {
        // Same logic as in org.owasp.validator.html.scan.MagicSAXFilter.startElement()
        boolean isValid = hrefAttribute.containsAllowedValue(url.toLowerCase());
        if (!isValid) {
            isValid = hrefAttribute.matchesAllowedExpression(url);
        }
        return isValid;
    }

    @Activate
    protected void activate() {
        // load default handler
        updateDefaultHandler();
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

    public void setDefaultPolicy(InputStream policyStream) throws Exception {
        setDefaultHandler(new PolicyHandler(policyStream));
    }

    public void resetDefaultPolicy() {
        updateDefaultHandler();
    }

    public void loadPolicy(String policyName, InputStream policyStream) throws Exception {
        if (policies.size() < DEFAULT_POLICY_CACHE_SIZE) {
            PolicyHandler policyHandler = new PolicyHandler(policyStream);
            policies.put(policyName, policyHandler);
        }
    }

    public void unloadPolicy(String policyName) {
        policies.remove(policyName);
    }

    public boolean hasPolicy(String policyName) {
        return policies.containsKey(policyName);
    }

    private synchronized void updateDefaultHandler() {
        this.defaultHandler = null;
        try (final ResourceResolver xssResourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            Resource policyResource = xssResourceResolver.getResource(DEFAULT_POLICY_PATH);
            if (policyResource != null) {
                try (InputStream policyStream = policyResource.adaptTo(InputStream.class)) {
                    setDefaultHandler(new PolicyHandler(policyStream));
                    logger.info("Installed default policy from {}.", policyResource.getPath());
                } catch (Exception e) {
                    Throwable[] suppressed = e.getSuppressed();
                    if (suppressed.length > 0) {
                        for (Throwable t : suppressed) {
                            logger.error("Unable to load policy from " + policyResource.getPath(), t);
                        }
                    }
                    logger.error("Unable to load policy from " + policyResource.getPath(), e);
                }
            }
        } catch (final LoginException e) {
            logger.error("Unable to load the default policy file.", e);
        }
        if (defaultHandler == null) {
            // the content was not installed but the service is active; let's use the embedded file for the default handler
            logger.info("Could not find a policy file at the default location {}. Attempting to use the default resource embedded in" +
                    " the bundle.", DEFAULT_POLICY_PATH);
            try (InputStream policyStream = this.getClass().getClassLoader().getResourceAsStream(EMBEDDED_POLICY_PATH)) {
                setDefaultHandler(new PolicyHandler(policyStream));
                logger.info("Installed default policy from the embedded {} file from the bundle.", EMBEDDED_POLICY_PATH);
            } catch (Exception e) {
                Throwable[] suppressed = e.getSuppressed();
                if (suppressed.length > 0) {
                    for (Throwable t : suppressed) {
                        logger.error("Unable to load policy from embedded policy file.", t);
                    }
                }
                logger.error("Unable to load policy from embedded policy file.", e);
            }
        }
        if (defaultHandler == null) {
            throw new IllegalStateException("Cannot load a default policy handler.");
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
}

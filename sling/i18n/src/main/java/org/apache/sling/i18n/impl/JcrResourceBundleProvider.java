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
package org.apache.sling.i18n.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>JcrResourceBundleProvider</code> TODO
 * 
 * @scr.component immediate="true"  label="%provider.name"
 *                description="%provider.description"
 * @scr.service interface="org.apache.sling.i18n.ResourceBundleProvider"
 */
public class JcrResourceBundleProvider implements ResourceBundleProvider,
        EventListener {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** @scr.property value="" */
    private static final String PROP_USER = "user";

    /** @scr.property value="" */
    private static final String PROP_PASS = "password";

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private SlingRepository repository;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private JcrResourceResolverFactory resourceResolverFactory;

    private Credentials repoCredentials;

    private ResourceResolver resourceResolver;

    private Map<Locale, ResourceBundle> resourceBundleCache = new HashMap<Locale, ResourceBundle>();

    public ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        ResourceResolver resolver = getResourceResolver();
        if (resolver == null) {
            log.info("createResourceBundle: Missing Resource Resolver, cannot create Resource Bundle");
            throw new MissingResourceException(
                "ResourceResolver not available", getClass().getName(), "");
        }
        
        return getResourceBundleInternal(resolver, locale);
    }
    
    private ResourceBundle getResourceBundleInternal(ResourceResolver resolver, Locale locale) {
        ResourceBundle resourceBundle = resourceBundleCache.get(locale);
        if (resourceBundle == null) {
            resourceBundle = createResourceBundle(resolver, locale);
            resourceBundleCache.put(locale, resourceBundle);
        }

        return resourceBundle;
    }

    private ResourceBundle createResourceBundle(ResourceResolver resolver, Locale locale) {

        JcrResourceBundle bundle = new JcrResourceBundle(locale, resolver);

        // set parent resource bundle
        Locale parent = getParentLocale(locale);
        if (parent != null) {
            bundle.setParent(getResourceBundleInternal(resolver, parent));
        }

        return bundle;
    }

    private Locale getParentLocale(Locale locale) {
        if (locale.getVariant().length() != 0) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        } else if (locale.getCountry().length() != 0) {
            return new Locale(locale.getLanguage());
        } else if (!locale.getLanguage().equals(
            Locale.getDefault().getLanguage())) {
            return Locale.getDefault();
        }

        // no more parents
        return null;
    }

    private ResourceResolver getResourceResolver() {
        if (resourceResolver == null) {
            SlingRepository repo = this.repository;
            JcrResourceResolverFactory fac = this.resourceResolverFactory;
            if (repo != null && fac != null) {
                Session s = null;
                try {
                    if (repoCredentials == null) {
                        s = repo.login();
                    } else {
                        s = repo.login(repoCredentials);
                    }

                    ObservationManager om = s.getWorkspace().getObservationManager();
                    om.addEventListener(this, 255, "/", true, null,
                        new String[] { "mix:language" }, true);
                    
                    resourceResolver = fac.getResourceResolver(s);
                    
                } catch (RepositoryException re) {
                    // TODO log
                } finally {
                    // drop session if we can login but not get the resource resolver
                    if (resourceResolver == null && s != null) {
                        s.logout();
                    }
                }

            }
        }

        return resourceResolver;
    }

    private void releaseRepository() {
        ResourceResolver resolver = this.resourceResolver;

        this.resourceResolver = null;
        this.resourceBundleCache.clear();

        if (resolver != null) {
            
            Session s = resolver.adaptTo(Session.class);
            if (s != null) {

                try {
                    ObservationManager om = s.getWorkspace().getObservationManager();
                    om.removeEventListener(this);
                } catch (Throwable t) {
                    // TODO log
                }

                try {
                    s.logout();
                } catch (Throwable t) {
                    // TODO log
                }
            }
        }
    }

    // ---------- EventListener ------------------------------------------------

    public void onEvent(EventIterator events) {
        // don't care for the concrete events, just drop the cache
        log.info("onEvent: Got " + events.getSize() + " events");
        resourceBundleCache.clear();
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        String user = (String) props.get(PROP_USER);
        if (user == null || user.length() == 0) {
            repoCredentials = null;
        } else {
            String pass = (String) props.get(PROP_PASS);
            char[] pwd = (pass == null) ? new char[0] : pass.toCharArray();
            repoCredentials = new SimpleCredentials(user, pwd);
        }
    }

    protected void bindRepository(SlingRepository repository) {
        if (this.repository != null) {
            releaseRepository();
        }
        this.repository = repository;
    }

    protected void unbindRepository(SlingRepository repository) {
        if (this.repository == repository) {
            releaseRepository();
            this.repository = null;
        }
    }

    protected void bindResourceResolverFactory(
            JcrResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory != null) {
            releaseRepository();
        }
        this.resourceResolverFactory = resourceResolverFactory;
    }

    protected void unbindResourceResolverFactory(
            JcrResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory == resourceResolverFactory) {
            releaseRepository();
            this.resourceResolverFactory = null;
        }
    }
}

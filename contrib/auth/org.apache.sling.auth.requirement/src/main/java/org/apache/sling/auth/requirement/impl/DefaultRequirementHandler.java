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
package org.apache.sling.auth.requirement.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the {@link RequirementHandler interface}.
 */
@Component(metatype = true,
        immediate = true,
        policy = ConfigurationPolicy.REQUIRE,
        name = " org.apache.sling.auth.requirement.impl.DefaultRequirementHandler",
        label = "Apache Sling Authentication Requirement and Login Path Handler",
        description = "Default RequirementHandler implementation that updates the Authentication " +
                "Requirements (and the corresponding exclusion for the associated login path).")
@Properties({
        @Property(name = DefaultRequirementHandler.PARAM_SUPPORTED_PATHS,
                label = "Supported Paths",
                description = "Paths under which authentication requirements can be created and will be respected.",
                cardinality = Integer.MAX_VALUE),
        @Property(name = org.osgi.framework.Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
})
public class DefaultRequirementHandler implements RequirementHandler, Constants {

    private static final Logger log = LoggerFactory.getLogger(DefaultRequirementHandler.class);

    public static final String PARAM_SUPPORTED_PATHS = "supportedPaths";

    @SuppressWarnings("UnusedDeclaration")
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @SuppressWarnings("UnusedDeclaration")
    @Reference
    private AuthenticationRequirement authenticationRequirement;

    private ServiceRegistration registration;

    private String id;

    private String[] supportedPaths;

    private Map<String, String> loginPathMapping = new TreeMap<String, String>(Collections.reverseOrder());

    //----------------------------------------------------------------< SCR >---
    @SuppressWarnings("UnusedDeclaration")
    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) throws Exception {
        supportedPaths = Utils.getValidPaths(PropertiesUtil.toStringArray(properties.get(PARAM_SUPPORTED_PATHS), new String[0]));

        registration = bundleContext.registerService(RequirementHandler.class.getName(), this, new Hashtable(properties));
        id = getID(registration);
        updateRequirements();
    }

    @SuppressWarnings("UnusedDeclaration")
    @Modified
    protected void modified(BundleContext bundleContext, Map<String, Object> properties) throws Exception {
        String[] newSupportedPaths = Utils.getValidPaths(PropertiesUtil.toStringArray(properties.get(PARAM_SUPPORTED_PATHS), new String[0]));

        if (!Arrays.equals(supportedPaths, newSupportedPaths)) {
            registration.setProperties(new Hashtable(properties));

            supportedPaths = newSupportedPaths;
            clearRequirements();
            updateRequirements();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deactivate
    protected void deactivate(ComponentContext componentContext) throws Exception {
        clearRequirements();
        registration.unregister();
    }

    @SuppressWarnings("UnusedDeclaration")
    private void bindResourceResolverFactory(@Nonnull ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void unbindResourceResolverFactory(@Nonnull ResourceResolverFactory resourceResolverFactory) {
        if (resourceResolverFactory == this.resourceResolverFactory) {
            this.resourceResolverFactory = null;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void bindAuthenticationRequirement(@Nonnull AuthenticationRequirement authenticationRequirement) {
        this.authenticationRequirement = authenticationRequirement;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void unbindAuthenticationRequirement(@Nonnull AuthenticationRequirement authenticationRequirement) {
        if (authenticationRequirement == this.authenticationRequirement) {
            this.authenticationRequirement = null;
        }
    }

    private static String getID(@Nonnull ServiceRegistration registration) {
        return OsgiUtil.toString(registration.getReference().getProperty(org.osgi.framework.Constants.SERVICE_ID), UUID.randomUUID().toString());
    }

    //-------------------------------------------------< RequirementHandler >---

    public void requirementAdded(@Nonnull String path, @Nullable String loginPath) {
        if (isSupportedPath(path, supportedPaths)) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = getServiceResolver();
                if (loginPath != null) {
                    loginPathMapping.put(path, loginPath);
                }
                authenticationRequirement.appendRequirements(id, buildRequirement(path, loginPath, resourceResolver));
            } catch (LoginException e) {
                log.error("Unable to add authentication requirements: failed to get service resolver.", e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
    }

    @Override
    public void requirementRemoved(@Nonnull String path, @Nullable String loginPath) {
        if (isSupportedPath(path, supportedPaths)) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = getServiceResolver();
                loginPathMapping.remove(path);
                authenticationRequirement.removeRequirements(id, buildRequirement(path, loginPath, resourceResolver));
            } catch (LoginException e) {
                log.error("Unable to remove authentication requirements: failed to get service resolver.", e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
    }

    @Override
    public void loginPathAdded(@Nonnull String path, @Nonnull String loginPath) {
        if (isSupportedPath(path, supportedPaths)) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = getServiceResolver();
                loginPathMapping.put(path, loginPath);
                authenticationRequirement.appendRequirements(id, buildLoginPathRequirement(loginPath, resourceResolver));
            } catch (LoginException e) {
                log.error("Unable to add login path to requirements: failed to get service resolver.", e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }

    }

    public void loginPathChanged(@Nonnull String path, @Nonnull String loginPathBefore, @Nonnull String loginPathAfter) {
        if (isSupportedPath(path, supportedPaths)) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = getServiceResolver();
                loginPathMapping.put(path, loginPathAfter);

                ServiceReference reference = registration.getReference();
                authenticationRequirement.removeRequirements(id, buildLoginPathRequirement(loginPathBefore, resourceResolver));
                authenticationRequirement.appendRequirements(id, buildLoginPathRequirement(loginPathAfter, resourceResolver));
            } catch (LoginException e) {
                log.error("Unable to update login path in authentication requirements: failed to get service resolver.", e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
    }

    public void loginPathRemoved(@Nonnull String path, @Nonnull String loginPath) {
        if (isSupportedPath(path, supportedPaths)) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = getServiceResolver();
                loginPathMapping.remove(path);
                authenticationRequirement.removeRequirements(id, buildLoginPathRequirement(loginPath, resourceResolver));
            } catch (LoginException e) {
                log.error("Unable to remove login path from authentication requirements: failed to get service resolver.", e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
    }

    @CheckForNull
    @Override
    public String getLoginPath(@Nonnull String path) {
        String loginPath = null;
        if (isSupportedPath(path, supportedPaths)) {
            loginPath = loginPathMapping.get(path);
            if (loginPath == null) {
                for (Map.Entry<String, String> mapping : loginPathMapping.entrySet()) {
                    if (Text.isDescendant(mapping.getKey(), path)) {
                        loginPath = mapping.getValue();
                        break;
                    }
                }
            }
        }
        return loginPath;
    }

    //------------------------------------------------------------< private >---

    @CheckForNull
    private ResourceResolver getServiceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(null);
    }

    private void updateRequirements() throws LoginException, RepositoryException {
        if (supportedPaths.length > 0) {
            Map<String, Boolean> requirements = new HashMap<String, Boolean>();
            loadAll(loginPathMapping, requirements, Utils.getCommonAncestor(supportedPaths));
            authenticationRequirement.setRequirements(id, requirements);
        }
    }

    private void loadAll(@Nonnull Map<String, String> loginPathMapping, @Nonnull Map<String, Boolean> requirements, @Nonnull String queryRoot) throws LoginException, RepositoryException {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getServiceResolver();
            Session serviceSession = checkNotNull(resourceResolver.adaptTo(Session.class));
            QueryManager qm = serviceSession.getWorkspace().getQueryManager();
            String queryString = "SELECT * FROM [" + MIX_SLING_AUTHENTICATION_REQUIRED + "] WHERE ISDESCENDANTNODE(["+ queryRoot +"])";
            Query q = qm.createQuery(queryString, Query.JCR_SQL2);
            RowIterator rows = q.execute().getRows();
            while (rows.hasNext()) {
                Row row = rows.nextRow();
                String path = row.getPath();
                if (isSupportedPath(path, supportedPaths)) {
                    Node n = row.getNode();
                    String loginPath = null;
                    if (n.hasProperty(NAME_SLING_LOGIN_PATH)) {
                        loginPath = n.getProperty(NAME_SLING_LOGIN_PATH).getString();
                        loginPathMapping.put(path, loginPath);
                    }
                    requirements.putAll(buildRequirement(path, loginPath, resourceResolver));
                }
            }
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private void clearRequirements() {
        loginPathMapping.clear();
        authenticationRequirement.clearRequirements(id);
    }

    private static Map<String, Boolean> buildRequirement(@Nonnull String path, @Nullable String loginPath,
                                                         @Nonnull ResourceResolver resourceResolver) {
        Map<String, Boolean> requirementMap = new HashMap<String, Boolean>(3);
        // the path that needs authentication requirement
        requirementMap.put(path, true);
        // the mapping of the path as root for the authentication requirement
        requirementMap.put(map(path, resourceResolver), true);
        // excluded login page (if existing), which must not be authenticated
        if (loginPath != null) {
            requirementMap.putAll(buildLoginPathRequirement(loginPath, resourceResolver));
        }
        return requirementMap;
    }

    private static Map<String, Boolean> buildLoginPathRequirement(@Nonnull String loginPath, @Nonnull ResourceResolver resourceResolver) {
        return Collections.singletonMap(toRawPath(loginPath, resourceResolver), false);
    }

    @Nonnull
    private static String map(@Nonnull String path, @CheckForNull ResourceResolver resolver) {
        return (resolver == null) ? path : resolver.map(path);
    }

    @Nonnull
    private static String toRawPath(@Nonnull String loginPath, @Nonnull ResourceResolver resolver) {
        Resource loginResource = resolver.resolve(loginPath);
        if (!ResourceUtil.isNonExistingResource(loginResource)) {
            return resolver.map(loginResource.getPath());
        }
        return loginPath;
    }

    private static boolean isSupportedPath(@Nonnull String pathToTest, @Nonnull String[] supportedPaths) {
        for (String sp : supportedPaths) {
            if (Text.isDescendantOrEqual(sp, pathToTest)) {
                return true;
            }
        }
        return false;
    }
}

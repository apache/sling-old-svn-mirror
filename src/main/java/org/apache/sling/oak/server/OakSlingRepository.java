/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.oak.server;

import static java.util.Collections.singleton;

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.AuthInfo;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthInfoImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.ConfigurationUtil;
import org.apache.jackrabbit.oak.spi.security.principal.AdminPrincipal;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.whiteboard.OsgiWhiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractNamespaceMappingRepository;
import org.osgi.service.component.ComponentContext;

/**
 * A Sling repository implementation that wraps the Oak OSGi repository
 * implementation from the Oak project.
 */
@Component(immediate = true, metatype = true)
@Service(value = { SlingRepository.class, Repository.class })
public class OakSlingRepository extends AbstractNamespaceMappingRepository
        implements SlingRepository {

    private RepositoryImpl jcrRepository;
    private SecurityProvider securityProvider;
    
    @Reference
    private NodeStore nodeStore;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        // FIXME GRANITE-2315
        Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));

        final Whiteboard whiteboard = new OsgiWhiteboard(ctx.getBundleContext());
        securityProvider = new SecurityProviderImpl(buildSecurityConfig());
        final Oak oak = new Oak(nodeStore)
        .with(securityProvider)
        .with(new LuceneIndexEditorProvider())
        .with(new LuceneIndexProvider())
        .withAsyncIndexing()
        .with(whiteboard);
        
        final ContentRepository contentRepository = oak.createContentRepository();  
        jcrRepository = new JcrRepositoryHacks(contentRepository, whiteboard, securityProvider);
        
        setup(ctx.getBundleContext());
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        tearDown();
    }
    
    @Override
    public String getDescriptor(String key) {
        return jcrRepository.getDescriptor(key);
    }

    @Override
    public String[] getDescriptorKeys() {
        return jcrRepository.getDescriptorKeys();
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return getNamespaceAwareSession(jcrRepository.login());
    }

    @Override
    public Session login(Credentials creds, String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return getNamespaceAwareSession(jcrRepository.login(creds, workspace));
    }

    @Override
    public Session login(Credentials creds) throws LoginException,
            RepositoryException {
        return getNamespaceAwareSession(jcrRepository.login(creds));
    }

    @Override
    public Session login(String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return getNamespaceAwareSession(jcrRepository.login(workspace));
    }

    @Override
    public String getDefaultWorkspace() {
        return "oak.sling";
    }

    @Override
    public Session loginAdministrative(String workspace) throws RepositoryException {
        final String adminId = securityProvider.getConfiguration(UserConfiguration.class)
        		.getParameters().getConfigValue(UserConstants.PARAM_ADMIN_ID, UserConstants.DEFAULT_ADMIN_ID);
        
        // TODO: use principal provider to retrieve admin principal
        Set<? extends Principal> principals = singleton(new AdminPrincipal() {
            @Override
            public String getName() {
                return adminId;
            }
        });
        AuthInfo authInfo = new AuthInfoImpl(adminId, Collections.<String, Object>emptyMap(), principals);
        Subject subject = new Subject(true, principals, singleton(authInfo), Collections.<Object>emptySet());
        Session adminSession;
        try {
            adminSession = Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {
                @Override
                public Session run() throws Exception {
                    Map<String,Object> attrs = new HashMap<String, Object>();
                    attrs.put(RepositoryImpl.REFRESH_INTERVAL,0);
                    //TODO OAK-803: Backwards compatibility of long-lived sessions
                    //Remove dependency on implementation specific API
                    return getNamespaceAwareSession((jcrRepository).login(null, null,attrs));
                }
            }, null);
        } catch (PrivilegedActionException e) {
            throw new RepositoryException("failed to retrieve admin session.", e);
        }

        return adminSession;
    }
    
    // TODO: use proper osgi configuration (once that works in oak)
    private static ConfigurationParameters buildSecurityConfig() {
        Map<String, Object> userConfig = new HashMap<String, Object>();
        userConfig.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        userConfig.put(UserConstants.PARAM_USER_PATH, "/home/users");
        userConfig.put(UserConstants.PARAM_DEFAULT_DEPTH, 1);
        userConfig.put(AccessControlAction.USER_PRIVILEGE_NAMES, PrivilegeConstants.JCR_ALL);
        userConfig.put(AccessControlAction.GROUP_PRIVILEGE_NAMES, PrivilegeConstants.JCR_READ);
        userConfig.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(
                UserConfiguration.NAME,
                new ConfigurationParameters(userConfig));
        return new ConfigurationParameters(config);
    }
}
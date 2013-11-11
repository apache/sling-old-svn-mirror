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

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singleton;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
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
import javax.jcr.Value;
import javax.jcr.security.Privilege;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.AuthInfo;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.index.aggregate.AggregateIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.aggregate.NodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.aggregate.SimpleNodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneIndexHelper;
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider;
import org.apache.jackrabbit.oak.plugins.name.NamespaceValidatorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.RegistrationEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthInfoImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.ConfigurationUtil;
import org.apache.jackrabbit.oak.spi.security.principal.AdminPrincipal;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.whiteboard.OsgiWhiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractNamespaceMappingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Sling repository implementation that wraps the Oak OSGi repository
 * implementation from the Oak project.
 */
@Component(immediate = true, metatype = true)
@Service(value = { SlingRepository.class, Repository.class })
public class OakSlingRepository extends AbstractNamespaceMappingRepository
        implements SlingRepository {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private RepositoryImpl jcrRepository;
    private SecurityProvider securityProvider;
    
    @Reference
    private NodeStore nodeStore;
    
    @Property(
            boolValue=true,
            label="Allow anonymous reads",
            description="If true, the anonymous user has read access to the whole repository (for backwards compatibility)")
    public static final String ANONYMOUS_READ_PROP = "anonymous.read.all";
    
    @Activate
    protected void activate(ComponentContext ctx) throws RepositoryException {
        // FIXME GRANITE-2315
        Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));

        final Whiteboard whiteboard = new OsgiWhiteboard(ctx.getBundleContext());
        securityProvider = new SecurityProviderImpl(buildSecurityConfig());
        final Oak oak = new Oak(nodeStore)
        .with(new InitialContent())
        .with(new ExtraSlingContent())

        .with(JcrConflictHandler.JCR_CONFLICT_HANDLER)
        .with(new EditorHook(new VersionEditorProvider()))

        .with(securityProvider)

        .with(new NameValidatorProvider())
        .with(new NamespaceValidatorProvider())
        .with(new TypeEditorProvider())
        .with(new RegistrationEditorProvider())
        .with(new ConflictValidatorProvider())

        // index stuff
        .with(new PropertyIndexEditorProvider())

        .with(new PropertyIndexProvider())
        .with(new NodeTypeIndexProvider())

        .with(new LuceneIndexEditorProvider())
        .with(AggregateIndexProvider.wrap(new LuceneIndexProvider()
                .with(getNodeAggregator())))

        .with(getDefaultWorkspace())
        .withAsyncIndexing()
        .with(whiteboard)
        ;
        
        final ContentRepository contentRepository = oak.createContentRepository();  
        jcrRepository = new JcrRepositoryHacks(contentRepository, whiteboard, securityProvider);
        
        setup(ctx.getBundleContext());
        
        final Object o = ctx.getProperties().get(ANONYMOUS_READ_PROP);
        if(o != null) {
            if(Boolean.valueOf(o.toString())) {
                log.warn("{} is true, granting anonymous user read access on /", ANONYMOUS_READ_PROP);
                final Session s = loginAdministrative(getDefaultWorkspace());
                try {
                    // TODO do we need to go via PrivilegeManager for the names? See OAK-1016 example.
                    final String [] privileges = new String[] { Privilege.JCR_READ };
                    AccessControlUtils.addAccessControlEntry(
                            s, 
                            "/", 
                            EveryonePrincipal.getInstance(), 
                            privileges, 
                            true);
                    s.save();
                } finally {
                    s.logout();
                }
            } else {
                log.warn("TODO: should disable anonymous access when {} becomes false", ANONYMOUS_READ_PROP);
            }
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        tearDown();
    }
    
    private static NodeAggregator getNodeAggregator() {
        return new SimpleNodeAggregator()
            .newRuleWithName("nt:file", Arrays.asList(new String [] {"jcr:content"}))
            ;
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
    
    private static final class ExtraSlingContent implements RepositoryInitializer {

        
        @Override
        public void initialize(NodeBuilder root) {
            if (root.hasChildNode(INDEX_DEFINITIONS_NAME)) {
                NodeBuilder index = root.child(INDEX_DEFINITIONS_NAME);
                
                // jcr: 
                property(index, "jcrLanguage", "jcr:language");
                property(index, "jcrLockOwner", "jcr:lockOwner");
                
                // sling:
                property(index, "slingAlias", "sling:alias");
                property(index, "slingResource", "sling:resource");
                property(index, "slingResourceType", "sling:resourceType");
                property(index, "slingVanityPath", "sling:vanityPath");
                
                // various
                property(index, "event.job.topic", "event.job.topic");
                property(index, "extensionType", "extensionType");
                property(index, "lockCreated", "lock.created");
                property(index, "status", "status");
                property(index, "type", "type");

                // lucene full-text index
                if (!index.hasChildNode("lucene")) {
                    LuceneIndexHelper.newLuceneIndexDefinition(
                            index, "lucene", LuceneIndexHelper.JR_PROPERTY_INCLUDES,
                            of(
                               "jcr:createdBy",
                               "jcr:lastModifiedBy", 
                               "sling:alias", 
                               "sling:resourceType",
                               "sling:vanityPath"),
                            "async");
                }
                
            }
        }
        
        /**
         * A convenience method to create a non-unique property index.
         */
        private static void property(NodeBuilder index, String indexName, String propertyName) {
            if (!index.hasChildNode(indexName)) {
                createIndexDefinition(index, indexName, true, false, singleton(propertyName), null);
            }
        }
        
    }
    
    // TODO: use proper osgi configuration (once that works in oak)
    private static ConfigurationParameters buildSecurityConfig() {
        Map<String, Object> userConfig = new HashMap<String, Object>();
        userConfig.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        userConfig.put(UserConstants.PARAM_USER_PATH, "/home/users");
        userConfig.put(UserConstants.PARAM_DEFAULT_DEPTH, 1);
        userConfig.put(AccessControlAction.USER_PRIVILEGE_NAMES, new String[] { PrivilegeConstants.JCR_ALL });
        userConfig.put(AccessControlAction.GROUP_PRIVILEGE_NAMES, new String[] { PrivilegeConstants.JCR_READ });
        userConfig.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(
                UserConfiguration.NAME,
                new ConfigurationParameters(userConfig));
        return new ConfigurationParameters(config);
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Value getDescriptorValue(String key) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        throw new UnsupportedOperationException("Not implemented yet");
    }    
}
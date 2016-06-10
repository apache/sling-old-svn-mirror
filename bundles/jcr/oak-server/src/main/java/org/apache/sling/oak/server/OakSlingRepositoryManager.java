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
import static org.apache.felix.scr.annotations.ReferencePolicy.STATIC;
import static org.apache.felix.scr.annotations.ReferencePolicyOption.GREEDY;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executor;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.osgi.OsgiRepository;
import org.apache.jackrabbit.oak.osgi.OsgiWhiteboard;
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.index.aggregate.NodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.aggregate.SimpleNodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneIndexHelper;
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider;
import org.apache.jackrabbit.oak.plugins.name.NamespaceEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.observation.CommitRateLimiter;
import org.apache.jackrabbit.oak.plugins.version.VersionHook;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardIndexEditorProvider;
import org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardIndexProvider;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository2;
import org.apache.sling.jcr.base.AbstractSlingRepositoryManager;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Sling repository implementation that wraps the Oak OSGi repository
 * implementation from the Oak project.
 */
@Component(
        immediate = true,
        metatype = true,
        name = "org.apache.sling.oak.server.OakSlingRepository",
        label = "Apache Sling Embedded JCR Repository (Oak)",
        description = "Configuration to launch an embedded JCR Repository "
            + "and provide it as a SlingRepository and a standard JCR "
            + "Repository. In addition, if the registration URL is not "
            + "empty, the repository is registered as defined.")
public class OakSlingRepositoryManager extends AbstractSlingRepositoryManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_OBSERVATION_QUEUE_LENGTH = 1000;
    private static final boolean DEFAULT_COMMIT_RATE_LIMIT = false;

    // For backwards compatibility loginAdministrative is still enabled
    // In future releases, this default may change to false.
    public static final boolean DEFAULT_LOGIN_ADMIN_ENABLED = true;

    @Property(
            label = "Default Workspace",
            description = "Name of the workspace to use by default if not is given in any of the login methods. This name is used "
                + "to implement the SlingRepository.getDefaultWorkspace() "
                + "method. If this name is empty, a null value is used in "
                + "JCR calls so that the default workspace provided by the JCR repository is used.")
    public static final String PROPERTY_DEFAULT_WORKSPACE = "defaultWorkspace";

    @Property(
            boolValue = DEFAULT_LOGIN_ADMIN_ENABLED,
            label = "Enable Administrator Login",
            description = "Whether to enable or disable the SlingRepository.loginAdministrative "
                + "method. The default is 'true'. See "
                + "http://sling.apache.org/documentation/the-sling-engine/service-authentication.html "
                + "for information on deprecating and disabling the loginAdministrative method.")
    public static final String PROPERTY_LOGIN_ADMIN_ENABLED = "admin.login.enabled";

    @Property(
            intValue = DEFAULT_OBSERVATION_QUEUE_LENGTH,
            label = "Observation queue length",
            description = "Maximum number of pending revisions in a observation listener queue")
    private static final String OBSERVATION_QUEUE_LENGTH = "oak.observation.queue-length";

    @Property(
            boolValue = DEFAULT_COMMIT_RATE_LIMIT,
            label = "Commit rate limiter",
            description = "Limit the commit rate once the number of pending revisions in the observation " +
                    "queue exceed 90% of its capacity.")
    private static final String COMMIT_RATE_LIMIT = "oak.observation.limit-commit-rate";

    public static final String DEFAULT_ADMIN_USER = "admin";

    @Property(
            value = DEFAULT_ADMIN_USER,
            label = "Administrator",
            description = "The user name of the administrative user. This user"
                + "name is used to implement the SlingRepository.loginAdministrative(String)"
                + "method. It is intended for this user to provide full read/write access to repository.")
    public static final String PROPERTY_ADMIN_USER = "admin.name";

    @Property(
        value = "oak-sling-repository",
        label = "Repository Name",
        description = "The name under which the repository will be registered in JNDI and RMI registries."
    )
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    @Reference
    private ServiceUserMapper serviceUserMapper;

    @Reference
    private NodeStore nodeStore;

    private ComponentContext componentContext;

    private String adminUserName;

    @Reference
    private ThreadPoolManager threadPoolManager = null;

    private ThreadPool threadPool;

    private ServiceRegistration oakExecutorServiceReference;

    private final WhiteboardIndexProvider indexProvider = new WhiteboardIndexProvider();

    private final WhiteboardIndexEditorProvider indexEditorProvider = new WhiteboardIndexEditorProvider();

    private int observationQueueLength;

    private CommitRateLimiter commitRateLimiter;

    @Property(
            boolValue=true,
            label="Allow anonymous reads",
            description="If true, the anonymous user has read access to the whole repository (for backwards compatibility)")
    public static final String ANONYMOUS_READ_PROP = "anonymous.read.all";

    @Override
    protected ServiceUserMapper getServiceUserMapper() {
        return this.serviceUserMapper;
    }

    @Reference(policy = STATIC, policyOption = GREEDY)
    private SecurityProvider securityProvider = null;

    private ServiceRegistration nodeAggregator;

    @Override
    protected Repository acquireRepository() {
        this.adminUserName = securityProvider.getConfiguration(UserConfiguration.class).getParameters().getConfigValue(
            UserConstants.PARAM_ADMIN_ID, UserConstants.DEFAULT_ADMIN_ID);

        final Whiteboard whiteboard = new OsgiWhiteboard(this.getComponentContext().getBundleContext());
        this.indexProvider.start(whiteboard);
        this.indexEditorProvider.start(whiteboard);
        this.oakExecutorServiceReference = this.componentContext.getBundleContext().registerService(
                Executor.class.getName(), new Executor() {
            @Override
            public void execute(Runnable command) {
                threadPool.execute(command);
            }
        }, new Hashtable<String, Object>());

        final Oak oak = new Oak(nodeStore)
        .with(new InitialContent())
        .with(new ExtraSlingContent())

        .with(JcrConflictHandler.createJcrConflictHandler())
        .with(new VersionHook())

        .with(securityProvider)

        .with(new NameValidatorProvider())
        .with(new NamespaceEditorProvider())
        .with(new TypeEditorProvider())
//        .with(new RegistrationEditorProvider())
        .with(new ConflictValidatorProvider())

        // index stuff
        .with(indexProvider)
        .with(indexEditorProvider)
        .with(getDefaultWorkspace())
        .withAsyncIndexing()
        .with(whiteboard)
        ;
        
        if (commitRateLimiter != null) {
            oak.with(commitRateLimiter);
        }

        final ContentRepository contentRepository = oak.createContentRepository();
        final boolean fastQueryResultSize = true;
        return new OsgiRepository(
                contentRepository, whiteboard, securityProvider, observationQueueLength, 
                commitRateLimiter, fastQueryResultSize);
    }

    private void setup(BundleContext bundleContext, SlingRepository repository) {

        final Object o = this.getComponentContext().getProperties().get(ANONYMOUS_READ_PROP);
        if(o != null) {
            if(Boolean.valueOf(o.toString())) {
                log.warn("{} is true, granting anonymous user read access on /", ANONYMOUS_READ_PROP);
                Session s = null;
                try {
                    // TODO do we need to go via PrivilegeManager for the names? See OAK-1016 example.
                    s = repository.loginAdministrative(getDefaultWorkspace());
                    final String [] privileges = new String[] { Privilege.JCR_READ };
                    AccessControlUtils.addAccessControlEntry(
                            s,
                            "/",
                            EveryonePrincipal.getInstance(),
                            privileges,
                            true);
                    s.save();
                } catch (RepositoryException re) {
                    log.error("TODO: Failed setting up anonymous access", re);
                } finally {
                    if (s != null) {
                        s.logout();
                    }
                }
            } else {
                log.warn("TODO: should disable anonymous access when {} becomes false", ANONYMOUS_READ_PROP);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Dictionary<String, Object> getServiceRegistrationProperties() {
        return this.getComponentContext().getProperties();
    }

    @Override
    protected AbstractSlingRepository2 create(Bundle usingBundle) {
        final AbstractSlingRepository2 result = new OakSlingRepository(this, usingBundle, this.adminUserName);
        setup(usingBundle.getBundleContext(), result);
        return result;
    }

    @Override
    protected void destroy(AbstractSlingRepository2 repositoryServiceInstance) {
        // nothing to do, just drop the reference
    }

    @Override
    protected void disposeRepository(Repository repository) {
        this.indexProvider.stop();
        this.indexEditorProvider.stop();
        this.oakExecutorServiceReference.unregister();
        this.oakExecutorServiceReference = null;
        ((JackrabbitRepository) repository).shutdown();
        this.adminUserName = null;
    }

    private ComponentContext getComponentContext() {
        return componentContext;
    }

    @Activate
    private void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = componentContext.getProperties();
        final String defaultWorkspace = PropertiesUtil.toString(properties.get(PROPERTY_DEFAULT_WORKSPACE), "default");
        final boolean disableLoginAdministrative = !PropertiesUtil.toBoolean(
            properties.get(PROPERTY_LOGIN_ADMIN_ENABLED), DEFAULT_LOGIN_ADMIN_ENABLED);

        this.adminUserName = PropertiesUtil.toString(properties.get(PROPERTY_ADMIN_USER), DEFAULT_ADMIN_USER);
        this.observationQueueLength = getObservationQueueLength(componentContext);
        this.commitRateLimiter = getCommitRateLimiter(componentContext);
        this.threadPool = threadPoolManager.get("oak-observation");
        this.nodeAggregator = componentContext.getBundleContext()
                .registerService(NodeAggregator.class.getName(), getNodeAggregator(), null);

        super.start(componentContext.getBundleContext(), defaultWorkspace, disableLoginAdministrative);
    }

    @Deactivate
    private void deactivate() {
        super.stop();
        this.componentContext = null;
        this.threadPoolManager.release(this.threadPool);
        this.threadPool = null;
        this.nodeAggregator.unregister();
    }

    private static NodeAggregator getNodeAggregator() {
        return new SimpleNodeAggregator()
            .newRuleWithName("nt:file", Arrays.asList(new String [] {"jcr:content"}))
            ;
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
                property(index, "slingeventEventId", "slingevent:eventId");
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

    private static int getObservationQueueLength(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Object value = properties.get(OBSERVATION_QUEUE_LENGTH);
        if (isNullOrEmpty(value)) {
            value = context.getBundleContext().getProperty(OBSERVATION_QUEUE_LENGTH);
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return DEFAULT_OBSERVATION_QUEUE_LENGTH;
        }
    }

    private static CommitRateLimiter getCommitRateLimiter(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Object value = properties.get(COMMIT_RATE_LIMIT);
        if (isNullOrEmpty(value)) {
            value = context.getBundleContext().getProperty(COMMIT_RATE_LIMIT);
        }
        return Boolean.parseBoolean(String.valueOf(value))
            ? new CommitRateLimiter()
            : null;
    }

    private static boolean isNullOrEmpty(Object value) {
        return (value == null || value.toString().trim().length() == 0);
    }
}

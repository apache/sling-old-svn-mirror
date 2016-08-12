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
package org.apache.sling.jcr.oak.server;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executor;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singleton;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;

/**
 * A Sling repository implementation that wraps the Oak repository
 * implementation from the Jackrabbit Oak project.
 */
@Component(
    immediate = true
)
@Designate(
    ocd = OakSlingRepositoryManagerConfiguration.class
)
public class OakSlingRepositoryManager extends AbstractSlingRepositoryManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ServiceUserMapper serviceUserMapper;

    @Reference
    private NodeStore nodeStore;

    private ComponentContext componentContext;

    @Reference
    private ThreadPoolManager threadPoolManager = null;

    private ThreadPool threadPool;

    private ServiceRegistration oakExecutorServiceReference;

    private final WhiteboardIndexProvider indexProvider = new WhiteboardIndexProvider();

    private final WhiteboardIndexEditorProvider indexEditorProvider = new WhiteboardIndexEditorProvider();

    private CommitRateLimiter commitRateLimiter;

    private OakSlingRepositoryManagerConfiguration configuration;

    @Reference(
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private SecurityProvider securityProvider = null;

    private ServiceRegistration nodeAggregatorRegistration;

    public OakSlingRepositoryManager() {
    }

    @Override
    protected ServiceUserMapper getServiceUserMapper() {
        return this.serviceUserMapper;
    }

    @Override
    protected Repository acquireRepository() {
        final BundleContext bundleContext = componentContext.getBundleContext();
        final Whiteboard whiteboard = new OsgiWhiteboard(bundleContext);
        this.indexProvider.start(whiteboard);
        this.indexEditorProvider.start(whiteboard);
        this.oakExecutorServiceReference = bundleContext.registerService(
                Executor.class.getName(), new Executor() {
            @Override
            public void execute(Runnable command) {
                threadPool.execute(command);
            }
        }, new Hashtable<String, Object>());

        final Oak oak = new Oak(nodeStore)
            .withAsyncIndexing("async", 5);

        final Jcr jcr = new Jcr(oak, false)
        .with(new InitialContent())
        .with(new ExtraSlingContent())

        .with(JcrConflictHandler.createJcrConflictHandler())
        .with(new VersionHook())

        .with(securityProvider)

        .with(new NameValidatorProvider())
        .with(new NamespaceEditorProvider())
        .with(new TypeEditorProvider())
        .with(new ConflictValidatorProvider())

        // index stuff
        .with(indexProvider)
        .with(indexEditorProvider)
        .with(getDefaultWorkspace())
        .with(whiteboard)
        .withFastQueryResultSize(true)
        .withObservationQueueLength(configuration.oak_observation_queue_length());

        if (commitRateLimiter != null) {
            jcr.with(commitRateLimiter);
        }

        jcr.createContentRepository();

        return new TCCLWrappingJackrabbitRepository((JackrabbitRepository) jcr.createRepository());
    }

    private void setup(final SlingRepository repository) {
        final boolean anonymous_read_all = configuration.anonymous_read_all();
        if (anonymous_read_all) {
            log.warn("anonymous.read.all is true, granting anonymous user read access on /");
            Session session = null;
            try {
                // TODO do we need to go via PrivilegeManager for the names? See OAK-1016 example.
                session = repository.loginAdministrative(getDefaultWorkspace());
                final String[] privileges = new String[]{Privilege.JCR_READ};
                AccessControlUtils.addAccessControlEntry(
                    session,
                    "/",
                    EveryonePrincipal.getInstance(),
                    privileges,
                    true);
                session.save();
            } catch (RepositoryException re) {
                log.error("TODO: Failed setting up anonymous access", re);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            log.warn("TODO: should disable anonymous access when anonymous.read.all becomes false");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Dictionary<String, Object> getServiceRegistrationProperties() {
        return componentContext.getProperties();
    }

    @Override
    protected AbstractSlingRepository2 create(Bundle usingBundle) {
        final String adminId = getAdminId();
        final AbstractSlingRepository2 slingRepository = new OakSlingRepository(this, usingBundle, adminId);
        setup(slingRepository);
        return slingRepository;
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
    }

    @Activate
    private void activate(final OakSlingRepositoryManagerConfiguration configuration, final ComponentContext componentContext) {
        this.configuration = configuration;
        this.componentContext = componentContext;
        final BundleContext bundleContext = componentContext.getBundleContext();

        final String defaultWorkspace = configuration.defaultWorkspace();
        final boolean disableLoginAdministrative = !configuration.admin_login_enabled();

        if (configuration.oak_observation_limitCommitRate()) {
            commitRateLimiter = new CommitRateLimiter();
        }
        this.threadPool = threadPoolManager.get("oak-observation");
        this.nodeAggregatorRegistration = bundleContext.registerService(NodeAggregator.class.getName(), getNodeAggregator(), null);

        super.start(bundleContext, defaultWorkspace, disableLoginAdministrative);
    }

    @Deactivate
    private void deactivate() {
        super.stop();
        this.componentContext = null;
        this.threadPoolManager.release(this.threadPool);
        this.threadPool = null;
        this.nodeAggregatorRegistration.unregister();
    }

    private String getAdminId() {
        return securityProvider.getConfiguration(UserConfiguration.class).getParameters().getConfigValue(UserConstants.PARAM_ADMIN_ID, UserConstants.DEFAULT_ADMIN_ID);
    }

    private static NodeAggregator getNodeAggregator() {
        return new SimpleNodeAggregator().newRuleWithName(JcrConstants.NT_FILE, Collections.singletonList(JcrConstants.JCR_CONTENT));
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

}

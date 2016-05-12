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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.NamingException;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.plugins.name.NamespaceEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.whiteboard.DefaultWhiteboard;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.commons.testing.jcr.RepositoryUtil.RepositoryWrapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTestHelper {

    private final static Logger logger = LoggerFactory.getLogger(RepositoryTestHelper.class);
    
    public static void dumpRepo(ResourceResolverFactory resourceResolverFactory) throws Exception {
        Session session = resourceResolverFactory
                .getAdministrativeResourceResolver(null).adaptTo(Session.class);
        logger.info("dumpRepo: ====== START =====");
        logger.info("dumpRepo: repo = " + session.getRepository());

        dump(session.getRootNode());

        // session.logout();
        logger.info("dumpRepo: ======  END  =====");

        session.logout();
    }
    
    public static void dump(Node node) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")
                || node.getPath().equals("/rep:policy")) {
            // ignore that one
            return;
        }
        PropertyIterator pi = node.getProperties();
        StringBuilder sb = new StringBuilder();
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            sb.append(" ");
            sb.append(p.getName());
            sb.append("=");
            if (p.getType() == PropertyType.BOOLEAN) {
                sb.append(p.getBoolean());
            } else if (p.getType() == PropertyType.STRING) {
                sb.append(p.getString());
            } else if (p.getType() == PropertyType.DATE) {
                sb.append(p.getDate().getTime());
            } else if (p.getType() == PropertyType.LONG) {
                sb.append(p.getLong());
            } else {
                sb.append("<unknown type=" + p.getType() + "/>");
            }
        }

        StringBuffer depth = new StringBuffer();
        for(int i=0; i<node.getDepth(); i++) {
            depth.append(" ");
        }
        logger.info(depth + "/" + node.getName() + " -- " + sb);
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            dump(child);
        }
    }

    static Map<SlingRepository,Session> adminSessions = new HashMap<SlingRepository, Session>();
    /** from commons.testing.jcr **/
    public static final String CONFIG_FILE = "jackrabbit-test-config.xml";

    public static SlingRepository newOakRepository(NodeStore nodeStore) throws RepositoryException {
            SlingRepository repository = new RepositoryWrapper(createOakRepository(nodeStore));
            return repository;
        }

    public static void initSlingNodeTypes(SlingRepository repository) throws RepositoryException {
        Session adminSession = null;
        try {
            adminSession = repository.loginAdministrative(null);
            RepositoryUtil.registerSlingNodeTypes(adminSession);
        } catch ( final IOException ioe ) {
            throw new RepositoryException(ioe);
        } finally {
            if ( adminSession != null ) {
                adminSession.logout();
            }
        }
    }

    /**
     * Stop a repository.
     */
    public static void stopRepository(SlingRepository repository) throws NamingException {
        Session adminSession = adminSessions.remove(repository);
        if ( adminSession != null ) {
            adminSession.logout();
        }
    }

    public static Repository createOakRepository() {
        return createOakRepository(new MemoryNodeStore());
    }
    
    public static Repository createOakRepository(NodeStore nodeStore) {
        DefaultWhiteboard whiteboard = new DefaultWhiteboard();
        final Oak oak = new Oak(nodeStore)
        .with(new InitialContent())
//        .with(new ExtraSlingContent())

        .with(JcrConflictHandler.createJcrConflictHandler())
        .with(new EditorHook(new VersionEditorProvider()))

        .with(new OpenSecurityProvider())

//        .with(new ValidatorProvider() {
//
//            @Override
//            public Validator getRootValidator(
//                    NodeState before, NodeState after, CommitInfo info) {
//                HashSet<String> set = newHashSet(after
//                                .getChildNode(JCR_SYSTEM)
//                                .getChildNode(REP_NAMESPACES)
//                                .getChildNode(REP_NSDATA)
//                                .getStrings(REP_PREFIXES));
//                set.add("sling");
//                return new NameValidator(set);
//            }
//        })
        .with(new NamespaceEditorProvider())
        .with(new TypeEditorProvider())
//        .with(new RegistrationEditorProvider())
        .with(new ConflictValidatorProvider())

        // index stuff
//        .with(indexProvider)
//        .with(indexEditorProvider)
        .with("default")//getDefaultWorkspace())
//        .withAsyncIndexing()
        .with(whiteboard)
        ;
        
//        if (commitRateLimiter != null) {
//            oak.with(commitRateLimiter);
//        }

        final ContentRepository contentRepository = oak.createContentRepository();
        return new RepositoryImpl(contentRepository, whiteboard, new OpenSecurityProvider(), 1000, null);
    }

    public static void resetRepo() throws Exception {
        Session l = RepositoryProvider.instance().getRepository()
                .loginAdministrative(null);
        try {
            l.removeItem("/var");
            l.save();
            l.logout();
        } catch (Exception e) {
            l.refresh(false);
            l.logout();
        }
    }

    public static ResourceResolverFactory mockResourceResolverFactory(final SlingRepository repositoryOrNull)
            throws Exception {
        Mockery context = new JUnit4Mockery(){{
        	// @see http://www.jmock.org/threading-synchroniser.html
            setThreadingPolicy(new Synchroniser());
        }};
    
        final ResourceResolverFactory resourceResolverFactory = context
                .mock(ResourceResolverFactory.class);
        // final ResourceResolver resourceResolver = new MockResourceResolver();
        // final ResourceResolver resourceResolver = new
        // MockedResourceResolver();
    
        context.checking(new Expectations() {
            {
                allowing(resourceResolverFactory)
                        .getAdministrativeResourceResolver(null);
                will(new Action() {
    
                    public Object invoke(Invocation invocation)
                            throws Throwable {
                    	return new MockedResourceResolver(repositoryOrNull);
                    }
    
                    public void describeTo(Description arg0) {
                        arg0.appendText("whateva - im going to create a new mockedresourceresolver");
                    }
                });
            }
        });
        return resourceResolverFactory;
    }


}

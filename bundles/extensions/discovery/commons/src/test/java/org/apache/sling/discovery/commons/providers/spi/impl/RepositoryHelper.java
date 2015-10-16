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
package org.apache.sling.discovery.commons.providers.spi.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.jackrabbit.commons.JcrUtils;
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
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.commons.testing.jcr.RepositoryUtil.RepositoryWrapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryHelper {

    private static class ShutdownThread extends Thread {
        
        private SlingRepository repository;
        
        public ShutdownThread(SlingRepository repository) {
            this.repository = repository;
        }
        @Override
        public void run() {
            try {
                stopRepository(repository);
            } catch(Exception e) {
                System.out.println("Exception in ShutdownThread:" + e);
            }
        }
        
    }

    private final static Logger logger = LoggerFactory.getLogger(RepositoryHelper.class);
    
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

    public static SlingRepository newRepository(String homeDir) throws RepositoryException {
        SlingRepository repository = startRepository(homeDir);
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(repository));
        return repository;
    }

    public static SlingRepository newOakRepository(NodeStore nodeStore) throws RepositoryException {
            SlingRepository repository = new RepositoryWrapper(createOakRepository(nodeStore));
    //        Runtime.getRuntime().addShutdownHook(new ShutdownThread(repository));
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
     * Start a new repository
     * @return 
     *
     * @throws RepositoryException when it is not possible to start the
     *             repository.
     */
    private static RepositoryWrapper startRepository(String homeDir) throws RepositoryException {
        // copy the repository configuration file to the repository HOME_DIR
        InputStream ins = RepositoryUtil.class.getClassLoader().getResourceAsStream(
            CONFIG_FILE);
        if (ins == null) {
            throw new RepositoryException("Cannot get " + CONFIG_FILE);
        }
    
        File configFile = new File(homeDir, "repository.xml");
        configFile.getParentFile().mkdirs();
    
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(configFile);
            byte[] buf = new byte[1024];
            int rd;
            while ((rd = ins.read(buf)) >= 0) {
                out.write(buf, 0, rd);
            }
        } catch (IOException ioe) {
            throw new RepositoryException("Cannot copy configuration file to "
                + configFile);
        } finally {
            try {
                ins.close();
            } catch (IOException ignore) {
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    
        // somewhat dirty hack to have the derby.log file in a sensible
        // location, but don't overwrite anything already set
        if (System.getProperty("derby.stream.error.file") == null) {
            String derbyLog = homeDir + "/derby.log";
            System.setProperty("derby.stream.error.file", derbyLog);
        }
    
        final File f = new File(homeDir);
        RepositoryWrapper repository = new RepositoryWrapper(JcrUtils.getRepository(f.toURI().toString()));
        Session adminSession = repository.loginAdministrative(null);
        adminSessions.put(repository, adminSession);
        return repository;
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


}

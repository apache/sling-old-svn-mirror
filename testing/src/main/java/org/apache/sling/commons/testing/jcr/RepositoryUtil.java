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
package org.apache.sling.commons.testing.jcr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.naming.NamingException;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * Utility class for managing JCR repositories, used to initialize temporary
 * Jackrabbit repositories for testing.
 */
public class RepositoryUtil {

    public static final String ADMIN_NAME = "admin";

    public static final String ADMIN_PASSWORD = "admin";

    public static final String HOME_DIR = "target/repository";

    public static final String CONFIG_FILE = "jackrabbit-test-config.xml";

    private static SlingRepository repository;

    private static Session adminSession;

    /**
     * Start a new repository
     *
     * @throws RepositoryException when it is not possible to start the
     *             repository.
     */
    public static void startRepository() throws RepositoryException {
        if ( adminSession == null ) {
            // copy the repository configuration file to the repository HOME_DIR
            InputStream ins = RepositoryUtil.class.getClassLoader().getResourceAsStream(
                CONFIG_FILE);
            if (ins == null) {
                throw new RepositoryException("Cannot get " + CONFIG_FILE);
            }

            File configFile = new File(HOME_DIR, "repository.xml");
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
                String derbyLog = HOME_DIR + "/derby.log";
                System.setProperty("derby.stream.error.file", derbyLog);
            }

            final File f = new File(HOME_DIR);
            repository = new RepositoryWrapper(JcrUtils.getRepository(f.toURI().toString()));
            adminSession = repository.loginAdministrative(null);
        }
    }

    /**
     * Stop a repository.
     */
    public static void stopRepository() throws NamingException {
        if ( adminSession != null ) {
            adminSession.logout();
            adminSession = null;
            repository = null;
        }
    }

    /**
     * Get a repository
     *
     * @return a JCR repository reference
     */
    public static SlingRepository getRepository() {
        return repository;
    }

    /**
     * Registers node types from the CND file read from the <code>source</code>
     * with the node type manager available from the given <code>session</code>.
     * <p>
     * This method is not synchronized. It is up to the calling method to
     * prevent paralell execution.
     *
     * @param session The <code>Session</code> providing the node type manager
     *            through which the node type is to be registered.
     * @param source The <code>InputStream</code> from which the CND file is
     *            read.
     * @return <code>true</code> if registration of all node types succeeded.
     */
    public static boolean registerNodeType(Session session, InputStream source)
            throws IOException, RepositoryException {
        try {
            CndImporter.registerNodeTypes(new InputStreamReader(source, "UTF-8"), session);
            return true;
        } catch (Exception e) {
            // ignore
            return false;
        }
    }

    public static void registerSlingNodeTypes(Session adminSession) throws IOException, RepositoryException {
        final Class<RepositoryUtil> clazz = RepositoryUtil.class;
        registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/folder.cnd"));
        RepositoryUtil.registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/resource.cnd"));
        RepositoryUtil.registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/vanitypath.cnd"));
    }

    public static final class RepositoryWrapper implements SlingRepository {

        protected final Repository wrapped;

        public RepositoryWrapper(Repository r) {
            wrapped = r;
        }

        public String getDescriptor(String key) {
            return wrapped.getDescriptor(key);
        }

        public String[] getDescriptorKeys() {
            return wrapped.getDescriptorKeys();
        }

        public Session login() throws LoginException, RepositoryException {
            return wrapped.login();
        }

        public Session login(Credentials credentials, String workspaceName)
                throws LoginException, NoSuchWorkspaceException,
                RepositoryException {
            return wrapped.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
        }

        public Session login(Credentials credentials) throws LoginException,
                RepositoryException {
            return wrapped.login(credentials);
        }

        public Session login(String workspaceName) throws LoginException,
                NoSuchWorkspaceException, RepositoryException {
            return wrapped.login((workspaceName == null ? getDefaultWorkspace() : workspaceName));
        }

        public String getDefaultWorkspace() {
            return "default";
        }

        public Session loginAdministrative(String workspaceName)
                throws RepositoryException {
            final Credentials credentials = new SimpleCredentials(ADMIN_NAME,
                ADMIN_PASSWORD.toCharArray());
            return this.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
        }

        public Value getDescriptorValue(String key) {
            return wrapped.getDescriptorValue(key);
        }

        public Value[] getDescriptorValues(String key) {
            return wrapped.getDescriptorValues(key);
        }

        public boolean isSingleValueDescriptor(String key) {
            return wrapped.isSingleValueDescriptor(key);
        }

        public boolean isStandardDescriptor(String key) {
            return wrapped.isStandardDescriptor(key);
        }

    }
}

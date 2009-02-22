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
import java.util.Hashtable;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * Utility class for managing JCR repositories, used to initialize temporary
 * Jackrabbit repositories for testing.
 */
public class RepositoryUtil {

    public static final String REPOSITORY_NAME = "repositoryTest";

    public static final String ADMIN_NAME = "admin";

    public static final String ADMIN_PASSWORD = "admin";

    public static final String CONTEXT_FACTORY = "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory";

    public static final String PROVIDER_URL = "localhost";

    public static final String CONFIG_FILE = "jackrabbit-test-config.xml";

    public static final String HOME_DIR = "target/repository";

    protected static InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL, PROVIDER_URL);
        return new InitialContext(env);
    }

    /**
     * Start a new repository
     * 
     * @throws RepositoryException when it is not possible to start the
     *             repository.
     * @throws NamingException
     */
    public static void startRepository() throws RepositoryException,
            NamingException {

        // copy the repository configuration file to the repository HOME_DIR
        InputStream ins = RepositoryUtil.class.getClassLoader().getResourceAsStream(
            CONFIG_FILE);
        if (ins == null) {
            throw new RepositoryException("Cannot get " + CONFIG_FILE);
        }

        File configFile = new File(HOME_DIR, CONFIG_FILE);
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

        RegistryHelper.registerRepository(getInitialContext(), REPOSITORY_NAME,
            configFile.getPath(), HOME_DIR, true);
    }

    /**
     * Stop a repository.
     * 
     * @throws NamingException when it is not possible to stop the repository
     * @throws NamingException
     */
    public static void stopRepository() throws NamingException {
        RegistryHelper.unregisterRepository(getInitialContext(),
            REPOSITORY_NAME);
    }

    /**
     * Get a repository
     * 
     * @return a JCR repository reference
     * @throws NamingException when it is not possible to get the repository.
     *             Before calling this method, the repository has to be
     *             registered (@see RepositoryUtil#registerRepository(String,
     *             String, String)
     * @throws NamingException
     */
    public static SlingRepository getRepository() throws NamingException {
        return new RepositoryWrapper((Repository) getInitialContext().lookup(
            REPOSITORY_NAME));
    }

    /**
     * Registers node types from the CND file read from the <code>source</code>
     * with the node type manager available from the given <code>session</code>.
     * <p>
     * The <code>NodeTypeManager</code> returned by the <code>session</code>'s
     * workspace is expected to be of type
     * <code>org.apache.jackrabbit.api.JackrabbitNodeTypeManager</code> for
     * the node type registration to succeed.
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

        Workspace workspace = session.getWorkspace();
        NodeTypeManager ntm = workspace.getNodeTypeManager();
        if (ntm instanceof JackrabbitNodeTypeManager) {
            JackrabbitNodeTypeManager jntm = (JackrabbitNodeTypeManager) ntm;
            try {
                jntm.registerNodeTypes(source,
                    JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
                return true;
            } catch (RepositoryException re) {
                Throwable t = re.getCause();
                if (t != null
                    && t.getClass().getName().endsWith(
                        ".InvalidNodeTypeDefException")) {
                    // hacky wacky: interpret message to check whether it is for
                    // duplicate node type -> very bad, that this is the only
                    // way to check !!!
                    if (re.getCause().getMessage().indexOf("already exists") >= 0) {
                        // alright, node types are already registered, ignore
                        // this
                        return true;
                    }
                }

                // get here to rethrow the RepositoryException
                throw re;
            }
        }

        return false;
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
            return wrapped.login(credentials, workspaceName);
        }

        public Session login(Credentials credentials) throws LoginException,
                RepositoryException {
            return wrapped.login(credentials);
        }

        public Session login(String workspaceName) throws LoginException,
                NoSuchWorkspaceException, RepositoryException {
            return wrapped.login(workspaceName);
        }

        public String getDefaultWorkspace() {
            return "default";
        }

        public Session loginAdministrative(String workspace)
                throws RepositoryException {
            final Credentials credentials = new SimpleCredentials(ADMIN_NAME,
                ADMIN_PASSWORD.toCharArray());
            return this.login(credentials, workspace);
        }

    }
}

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
package org.apache.sling.jcr.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>NodeTypeSupport</code> contains utility methods to register node
 * types from a <a href="http://jackrabbit.apache.org/doc/nodetype/cnd.html">CND
 * nodetype definition</a> file given as an URL or InputStream with the
 * repository.
 */
public class NodeTypeLoader {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(NodeTypeLoader.class);

    /**
     * Registers node types from the CND file accessible by the <code>URL</code>
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
     * @param source The URL from which to read the CND file
     * @return <code>true</code> if registration of all node types succeeded.
     */
    public static boolean registerNodeType(Session session, URL source) {

        // Access the node type definition file, "fail" if not available
        if (source == null) {
            log.info("No node type definition source available");
            return false;
        }

        InputStream ins = null;
        try {
            ins = source.openStream();
            return registerNodeType(session, ins);
        } catch (IOException ioe) {
            log.error("Cannot register node types from " + source, ioe);
        } catch (RepositoryException re) {
            if(isReRegisterBuiltinNodeType(re)) {
                log.debug("Attempt to re-register built-in node type from " + source, re);
            } else {
                log.error("Cannot register node types from " + source, re);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

        // fall back to failure, reason has been logged
        return false;
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
        return registerNodeType(session, "cnd input stream", new InputStreamReader(source), false);
    }

    public static boolean registerNodeType(Session session, String systemId, Reader reader, boolean reregisterExisting)
        throws IOException, RepositoryException {
        try {
            Workspace wsp = session.getWorkspace();
            CndImporter.registerNodeTypes(reader, systemId, wsp.getNodeTypeManager(), wsp.getNamespaceRegistry(), session.getValueFactory(), reregisterExisting);
        } catch(RepositoryException re) {
            if(isReRegisterBuiltinNodeType(re)) {
                log.debug("Attempt to re-register built-in node type, RepositoryException ignored", re);
            } else {
                throw re;
            }
        } catch (ParseException e) {
            throw new IOException("Unable to parse CND Input: " + e.getMessage());
        }
        return true;
    }
    
    /** True if we think e was caused by an attempt to reregister a built-in node type */
    static boolean isReRegisterBuiltinNodeType(Exception e) {
        return 
            (e instanceof RepositoryException)
            & (e.getMessage() != null && e.getMessage().contains("reregister built-in node type"));
    }
}

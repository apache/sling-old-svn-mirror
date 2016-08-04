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
package org.apache.sling.ide.impl.vlt;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository.CommandExecutionFlag;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;

public abstract class JcrCommand<T> implements Command<T> {

    private final Credentials credentials;
    private final Repository repository;
    private final String path;
    private final Logger logger;
    private final EnumSet<CommandExecutionFlag> flags;

    public JcrCommand(Repository repository, Credentials credentials, String path, Logger logger,
            CommandExecutionFlag... flags) {

        this.repository = repository;
        this.credentials = credentials;
        this.path = path;
        this.logger = logger;
        this.flags = EnumSet.noneOf(CommandExecutionFlag.class);
        this.flags.addAll(Arrays.asList(flags));
    }

    @Override
    public Result<T> execute() {

        Session session = null;
        try {
            session = repository.login(credentials);

            T result = execute0(session);

            session.save();

            return JcrResult.success(result);
        } catch (LoginException e) {
            return JcrResult.failure(e);
        } catch (RepositoryException e) {
            return JcrResult.failure(e);
        } catch (IOException e) {
            return JcrResult.failure(e);
        } finally {
            if (session != null)
                session.logout();
        }
    }

    protected abstract T execute0(Session session) throws RepositoryException, IOException;

    public String getPath() {
        return path;
    }

    protected Logger getLogger() {
        return logger;
    }

    public Set<CommandExecutionFlag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }
    
    @Override
    public Kind getKind() {
        return null;
    }

    protected ResourceProxy nodeToResource(Node node) throws RepositoryException {
    
        ResourceProxy resource = new ResourceProxy(node.getPath());
        resource.addAdapted(Node.class, node);

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            String propertyName = property.getName();
            Object propertyValue = ConversionUtils.getPropertyValue(property);
    
            if (propertyValue != null) {
                resource.addProperty(propertyName, propertyValue);
            }
        }
    
        return resource;
    
    }

    /**
     * Recursively prints this node and all its children
     * 
     * <p>
     * Only the node name and the primary node type are printed, with children being indented
     * 
     * @param node the node to start at
     * @param ps the stream to print to
     * @throws RepositoryException
     */
    protected void dumpNode(Node node, PrintStream ps) throws RepositoryException {

        printNode(node, ps);
        writeChildren(node, 1, ps);
    }

    private void printNode(Node node, PrintStream ps) throws RepositoryException {

        ps.println(node.getName() + " [" + node.getPrimaryNodeType().getName() + "]");
    }

    private void writeChildren(Node parent, int depth, PrintStream ps) throws RepositoryException {

        for (NodeIterator it = parent.getNodes(); it.hasNext();) {
            for (int i = 0; i < depth; i++) {
                ps.append(' ');
            }
            Node child = it.nextNode();
            printNode(child, ps);
            writeChildren(child, depth + 1, ps);
        }
    }
}

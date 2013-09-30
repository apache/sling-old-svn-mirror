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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;

public abstract class JcrCommand<T> implements Command<T> {

    private final Credentials credentials;
    private final Repository repository;
    private final String path;

    public JcrCommand(Repository repository, Credentials credentials, String path) {

        this.repository = repository;
        this.credentials = credentials;
        this.path = path;
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
}

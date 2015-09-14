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

package org.apache.sling.launchpad.testservices.handlers;

import org.apache.jackrabbit.server.io.DeleteContext;
import org.apache.jackrabbit.server.io.DeleteHandler;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public abstract class AbstractDeleteHandler implements DeleteHandler {

    protected String HANDLER_NAME = "";
    protected String HANDLER_BKP;

    @Override
    public boolean delete(DeleteContext deleteContext,
            DavResource resource) throws DavException {
        try {
            deleteContext.getSession().getWorkspace().move(resource.getResourcePath(), resource.getResourcePath() + HANDLER_BKP);
            return true;
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    public boolean canDelete(DeleteContext deleteContext,
            DavResource resource) {
        try {
            Node nodeToDelete = deleteContext.getSession().getNode(resource.getResourcePath());
            return HANDLER_NAME.equals(nodeToDelete.getName());
        } catch (RepositoryException e) {
            return false;
        }
    }

}

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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.ResourceProxy;

public class GetNodeContentCommand extends JcrCommand<ResourceProxy> {

    public GetNodeContentCommand(Repository repository, Credentials credentials, String path, Logger logger) {
        super(repository, credentials, path, logger);
    }

    @Override
    protected ResourceProxy execute0(Session session) throws RepositoryException {

        Node node = session.getNode(getPath());

        return nodeToResource(node);
    }
}

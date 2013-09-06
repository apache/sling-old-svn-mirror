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
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.ide.transport.FileInfo;

public class UpdateNodePropertiesCommand extends JcrCommand<Void> {

    private final Map<String, Object> serializationData;

    public UpdateNodePropertiesCommand(Repository jcrRepo, Credentials credentials, FileInfo fileInfo,
            Map<String, Object> serializationData) {

        // intention since the fileInfo refers to the .content.xml file ( TODO - should we change that )?
        super(jcrRepo, credentials, PathUtil.makePath(fileInfo.getRelativeLocation(), ""));

        this.serializationData = serializationData;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        Node node = session.getNode(getPath());

        for (Map.Entry<String, Object> entry : serializationData.entrySet()) {

            if (node.hasProperty(entry.getKey())) {

                Property prop = node.getProperty(entry.getKey());
                if (prop.getDefinition().isProtected()) {
                    continue;
                }

                if (prop.getType() != PropertyType.STRING)
                    throw new UnsupportedOperationException("Unable to set value of property '" + prop.getName()
                            + "' since its type is '" + prop.getType() + "'");
            }

            node.setProperty(entry.getKey(), entry.getValue().toString());
        }

        return null;

    }

}

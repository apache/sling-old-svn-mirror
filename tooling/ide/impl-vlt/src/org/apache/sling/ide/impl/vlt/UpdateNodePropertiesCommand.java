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
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.ide.transport.FileInfo;

public class UpdateNodePropertiesCommand extends JcrCommand<Void> {

    private final Map<String, Object> serializationData;

    public UpdateNodePropertiesCommand(Repository jcrRepo, Credentials credentials, FileInfo fileInfo,
            Map<String, Object> serializationData) {

        // intentional since the fileInfo refers to the .content.xml file ( TODO - should we change that )?
        super(jcrRepo, credentials, PathUtil.makePath(fileInfo.getRelativeLocation(), ""));

        this.serializationData = serializationData;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        Node node = session.getNode(getPath());

        // TODO - review for completeness and filevault compatibility
        // TODO - multi-valued properties
        for (Map.Entry<String, Object> entry : serializationData.entrySet()) {

            if (node.hasProperty(entry.getKey())) {

                Property prop = node.getProperty(entry.getKey());
                if (prop.getDefinition().isProtected()) {
                    continue;
                }

                if (entry.getValue() instanceof String) {
                    node.setProperty(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    node.setProperty(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() instanceof Calendar) {
                    node.setProperty(entry.getKey(), (Calendar) entry.getValue());
                } else if (entry.getValue() instanceof Double) {
                    node.setProperty(entry.getKey(), (Double) entry.getValue());
                } else if (entry.getValue() instanceof BigDecimal) {
                    node.setProperty(entry.getKey(), (BigDecimal) entry.getValue());
                } else if (entry.getValue() instanceof Double) {
                    node.setProperty(entry.getKey(), (Double) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    node.setProperty(entry.getKey(), (Long) entry.getValue());
                } else {
                    throw new IllegalArgumentException("Unable to handle value of type '"
                            + entry.getValue().getClass().getName() + "' for property '" + entry.getKey() + "'");
                }

            }
        }

        return null;

    }

}

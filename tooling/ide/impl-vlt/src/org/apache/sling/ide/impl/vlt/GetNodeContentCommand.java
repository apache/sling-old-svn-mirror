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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class GetNodeContentCommand extends JcrCommand<Map<String, Object>> {

    public GetNodeContentCommand(Repository repository, Credentials credentials, String path) {
        super(repository, credentials, path);
    }

    @Override
    protected Map<String, Object> execute0(Session session) throws RepositoryException {

        Node node = session.getNode(getPath());
        PropertyIterator properties = node.getProperties();

        Map<String, Object> props = new HashMap<String, Object>();

        while (properties.hasNext()) {

            Property property = properties.nextProperty();
            Object value = ConversionUtils.getPropertyValue(property);
            if (value == null) {
                continue;
            }
            props.put(property.getName(), value);
        }

        return props;
    }

}

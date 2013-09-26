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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class GetNodeCommand extends JcrCommand<byte[]> {
    
    public GetNodeCommand(Repository repository, Credentials credentials, String path) {
        super(repository, credentials, path);
    }

    @Override
    protected byte[] execute0(Session session) throws RepositoryException, IOException {

        Node node = session.getNode(getPath());

        Property property;
        if (node.hasProperty("jcr:data")) {
        	property = node.getProperty("jcr:data");
        } else {
	        if (!node.hasNode("jcr:content")) {
	            return null;
	        }
	
	        Node contentNode = node.getNode("jcr:content");
	
	        if (!contentNode.hasProperty("jcr:data")) {
	            return null;
	        }
	
	        property = contentNode.getProperty("jcr:data");
        }

        if (property.getType() == PropertyType.BINARY) {
            Binary binary = property.getBinary();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                byte[] buffer = new byte[2048];
                InputStream stream = binary.getStream();
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                return out.toByteArray();

            } finally {
                binary.dispose();
            }
        }

        return null;
    }

}

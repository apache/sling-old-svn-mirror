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
package org.apache.sling.pipes.internal;

import java.util.Collections;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does a JCR Move of a node, returns the resource corresponding to the moved node
 */
public class MovePipe extends BasePipe {
    Logger logger = LoggerFactory.getLogger(MovePipe.class);

    public static final String RESOURCE_TYPE = RT_PREFIX + "mv";

    public MovePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    @Override
    public boolean modifiesContent() {
        return true;
    }

    @Override
    public Iterator<Resource> getOutput() {
        Iterator<Resource> output = Collections.emptyIterator();
        Resource resource = getInput();
        if (resource != null && resource.adaptTo(Item.class) != null) {
            String targetPath = getExpr();
            try {
                Session session = resolver.adaptTo(Session.class);
                if (session.itemExists(targetPath)){
                    logger.warn("{} already exists, nothing will be done here, nothing outputed");
                } else {
                    logger.info("moving resource {} to {}", resource.getPath(), targetPath);
                    if (!isDryRun()) {
                        if (resource.adaptTo(Node.class) != null) {
                            session.move(resource.getPath(), targetPath);
                        } else {
                            int lastLevel = targetPath.lastIndexOf("/");
                            // /a/b/c will get cut in /a/b for parent path, and c for name
                            String parentPath = targetPath.substring(0, lastLevel);
                            String name = targetPath.substring(lastLevel + 1, targetPath.length());
                            Property sourceProperty = resource.adaptTo(Property.class);
                            Node destNode = session.getNode(parentPath);
                            if (sourceProperty.isMultiple()){
                                destNode.setProperty(name, sourceProperty.getValues(), sourceProperty.getType());
                            } else {
                                destNode.setProperty(name, sourceProperty.getValue(), sourceProperty.getType());
                            }
                            sourceProperty.remove();
                        }
                        Resource target = resolver.getResource(targetPath);
                        output = Collections.singleton(target).iterator();
                    }
                }
            } catch (RepositoryException e){
                logger.error("unable to move the resource", e);
            }
        } else {
            logger.warn("bad configuration of the pipe, will do nothing");
        }
        return output;
    }
}

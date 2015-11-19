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
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.apache.sling.ide.transport.ResourceProxy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ListTreeCommand extends JcrCommand<ResourceProxy> {

    private final int levels;
    private final EventAdmin eventAdmin;

    public ListTreeCommand(Repository repository, Credentials credentials, String path, int levels,
            EventAdmin eventAdmin, Logger logger) {
        super(repository, credentials, path, logger);
        this.levels = Math.max(1,levels);
        this.eventAdmin = eventAdmin;
    }

    @Override
    protected ResourceProxy execute0(Session session) throws RepositoryException {

        Node node = session.getNode(getPath());

        ResourceProxy parent = nodeToResource(node);

        addChildren(parent, node, levels-1);
        
        return parent;
    }
    
    private void addChildren(ResourceProxy parent, Node node, int remainingLevels) throws RepositoryException {
        if (remainingLevels<0) {
            // paranoia check
            throw new IllegalArgumentException("remainingLevels must be >=0, not: "+remainingLevels);
        }
        final long start = System.currentTimeMillis();
        NodeIterator nodes = node.getNodes();
        final long end = System.currentTimeMillis();
        log("ListTreeCommand.child -> "+node.getPath(), start, end);
        while (nodes.hasNext()) {
            Node childNode = nodes.nextNode();

            // TODO - this should not be needed if we obey the vlt filters
            if (childNode.getPath().equals("/jcr:system")) {
                continue;
            }

            final ResourceProxy childResourceProxy = nodeToResource(childNode);
            parent.addChild(childResourceProxy);
            
            if (remainingLevels>0) {
                addChildren(childResourceProxy, childNode, remainingLevels-1);
            }
        }
    }

    private void log(String text, long start, long end) {
        Map<String, Object> props = new HashMap<>();
        props.put(CommandExecutionProperties.RESULT_TEXT, text);
        props.put(CommandExecutionProperties.RESULT_STATUS, Boolean.TRUE);
        props.put(CommandExecutionProperties.ACTION_TYPE, getClass().getSimpleName());
        props.put(CommandExecutionProperties.ACTION_TARGET, getPath());
        props.put(CommandExecutionProperties.TIMESTAMP_START, start);
        props.put(CommandExecutionProperties.TIMESTAMP_END, end);
        Event event = new Event(CommandExecutionProperties.REPOSITORY_TOPIC, props);
        eventAdmin.postEvent(event);
    }

}

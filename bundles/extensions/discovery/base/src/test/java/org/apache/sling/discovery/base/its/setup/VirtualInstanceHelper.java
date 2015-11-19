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
package org.apache.sling.discovery.base.its.setup;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualInstanceHelper {

    private final static Logger logger = LoggerFactory.getLogger(VirtualInstanceHelper.class);

    public static void dumpRepo(ResourceResolverFactory resourceResolverFactory) throws Exception {
        Session session = resourceResolverFactory
                .getAdministrativeResourceResolver(null).adaptTo(Session.class);
        logger.info("dumpRepo: ====== START =====");
        logger.info("dumpRepo: repo = " + session.getRepository());
    
        dump(session.getRootNode());
    
        // session.logout();
        logger.info("dumpRepo: ======  END  =====");
    
        session.logout();
    }

    public static void dump(Node node) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")
                || node.getPath().equals("/rep:policy")) {
            // ignore that one
            return;
        }
    
        PropertyIterator pi = node.getProperties();
        StringBuilder sb = new StringBuilder();
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            sb.append(" ");
            sb.append(p.getName());
            sb.append("=");
            if (p.getType() == PropertyType.BOOLEAN) {
                sb.append(p.getBoolean());
            } else if (p.getType() == PropertyType.STRING) {
                sb.append(p.getString());
            } else if (p.getType() == PropertyType.DATE) {
                sb.append(p.getDate().getTime());
            } else {
                sb.append("<unknown type=" + p.getType() + "/>");
            }
        }
    
        StringBuffer depth = new StringBuffer();
        for(int i=0; i<node.getDepth(); i++) {
            depth.append(" ");
        }
        logger.info(depth + "/" + node.getName() + " -- " + sb);
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            dump(child);
        }
    }

}

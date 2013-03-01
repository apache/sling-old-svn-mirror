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
package org.apache.sling.jcr.resource.internal;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

public abstract class NodeUtil {

    /** Property for the mixin node types. */
    public static final String MIXIN_TYPES = "jcr:mixinTypes";

    /** Property for the node type. */
    public static final String NODE_TYPE = "jcr:primaryType";

    /**
     * Update the mixin node types
     */
    public static void handleMixinTypes(final Node node, final Value[] mixinTypes)
    throws RepositoryException {
        final Set<String> newTypes = new HashSet<String>();
        if ( mixinTypes != null ) {
            for(final Value value : mixinTypes ) {
                newTypes.add(value.getString());
            }
        }
        final Set<String> oldTypes = new HashSet<String>();
        for(final NodeType mixinType : node.getMixinNodeTypes()) {
            oldTypes.add(mixinType.getName());
        }
        for(final String name : oldTypes) {
            if ( !newTypes.contains(name) ) {
                node.removeMixin(name);
            } else {
                newTypes.remove(name);
            }
        }
        for(final String name : newTypes) {
            node.addMixin(name);
        }
    }
}

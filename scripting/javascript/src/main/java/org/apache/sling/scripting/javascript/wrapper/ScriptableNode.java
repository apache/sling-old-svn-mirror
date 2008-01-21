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
package org.apache.sling.scripting.javascript.wrapper;

import java.util.Collections;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for JCR nodes that exposes all properties and child nodes as
 * properties of a Javascript object.
 */
public class ScriptableNode extends ScriptableObject implements Wrapper {

    public static final String CLASSNAME = "Node";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Node node;

    public ScriptableNode() {
    }

    public ScriptableNode(Node item) {
        super();
        this.node = item;
    }

    public void jsConstructor(Object res) {
        this.node = (Node) res;
    }

    public String getClassName() {
        return CLASSNAME;
    }

    public ScriptableItemMap jsGet_children() {
        try {
            return new ScriptableItemMap(node.getNodes());
        } catch (RepositoryException re) {
            log.warn("Cannot get children of " + jsGet_path(), re);
            return new ScriptableItemMap();
        }
    }

    public ScriptableItemMap jsGet_properties() {
        try {
            return new ScriptableItemMap(node.getProperties());
        } catch (RepositoryException re) {
            log.warn("Cannot get children of " + jsGet_path(), re);
            return new ScriptableItemMap();
        }
    }

    public Object jsGet_primaryItem() {
        try {
            return ScriptRuntime.toObject(this, node.getPrimaryItem());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public String jsGet_UUID() {
        try {
            return node.getUUID();
        } catch (RepositoryException re) {
            return "";
        }
    }

    public int jsGet_index() {
        try {
            return node.getIndex();
        } catch (RepositoryException re) {
            return 1;
        }
    }

    public Iterator<?> jsGet_references() {
        try {
            return node.getReferences();
        } catch (RepositoryException re) {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    public Object jsGet_primaryNodeType() {
        try {
            return node.getPrimaryNodeType();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public NodeType[] jsGet_mixinNodeTypes() {
        try {
            return node.getMixinNodeTypes();
        } catch (RepositoryException re) {
            return new NodeType[0];
        }
    }

    public Object jsGet_definition() {
        try {
            return node.getDefinition();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public boolean jsGet_checkedOut() {
        try {
            return node.isCheckedOut();
        } catch (RepositoryException re) {
            return false;
        }
    }

    public Object jsGet_versionHistory() {
        try {
            return node.getVersionHistory();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_baseVersion() {
        try {
            return node.getBaseVersion();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_lock() {
        try {
            return node.getLock();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public boolean jsGet_locked() {
        try {
            return node.isLocked();
        } catch (RepositoryException re) {
            return false;
        }
    }

    public Object jsGet_session() {
        try {
            return node.getSession();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public String jsGet_path() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            return node.toString();
        }
    }

    public String jsGet_name() {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            return node.toString();
        }
    }

    public Object jsGet_parent() {
        try {
            return ScriptRuntime.toObject(this, node.getParent());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public int jsGet_depth() {
        try {
            return node.getDepth();
        } catch (RepositoryException re) {
            return -1;
        }
    }

    public boolean jsGet_new() {
        return node.isNew();
    }

    public boolean jsGet_modified() {
        return node.isModified();
    }

    @Override
    public Object getDefaultValue(Class typeHint) {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            return super.getDefaultValue(typeHint);
        }
    }

    // ---------- Wrapper interface --------------------------------------------

    // returns the wrapped node
    public Object unwrap() {
        return node;
    }
}

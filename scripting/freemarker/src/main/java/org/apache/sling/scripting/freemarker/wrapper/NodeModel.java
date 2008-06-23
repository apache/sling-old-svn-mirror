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
package org.apache.sling.scripting.freemarker.wrapper;

import freemarker.template.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;

/**
 * A wrapper for JCR nodes to support freemarker scripting.
 */
public class NodeModel implements TemplateScalarModel, TemplateNodeModel, TemplateHashModel, TemplateSequenceModel, AdapterTemplateModel {

    private Node node;
    private NodeListModel nodeList;

    public NodeModel(Node node) throws RepositoryException {
        this.node = node;
        this.nodeList = new NodeListModel(node.getNodes());
    }

    /**
     * @return the parent of this node or null, in which case
     *         this node is the root of the tree.
     */
    public TemplateNodeModel getParentNode() throws TemplateModelException {
        try {
            return new NodeModel(node.getParent());
        } catch (ItemNotFoundException infe) {
            return null;
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * @return a sequence containing this node's children.
     *         If the returned value is null or empty, this is essentially
     *         a leaf node.
     */
    public TemplateSequenceModel getChildNodes() throws TemplateModelException {
        return this.nodeList;
    }

    /**
     * @return a String that is used to determine the processing
     *         routine to use. In the XML implementation, if the node
     *         is an element, it returns the element's tag name.  If it
     *         is an attribute, it returns the attribute's name. It
     *         returns "@text" for text nodes, "@pi" for processing instructions,
     *         and so on.
     */
    public String getNodeName() throws TemplateModelException {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * @return a String describing the <em>type</em> of node this is.
     *         In the W3C DOM, this should be "element", "text", "attribute", etc.
     *         A TemplateNodeModel implementation that models other kinds of
     *         trees could return whatever it appropriate for that application. It
     *         can be null, if you don't want to use node-types.
     */
    public String getNodeType() throws TemplateModelException {
        try {
            return node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * @return the XML namespace URI with which this node is
     *         associated. If this TemplateNodeModel implementation is
     *         not XML-related, it will almost certainly be null. Even
     *         for XML nodes, this will often be null.
     */
    public String getNodeNamespace() throws TemplateModelException {
        return null;
    }

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     *            is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     *         or null if not found.
     */
    public TemplateModel get(String key) throws TemplateModelException {
        if (key == null) return null;
        if (key.startsWith("@")) {
            try {
                if (node.hasProperty(key.substring(1))) {
                    if (node.getProperty(key.substring(1)).getDefinition().isMultiple()) {
                        return new PropertyListModel(node.getProperty(key.substring(1)));
                    }
                    else {
                        return new PropertyModel(node.getProperty(key.substring(1)));
                    }
                }
                else return null;
            } catch (RepositoryException e) {
                throw new TemplateModelException(e);
            }
        }
        else {
            try {
                if (node.hasNode(key)) {
                    return new NodeModel(node.getNode(key));
                }
                else if (node.hasProperty(key) && node.getProperty(key).getType() == PropertyType.REFERENCE) {
                    return new NodeModel(node.getProperty(key).getNode());
                }
                return null;
            } catch (RepositoryException e) {
                throw new TemplateModelException(e);
            }
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        try {
            return !node.hasNodes() && ! node.hasProperties();
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * Retrieves the i-th template model in this sequence.
     *
     * @return the item at the specified index, or <code>null</code> if
     *         the index is out of bounds. Note that a <code>null</code> value is
     *         interpreted by FreeMarker as "variable does not exist", and accessing
     *         a missing variables is usually considered as an error in the FreeMarker
     *         Template Language, so the usage of a bad index will not remain hidden.
     */
    public TemplateModel get(int index) throws TemplateModelException {
        return nodeList.get(index);
    }

    /**
     * @return the number of items in the list.
     */
    public int size() throws TemplateModelException {
        return nodeList.size();
    }

    /**
     * Retrieves the underlying object, or some other object semantically
     * equivalent to its value narrowed by the class hint.
     *
     * @param hint the desired class of the returned value. An implementation
     *             should make reasonable effort to retrieve an object of the requested
     *             class, but if that is impossible, it must at least return the underlying
     *             object as-is. As a minimal requirement, an implementation must always
     *             return the exact underlying object when
     *             <tt>hint.isInstance(underlyingObject) == true</tt> holds. When called
     *             with <tt>java.lang.Object.class</tt>, it should return a generic Java
     *             object (i.e. if the model is wrapping a scripting lanugage object that is
     *             further wrapping a Java object, the deepest underlying Java object should
     *             be returned).
     * @return the underlying object, or its value accommodated for the hint
     *         class.
     */
    public Object getAdaptedObject(Class hint) {
        return node;
    }

    /**
     * Returns the string representation of this model. In general, avoid
     * returning null. In compatibility mode the engine will convert
     * null into empty string, however in normal mode it will
     * throw an exception if you return null from this method.
     */
    public String getAsString() throws TemplateModelException {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }
    
}

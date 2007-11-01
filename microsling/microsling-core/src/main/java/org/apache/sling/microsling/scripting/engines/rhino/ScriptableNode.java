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
package org.apache.sling.microsling.scripting.engines.rhino;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
/**
 * A wrapper for JCR nodes that exposes all properties and child nodes
 * as properties of a Javascript object.
 */
public class ScriptableNode extends ScriptableObject {
	private Node node;
	
	public ScriptableNode(Node item) {
		super();
		this.node = item;
	}
	
	public String getClassName() {
		return "Node";
	}
	/**
	 * Gets the value of a (Javascript) property. If there is a single single-value
	 * JCR property of this node, return its string value. If there are multiple properties
	 * of the same name or child nodes of the same name, return an array.
	 */
	@Override
	public Object get(String name, Scriptable start) {
		Set items = new HashSet();
		try {
			Iterator it = node.getNodes(name);
			while (it.hasNext()) {
				items.add(new ScriptableNode((Node) it.next()));
			}
		} catch (RepositoryException e) {}
		
		try {
			Iterator it = node.getProperties(name);
			while (it.hasNext()) {
				Property prop = (Property) it.next();
				int type = prop.getType();
				if (prop.getDefinition().isMultiple()) {
					Value[] values = prop.getValues();
					for (int i=0;i<values.length;i++) {
						items.add(wrap(values[i], type));
					}
				} else {
					if (type==PropertyType.REFERENCE) {
						items.add(new ScriptableNode(prop.getNode()));
					} else {
						items.add(wrap(prop.getValue(), type));
					}
				}
			}
		} catch (RepositoryException e) {}
		
		if (items.size()==0) {
			return null;
		} else if (items.size()==1) {
			return items.iterator().next();
		} else {
			//TODO: add write support
			NativeArray result = new NativeArray(items.toArray());
	        ScriptRuntime.setObjectProtoAndParent(result, this);
	        return result;
		}
	}
	private Object wrap(Value value, int type) throws ValueFormatException, IllegalStateException, RepositoryException {
		if (type==PropertyType.BINARY) {
			return Context.toBoolean(value.getBoolean());
		} else if (type==PropertyType.DOUBLE) {
			return Context.toNumber(value.getDouble());
		} else if (type==PropertyType.LONG) {
			return Context.toNumber(value.getLong());
		}
		return value.getString();
	}

	@Override
	public Object[] getIds() {
		Collection<String> ids = new HashSet<String>();
		try {
			PropertyIterator pit = node.getProperties();
			while (pit.hasNext()) {
				ids.add(pit.nextProperty().getName());
			}
		} catch (RepositoryException e) {
			//do nothing, just do not list properties
		}
		try {
			NodeIterator nit = node.getNodes();
			while (nit.hasNext()) {
				ids.add(nit.nextNode().getName());
			}
		} catch (RepositoryException e) {
			//do nothing, just do not list child nodes
		}
		return ids.toArray();
	}

	@Override
	public boolean has(String name, Scriptable start) {
		try {
			return node.hasProperty(name) || node.hasNode(name);
		} catch (RepositoryException e) {
			return false;
		}
	}

	@Override
	public Object getDefaultValue(Class typeHint) {
		try {
			return node.getPath();
		} catch (RepositoryException e) {
			return null;
		}
	}

}

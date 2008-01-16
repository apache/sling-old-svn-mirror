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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
/**
 * A wrapper for JCR nodes that exposes all properties and child nodes
 * as properties of a Javascript object.
 */
public class ScriptableNode extends ScriptableObject implements Wrapper {

    public static final String CLASSNAME = "Node";

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

	/**
	 * Gets the value of a (Javascript) property. If there is a single single-value
	 * JCR property of this node, return its string value. If there are multiple properties
	 * of the same name or child nodes of the same name, return an array.
	 */
	@Override
	public Object get(String name, Scriptable start) {
		List<Scriptable> items = new ArrayList<Scriptable>();

		// add all matching nodes
		try {
			NodeIterator it = node.getNodes(name);
			while (it.hasNext()) {
				items.add(new ScriptableNode(it.nextNode()));
			}
		} catch (RepositoryException e) {}

		// add all matching properies
		try {
			PropertyIterator it = node.getProperties(name);
			while (it.hasNext()) {
				Property prop = it.nextProperty();
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
			return Undefined.instance;
		} else if (items.size()==1) {
			return items.iterator().next();
		} else {
			//TODO: add write support
			NativeArray result = new NativeArray(items.toArray());
	        ScriptRuntime.setObjectProtoAndParent(result, this);
	        return result;
		}
	}

	private Scriptable wrap(Value value, int type) throws ValueFormatException, IllegalStateException, RepositoryException {
	    Object valObj;
		if (type==PropertyType.BINARY) {
			valObj = value.getBoolean();
		} else if (type==PropertyType.DOUBLE) {
		    valObj = value.getDouble();
		} else if (type==PropertyType.LONG) {
		    valObj = value.getLong();
		} else {
		    valObj = value.getString();
		}

		return ScriptRuntime.toObject(this, valObj);
	}

	@Override
	public Object[] getIds() {
		Collection<String> ids = new ArrayList<String>();
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

	//---------- Wrapper interface --------------------------------------------
	
	// returns the wrapped node
	public Object unwrap() {
	    return node;
	}
}

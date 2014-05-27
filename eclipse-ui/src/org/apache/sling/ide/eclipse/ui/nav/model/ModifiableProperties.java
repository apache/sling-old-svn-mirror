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
package org.apache.sling.ide.eclipse.ui.nav.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Element;

public class ModifiableProperties implements IPropertySource {
	
	private Map<String, String> properties = new HashMap<String, String>();
	private JcrNode node;
	private Element domElement;
	private GenericJcrRootFile genericJcrRootFile;
	
	public ModifiableProperties(JcrNode node) {
		this.node = node;
	}
	
	public void setJcrNode(JcrNode node) {
		if (node==null) {
			throw new IllegalArgumentException("node must not be null");
		}
		this.node = node;
	}
	
	public GenericJcrRootFile getUnderlying() {
		return genericJcrRootFile;
	}
	
	public Element getDomElement() {
	    return domElement;
	}
	
	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		final List<IPropertyDescriptor> result = new LinkedList<IPropertyDescriptor>();
		for (Iterator<Map.Entry<String, String>> it = properties.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			TextPropertyDescriptor pd = new TextPropertyDescriptor(entry, entry.getKey());
			result.add(pd);
		}
		final String jcrPrimaryType = "jcr:primaryType";
        if (!properties.containsKey(jcrPrimaryType)) {
		    Map<String, String> pseudoMap = new HashMap<String, String>();
		    pseudoMap.put(jcrPrimaryType, node.getPrimaryType());
		    result.add(new TextPropertyDescriptor(pseudoMap.entrySet().iterator().next(), jcrPrimaryType));
		}
		return result.toArray(new IPropertyDescriptor[] {});
	}

	public String getValue(String key) {
		return properties.get(key);
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		Map.Entry<String, String> entry = (Map.Entry<String, String>)id;
		return entry.getValue();
	}

	@Override
	public boolean isPropertySet(Object id) {
		return properties.containsKey(String.valueOf(id));
	}

	@Override
	public void resetPropertyValue(Object id) {

	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	    if (id instanceof Map.Entry<?, ?>) {
	        Map.Entry<String, String> entry = (Map.Entry<String, String>)id;
	        entry.setValue(String.valueOf(value));
	        Attribute a = domElement.getAttribute(entry.getKey());
	        a.setValue(String.valueOf(value));
	        genericJcrRootFile.save();
	    } else if (value instanceof String) {
            Attribute a = domElement.getAttribute((String) id);
            a.setValue((String) value);
            genericJcrRootFile.save();
	    } else {
	        System.out.println("UNSUPPORTED VALUE TYPE: "+value.getClass());
	    }
	}

	public void setNode(GenericJcrRootFile genericJcrRootFile, Element domNode) {
		this.domElement = domNode;
		final List<Attribute> attributes = domNode.getAttributes();
		if (attributes!=null) {
			for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
				final Attribute a = it.next();
				final String name = a.getName();
				if (name.startsWith("xmlns:")) {
				    continue;
				}
                properties.put(name, a.getValue());
			}
		}
		this.genericJcrRootFile = genericJcrRootFile;
	}

    public void deleteProperty(String displayName) {
        domElement.removeAttribute(displayName);
        genericJcrRootFile.save();
    }

    public void addProperty(String name, String value) {
        domElement.addAttribute(name, value);
        genericJcrRootFile.save();
    }

    public void renameProperty(String oldKey, String newKey) {
        Attribute a = domElement.getAttribute(oldKey);
        a.setName(newKey);
        genericJcrRootFile.save();
    }

    public void changePropertyType(String key, int propertyType) {
        Attribute a = domElement.getAttribute(key);
        String value = a.getValue();
        if (value.startsWith("{") && value.contains("}")) {
            int index = value.indexOf("}");
            value = value.substring(index+1);
        }
        if (propertyType!=PropertyType.STRING) {
            value = "{" + PropertyType.nameFromValue(propertyType)+"}"+value;
        }
        a.setValue(value);
        genericJcrRootFile.save();
    }

}

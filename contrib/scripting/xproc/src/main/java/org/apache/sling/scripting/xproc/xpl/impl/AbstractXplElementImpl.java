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
package org.apache.sling.scripting.xproc.xpl.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.xproc.xpl.api.XplElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXplElementImpl implements XplElement {
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private SlingScriptHelper sling;
	private final Map<String, String> attributes = new HashMap<String, String>();	
	private XplElement parent; 
	private final LinkedList<XplElement> children = new LinkedList<XplElement>();
	private int depth = 0;

	public LinkedList<XplElement> getChildren() {
		return children;
	}

	public void addChild(XplElement child) {
		
		if (child == null) {
            String msg = "Element of class " + this.getClass().getName() + " received null child.";
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }
		
		Field childField = this.getChildField(child);
        if (childField != null) {
            childField.setAccessible(true);
            try {
                childField.set(this, child);
            } catch (IllegalArgumentException e) {
                this.log.error("Failed to set child field for child class '" + child.getClass().getName(), e);
            } catch (IllegalAccessException e) {
                this.log.error("Failed to set child field for child class '" + child.getClass().getName(), e);
            }
        }
        child.setDepth(this.getDepth() + 1);
        this.children.add(child);
		child.setParent(this);
		
	}
	
	public void setAttributes(Map<String, String> attributes) {
		if (attributes == null || attributes.isEmpty()) {
            // nothing to do
            return;
        }

        // check for special attribute fields
        Map<String, Field> attributeFields = this.getAttributeFields();
        for (Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Field attributeField = attributeFields.get(key);
            if (attributeField != null) {
            	attributeField.setAccessible(true);
                try {
                	attributeField.set(this, value);
                } catch (IllegalArgumentException e) {
                    String message = "Failed to set attribute field " + key;
                    this.log.error(message, e);
                    throw new RuntimeException(message, e);
                } catch (IllegalAccessException e) {
                    String message = "Failed to set attribute field " + key;
                    this.log.error(message, e);
                    throw new RuntimeException(message, e);
                }
             }

            // default attribute processing
            this.processAttribute(key, value);
        }
	}
	
	public SlingScriptHelper getSling() {
		return sling;
	}
	
	public abstract QName getQName();
	
	public XplElement getParent() {
		return parent;
	}
	
	public void setParent(XplElement parent) {
		this.parent = parent;
	}
	
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	@Override
	public String toString() {
		StringBuffer sbXplElement = new StringBuffer();
		addTabs(sbXplElement, getDepth());
		sbXplElement.append("<");
		sbXplElement.append("p:" + this.getQName().getLocalPart());
		if (getDepth() == 0) {
			sbXplElement.append(" xmlns:p=\"http://www.w3.org/ns/xproc\"");
		}
		for (String attribute : this.attributes.keySet()) {
			sbXplElement.append(" ");
			sbXplElement.append(attribute + "=" + "\"" + this.attributes.get(attribute) + "\"");
		}
		if (getChildren().size() == 0) {
			sbXplElement.append(" />");
			return sbXplElement.toString();
		}
		sbXplElement.append(">");
		for (XplElement child : this.getChildren()) {
			sbXplElement.append("\r\n");
			sbXplElement.append(child.toString());
		}
		sbXplElement.append("\r\n");
		addTabs(sbXplElement, getDepth());
		sbXplElement.append("</");
		sbXplElement.append("p:" + this.getQName().getLocalPart());
		sbXplElement.append(">");
		
		return sbXplElement.toString();
	}
	
	private void addTabs(StringBuffer sb, int num) {
		for (int i = 0; i < num; i++) 
			sb.append("\t");
	}
	
	protected void processAttribute(String key, String value) {
        this.attributes.put(key, value);
    }
	
	private Field getChildField(XplElement child) {
        Class<?> currentClass = this.getClass();

        Field[] declaredFields = currentClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getType().isAssignableFrom(child.getClass())) {
                return declaredField;
            }
        }

        return null;
    }
	
    private Map<String, Field> getAttributeFields() {
        Map<String, Field> attributeFields = new HashMap<String, Field>();

        Class<?> currentClass = this.getClass();
        while (currentClass != null) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {              
                String fieldName = this.convertCamelCase(declaredField.getName());
                attributeFields.put(fieldName, declaredField);
            }

            currentClass = currentClass.getSuperclass();
        }

        return attributeFields;
    }

    private String convertCamelCase(String name) {
        Pattern camelCasePattern = Pattern.compile("(.)([A-Z])");
        Matcher matcher = camelCasePattern.matcher(name);

        int lastMatch = 0;
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            result.append(name.substring(lastMatch, matcher.start()));
            result.append(matcher.group(1));
            result.append("-");
            result.append(matcher.group(2).toLowerCase());
            lastMatch = matcher.end();
        }

        result.append(name.substring(lastMatch, name.length()));

        return result.toString();
    }

}

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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;

import org.apache.sling.ide.eclipse.ui.views.DateTimeSupport;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.Text;

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
        reformat();
        genericJcrRootFile.save();
    }

    public void addProperty(String name, String value) {
        domElement.addAttribute(name, value);
        reformat();
        genericJcrRootFile.save();
    }

    private void reformat() {
        List<Attribute> list = domElement.getAttributes();
        if (list.size()==0) {
            // then there are no attributes at all - nothing to format
            return;
        } else if (list.size()==1) {
            // only one attribute - make sure it has no preSpace
            Attribute first = list.get(0);
            first.setPreSpace("");
        } else {
            final String NL = System.getProperty("line.separator");
            final String INDENT = "    ";
            // otherwise, make sure each element has the correct preSpace
            final String correctPreSpace;
            Element parent = domElement.getParentElement();
            if (parent!=null) {
                List<Node> nodes = parent.getNodes();
                if (nodes.size()>1 && (nodes.get(0) instanceof Text) && (nodes.get(0).toXML().startsWith(NL))) {
                    correctPreSpace = nodes.get(0).toXML() + INDENT;
                } else {
                    String totalIndent = INDENT;
                    while(parent!=null) {
                        totalIndent = totalIndent + INDENT;
                        parent = parent.getParentElement();
                    }
                    correctPreSpace = NL + totalIndent;
                }
            } else {
                // guestimate
                correctPreSpace = NL + INDENT;
            }
            for (Iterator it = list.iterator(); it.hasNext();) {
                Attribute attribute = (Attribute) it.next();
                if (!attribute.getName().startsWith("xmlns:")) {
                    attribute.setPreSpace(correctPreSpace);
                }
            }
        }
    }

    public void renameProperty(String oldKey, String newKey) {
        Attribute a = domElement.getAttribute(oldKey);
        a.setName(newKey);
        genericJcrRootFile.save();
    }

    public void changePropertyType(String key, int propertyType) {
        Attribute a = domElement.getAttribute(key);
        String value = a.getValue();
        
        // when changing the property type, the value needs to be adjusted
        // to make up a valid property
        // a simple approach is to create default values if a conversion
        // of the existing value is not possible/feasible.
        switch(propertyType) {
        case PropertyType.BINARY: {
            value = "";
            break;
        }
        case PropertyType.BOOLEAN: {
            try{
                value = String.valueOf(Boolean.parseBoolean(value));
            } catch(Exception e) {
                // hardcode to false then
                value = "false";
            }
            break;
        }
        case PropertyType.DATE: {
            try{
                value = DateTimeSupport.print(DateTimeSupport.parseAsCalendar(value));
            } catch(Exception e) {
                value = DateTimeSupport.print(Calendar.getInstance());
            }
            break;
        }
        case PropertyType.DECIMAL: {
            try{
                Float f = Float.parseFloat(value);
                value = String.valueOf(f);
            } catch(Exception e) {
                value = "0.0";
            }
            break;
        }
        case PropertyType.LONG: {
            try{
                value = String.valueOf(Long.parseLong(value));
            } catch(Exception e) {
                value = "0";
            }
            break;
        }
        case PropertyType.NAME:
        case PropertyType.STRING: {
            // no conversion needed, already converted
            break;
        }
        case PropertyType.PATH: 
        case PropertyType.URI:
        case PropertyType.REFERENCE:
        case PropertyType.WEAKREFERENCE: {
            //TODO validation would be necessary but not implemented atm
            // no conversion needed, already converted
            break;
        }
        }
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

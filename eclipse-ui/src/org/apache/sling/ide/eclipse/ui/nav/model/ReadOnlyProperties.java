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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class ReadOnlyProperties implements IPropertySource {
	
	private Map<String, String> properties = new HashMap<String, String>();
	
	public ReadOnlyProperties() {
		
	}
	
	public ReadOnlyProperties(Map<String, String> properties) {
		this.properties.putAll(properties);
	}
	
	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		final List<IPropertyDescriptor> result = new LinkedList<IPropertyDescriptor>();
		for (Iterator<Map.Entry<String, String>> it = properties.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			IPropertyDescriptor pd = new PropertyDescriptor(entry, entry.getKey());
			result.add(pd);
		}
		return result.toArray(new IPropertyDescriptor[] {});
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

	}

	public void add(String label, String value) {
		properties.put(label, value);
	}

}

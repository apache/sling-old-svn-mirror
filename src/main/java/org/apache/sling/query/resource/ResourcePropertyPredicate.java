/*-
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

package org.apache.sling.query.resource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.predicate.SelectorOperator;
import org.apache.sling.query.selector.parser.Attribute;

public class ResourcePropertyPredicate implements Predicate<Resource> {
	private final String key;

	private final String value;

	private final SelectorOperator operator;

	public ResourcePropertyPredicate(Attribute attribute) {
		this.key = attribute.getKey();
		this.value = attribute.getValue();
		this.operator = SelectorOperator.getSelectorOperator(attribute.getOperator());
	}

	@Override
	public boolean accepts(Resource resource) {
		Resource property = resource.getChild(key);
		if (property == null) {
			return false;
		} else if (value == null) {
			return true;
		} else {
			return isEqualToValue(property);
		}
	}

	private boolean isEqualToValue(Resource property) {
		final String[] multiProperty = property.adaptTo(String[].class);
		if (multiProperty != null) {
			for (String p : multiProperty) {
				if (operator.accepts(p, value)) {
					return true;
				}
			}
			return false;
		} else {
			return operator.accepts(property.adaptTo(String.class), value);
		}
	}
}

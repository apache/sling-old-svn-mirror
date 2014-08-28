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

package org.apache.sling.query.selector.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class SelectorSegment {
	private final String type;

	private final String name;

	private final List<Attribute> attributes;

	private final List<Modifier> modifiers;

	private final char hierarchyOperator;

	public SelectorSegment(ParserContext context, boolean firstSegment) {
		this.type = context.getType();
		this.name = context.getName();
		this.attributes = new ArrayList<Attribute>(context.getAttributes());
		this.modifiers = new ArrayList<Modifier>(context.getModifiers());
		if (firstSegment) {
			hierarchyOperator = 0;
		} else {
			hierarchyOperator = context.getHierarchyOperator();
		}
	}

	SelectorSegment(String type, String name, List<Attribute> attributes,
			List<Modifier> modifiers, char hierarchyOperator) {
		this.type = type;
		this.name = name;
		this.attributes = attributes;
		this.modifiers = modifiers;
		this.hierarchyOperator = hierarchyOperator;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public char getHierarchyOperator() {
		return hierarchyOperator;
	}

	public List<Modifier> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		SelectorSegment rhs = (SelectorSegment) obj;
		return new EqualsBuilder().append(type, rhs.type).append(attributes, rhs.attributes)
				.append(modifiers, rhs.modifiers).append(hierarchyOperator, rhs.hierarchyOperator).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(type).append(attributes).append(modifiers)
				.append(hierarchyOperator).toHashCode();
	}

	@Override
	public String toString() {
		return String.format("SelectorSegment[%s,%s,%s,%s]", type, attributes, modifiers,
				hierarchyOperator);
	}
}

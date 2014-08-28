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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Modifier {

	private final String name;

	private final String argument;

	public Modifier(String name, String argument) {
		this.name = name;
		this.argument = argument;
	}

	public String getName() {
		return name;
	}

	public String getArgument() {
		return argument;
	}

	@Override
	public String toString() {
		return String.format("Modifier[%s,%s]", name, argument);
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
		Modifier rhs = (Modifier) obj;
		return new EqualsBuilder().append(name, rhs.name).append(argument, rhs.argument)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(name).append(argument).toHashCode();
	}
}
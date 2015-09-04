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

package org.apache.sling.query.api.internal;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Option<T> {
	private final T element;

	private final int argumentId;

	private Option(T element, int argumentId) {
		this.element = element;
		this.argumentId = argumentId;
	}

	public static <T> Option<T> of(T element, int argumentId) {
		return new Option<T>(element, argumentId);
	}

	public static <T> Option<T> empty(int argumentId) {
		return new Option<T>(null, argumentId);
	}

	public int getArgumentId() {
		return argumentId;
	}

	public T getElement() {
		return element;
	}

	public boolean isEmpty() {
		return element == null;
	}

	public String toString() {
		if (isEmpty()) {
			return "Option[-]";
		} else {
			return String.format("Option[%s]", element.toString());
		}
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
		Option<?> rhs = (Option<?>) obj;
		return new EqualsBuilder().append(element, rhs.element).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(element).toHashCode();
	}
}

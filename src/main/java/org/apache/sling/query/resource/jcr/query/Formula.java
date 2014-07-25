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

package org.apache.sling.query.resource.jcr.query;

import java.util.Iterator;
import java.util.List;

public class Formula implements Term {
	public enum Operator {
		AND, OR
	}

	private final Operator operator;

	private final List<Term> conditions;

	public Formula(Operator operator, List<Term> conditions) {
		this.operator = operator;
		this.conditions = conditions;
	}

	public String buildString() {
		if (conditions.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		Iterator<Term> iterator = conditions.iterator();
		if (conditions.size() > 1) {
			builder.append("(");
		}
		while (iterator.hasNext()) {
			Term term = iterator.next();
			builder.append(term.buildString());
			if (iterator.hasNext()) {
				builder.append(' ').append(operator.toString()).append(' ');
			}
		}
		if (conditions.size() > 1) {
			builder.append(")");
		}
		return builder.toString();
	}
}

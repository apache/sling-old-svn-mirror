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

package org.apache.sling.query.predicate;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public enum SelectorOperator {
	CONTAINS("*=") {
		@Override
		public boolean accepts(String property, String value) {
			return StringUtils.contains(property, value);
		}
	},
	CONTAINS_WORD("~=") {
		@Override
		public boolean accepts(String property, String value) {
			String quoted = Pattern.quote(value);
			String regex = String.format("(^| )%s( |$)", quoted);
			return property != null && Pattern.compile(regex).matcher(property).find();
		}
	},
	ENDS_WITH("$=") {
		@Override
		public boolean accepts(String property, String value) {
			return StringUtils.endsWith(property, value);
		}
	},
	EQUALS("=") {
		@Override
		public boolean accepts(String property, String value) {
			return StringUtils.equals(property, value);
		}
	},
	NOT_EQUAL("!=") {
		@Override
		public boolean accepts(String property, String value) {
			return !StringUtils.equals(property, value);
		}
	},
	STARTS_WITH("^=") {
		@Override
		public boolean accepts(String property, String value) {
			return StringUtils.startsWith(property, value);
		}
	};

	private final String operator;

	private SelectorOperator(String operator) {
		this.operator = operator;
	}

	public abstract boolean accepts(String key, String value);

	public static SelectorOperator getSelectorOperator(String operator) {
		for (SelectorOperator o : values()) {
			if (o.operator.equals(operator)) {
				return o;
			}
		}
		return EQUALS;
	}
}

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

package org.apache.sling.query.resource.jcr;

public enum JcrOperator {
	CONTAINS("*=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return String.format("s.[%s] LIKE '%%%s%%'", key, value);
		}
	},
	CONTAINS_WORD("~=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return CONTAINS.getJcrQueryFragment(key, value);
		}
	},
	ENDS_WITH("$=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return String.format("s.[%s] LIKE '%%%s'", key, value);
		}
	},
	EQUALS("=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return String.format("s.[%s] = '%s'", key, value);
		}
	},
	NOT_EQUAL("!=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return String.format("s.[%s] != '%s'", key, value);
		}
	},
	STARTS_WITH("^=") {
		@Override
		public String getJcrQueryFragment(String key, String value) {
			return String.format("s.[%s] LIKE '%s%%'", key, value);
		}
	};

	private final String operator;

	private JcrOperator(String operator) {
		this.operator = operator;
	}

	public abstract String getJcrQueryFragment(String key, String value);

	public static JcrOperator getSelectorOperator(String operator) {
		for (JcrOperator o : values()) {
			if (o.operator.equals(operator)) {
				return o;
			}
		}
		return EQUALS;
	}
}

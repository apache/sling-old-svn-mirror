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

import org.apache.commons.lang.ArrayUtils;

public enum State {
	START {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '/') {
				context.setState(State.TYPE_WITH_SLASHES);
				context.append(c);
			} else if (c == '[') {
				context.setState(State.ATTRIBUTE_KEY);
			} else if (c == ':') {
				context.setState(State.MODIFIER);
			} else if (c == '>' || c == '+' || c == '~') {
				context.setHierarchyOperator(c);
			} else if (c == '#') {
				context.setType();
				context.setState(NAME);
			} else if (c != ' ') {
				context.setState(State.TYPE);
				context.append(c);
			}
		}
	},
	IDLE {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '[') {
				context.setState(State.ATTRIBUTE_KEY);
			} else if (c == ':') {
				context.setState(State.MODIFIER);
			} else if (c == ' ') {
				context.finishSelectorSegment();
				context.setState(START);
			} else if (c == ',' || c == 0) {
				context.finishSelectorSegment();
				context.finishSelector();
				context.setState(START);
			}
		}
	},
	TYPE {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '/') {
				context.setState(State.TYPE_WITH_SLASHES);
				context.append(c);
			} else if (c == '[') {
				context.setState(State.ATTRIBUTE_KEY);
				context.setType();
			} else if (c == ':') {
				context.setState(State.TYPE_WITH_SLASHES);
				context.append(c);
			} else if (c == '#') {
				context.setType();
				context.setState(NAME);
			} else if (c == ' ') {
				context.setType();
				context.finishSelectorSegment();
				context.setState(START);
			} else if (c == ',' || c == 0) {
				context.setType();
				context.finishSelectorSegment();
				context.finishSelector();
				context.setState(START);
			} else {
				context.append(c);
			}
		}
	},
	TYPE_WITH_SLASHES {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '[') {
				context.setState(State.ATTRIBUTE_KEY);
				context.setType();
			} else if (c == ':') {
				context.setState(State.MODIFIER);
				context.setType();
			} else if (c == '#') {
				context.setType();
				context.setState(NAME);
			} else if (c == ' ') {
				context.setType();
				context.finishSelectorSegment();
				context.setState(START);
			} else if (c == ',' || c == 0) {
				context.setType();
				context.finishSelectorSegment();
				context.finishSelector();
				context.setState(START);
			} else {
				context.append(c);
			}
		}
	},
	NAME {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '[') {
				context.setName();
				context.setState(State.ATTRIBUTE_KEY);
			} else if (c == ':') {
				context.setName();
				context.setState(State.MODIFIER);
			} else if (c == ' ') {
				context.setName();
				context.finishSelectorSegment();
				context.setState(START);
			} else if (c == ',' || c == 0) {
				context.setName();
				context.finishSelectorSegment();
				context.finishSelector();
				context.setState(START);
			} else if (c == '\'') {
				context.setState(State.ESCAPED_NAME);
			} else {
				context.append(c);
			}
		}
	},
	ESCAPED_NAME {
		@Override
		public void process(ParserContext context, char c) {
			if (c == '\'') {
				context.setName();
				context.setState(IDLE);
			} else {
				context.append(c);
			}
		}
	},
	ATTRIBUTE_KEY {
		@Override
		public void process(ParserContext context, char c) {
			if (c == ']') {
				context.setAttributeKey();
				context.addAttribute();
				context.setState(State.IDLE);
			} else if (ArrayUtils.contains(OPERATORS, c)) {
				context.setAttributeKey();
				context.setState(State.ATTRIBUTE_OPERATOR);
				context.append(c);
			} else {
				context.append(c);
			}
		}
	},
	ATTRIBUTE_OPERATOR {
		@Override
		public void process(ParserContext context, char c) {
			if (!ArrayUtils.contains(OPERATORS, c)) {
				context.setAttributeOperator();
				context.append(c);
				context.setState(ATTRIBUTE_VALUE);
			} else {
				context.append(c);
			}
		}
	},
	ATTRIBUTE_VALUE {
		@Override
		public void process(ParserContext context, char c) {
			if (c == ']') {
				context.setState(State.IDLE);
				context.setAttributeValue();
				context.addAttribute();
			} else {
				context.append(c);
			}
		}
	},
	MODIFIER {
		@Override
		public void process(ParserContext context, char c) {
			if (c == ':') {
				context.addModifier();
			} else if (c == '(') {
				context.setModifierName();
				context.setState(State.MODIFIER_ARGUMENT);
				context.increaseParentheses();
			} else if (c == ' ') {
				context.addModifier();
				context.finishSelectorSegment();
				context.setState(START);
			} else if (c == ',' || c == 0) {
				context.addModifier();
				context.finishSelectorSegment();
				context.finishSelector();
				context.setState(START);
			} else {
				context.append(c);
			}
		}
	},
	MODIFIER_ARGUMENT {
		@Override
		public void process(ParserContext context, char c) {
			if (c == ')') {
				if (context.decreaseParentheses() == 0) {
					context.addModifier();
					context.setState(IDLE);
				} else {
					context.append(c);
				}
			} else if (c == '(') {
				context.increaseParentheses();
				context.append(c);
			} else {
				context.append(c);
			}
		}
	};
	public abstract void process(ParserContext context, char c);

	private static final char[] OPERATORS = "*~$!^=".toCharArray();
}

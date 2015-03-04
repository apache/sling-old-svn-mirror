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

package org.apache.sling.query.selector;

import org.apache.sling.query.api.Function;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.function.ChildrenFunction;
import org.apache.sling.query.function.FindFunction;
import org.apache.sling.query.function.IdentityFunction;
import org.apache.sling.query.function.NextFunction;
import org.apache.sling.query.predicate.RejectingPredicate;
import org.apache.sling.query.selector.parser.SelectorSegment;

public enum HierarchyOperator {
//@formatter:off
	CHILD('>') {
		@Override
		public <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy, TreeProvider<T> provider) {
			return new ChildrenFunction<T>(provider);
		}
	},
	DESCENDANT(' ') {
		@Override
		public <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy, TreeProvider<T> provider) {
			return new FindFunction<T>(strategy, provider, segment);
		}
	},
	NEXT_ADJACENT('+') {
		@Override
		public <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy, TreeProvider<T> provider) {
			return new NextFunction<T>(null, provider);
		}
	},
	NEXT_SIBLINGS('~') {
		@Override
		public <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy, TreeProvider<T> provider) {
			return new NextFunction<T>(new RejectingPredicate<T>(), provider);
		}
	},
	NOOP((char)0) {
		@Override
		public <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy, TreeProvider<T> provider) {
			return new IdentityFunction<T>();
		}
	};
//@formatter:on

	private final char c;

	private HierarchyOperator(char c) {
		this.c = c;
	}

	public abstract <T> Function<?, ?> getFunction(SelectorSegment segment, SearchStrategy strategy,
			TreeProvider<T> provider);

	public static HierarchyOperator findByCharacter(char c) {
		for (HierarchyOperator operator : values()) {
			if (operator.c == c) {
				return operator;
			}
		}
		return NOOP;
	}
}

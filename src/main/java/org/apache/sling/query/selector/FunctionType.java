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
import org.apache.sling.query.function.EvenFunction;
import org.apache.sling.query.function.FilterFunction;
import org.apache.sling.query.function.HasFunction;
import org.apache.sling.query.function.LastFunction;
import org.apache.sling.query.function.NotFunction;
import org.apache.sling.query.function.SliceFunction;
import org.apache.sling.query.predicate.ParentPredicate;
import org.apache.sling.query.predicate.RejectingPredicate;

public enum FunctionType {
	EQ {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> providerw) {
			int index = Integer.parseInt(argument);
			return new SliceFunction<T>(index, index);
		}
	},
	FIRST {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new SliceFunction<T>(0, 0);
		}
	},
	LAST {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new LastFunction<T>();
		}
	},
	GT {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new SliceFunction<T>(Integer.valueOf(argument) + 1);
		}
	},
	LT {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new SliceFunction<T>(0, Integer.valueOf(argument) - 1);
		}
	},
	HAS {
		@Override
		public <T> Function<?, ?> getFunction(String selector, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new HasFunction<T>(selector, strategy, provider);
		}
	},
	PARENT {
		@Override
		public <T> Function<?, ?> getFunction(String selector, SearchStrategy strategy,
				final TreeProvider<T> provider) {
			return new FilterFunction<T>(new ParentPredicate<T>(provider));
		}
	},
	EMPTY {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				final TreeProvider<T> provider) {
			return new FilterFunction<T>(new RejectingPredicate<T>(new ParentPredicate<T>(provider)));
		}
	},
	ODD {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new EvenFunction<T>(false);
		}
	},
	EVEN {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new EvenFunction<T>(true);
		}
	},
	NOT {
		@Override
		public <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
				TreeProvider<T> provider) {
			return new NotFunction<T>(new SelectorFunction<T>(argument, provider, strategy));
		}
	};

	public abstract <T> Function<?, ?> getFunction(String argument, SearchStrategy strategy,
			TreeProvider<T> provider);
}

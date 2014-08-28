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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public final class SelectorParser {

	private SelectorParser() {
	}

	public static List<Selector> parse(String selector) {
		if (StringUtils.isEmpty(selector)) {
			return Arrays.asList(new Selector());
		}
		ParserContext context = new ParserContext();
		for (char c : selector.toCharArray()) {
			context.getState().process(context, c);
		}
		context.getState().process(context, (char) 0);
		return context.getSelectors();
	}

	public static List<SelectorSegment> getFirstSegmentFromEachSelector(List<Selector> selectors) {
		List<SelectorSegment> segments = new ArrayList<SelectorSegment>();
		for (Selector selector : selectors) {
			if (!selector.getSegments().isEmpty()) {
				segments.add(selector.getSegments().get(0));
			}
		}
		return segments;
	}

}

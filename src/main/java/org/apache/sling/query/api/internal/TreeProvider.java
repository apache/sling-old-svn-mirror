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

import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.selector.parser.Attribute;
import org.apache.sling.query.selector.parser.SelectorSegment;

import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface TreeProvider<T> {
	Iterator<T> listChildren(T parent);

	T getParent(T element);

	String getName(T element);

	Predicate<T> getPredicate(String type, String name, List<Attribute> attributes);

	Iterator<T> query(List<SelectorSegment> segment, T resource);

	boolean sameElement(T o1, T o2);

	boolean isDescendant(T root, T testedElement);
}

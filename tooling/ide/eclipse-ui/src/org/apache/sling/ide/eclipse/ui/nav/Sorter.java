/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.nav;

import java.text.Collator;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class Sorter extends ViewerSorter {

	public Sorter() {
	}

	public Sorter(Collator collator) {
		super(collator);
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof JcrNode) || !(e2 instanceof JcrNode)) {
			return super.compare(viewer, e1, e2);
		}
		JcrNode node1 = (JcrNode) e1;
		JcrNode node2 = (JcrNode) e2;
		JcrNode parent = node1.getParent();
		Object[] children = parent.getChildren(false);
		for (Object child : children) {
			if (child==node1) {
				return -1;
			} else if (child==node2) {
				return 1;
			}
		}
		return 0;
	}

}

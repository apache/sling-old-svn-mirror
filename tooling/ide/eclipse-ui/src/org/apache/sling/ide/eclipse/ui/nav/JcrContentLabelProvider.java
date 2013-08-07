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

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

/** WIP: label provider for content package view in project explorer **/
public class JcrContentLabelProvider implements ILabelProvider, IDescriptionProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof JcrNode) {
			JcrNode jcrNode = (JcrNode)element;
			return jcrNode.getImage();
		} else {
			// fallback to default
			return null;
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof JcrNode) {
			JcrNode jcrNode = (JcrNode)element;
			return jcrNode.getLabel();
		} else {
			// fallback to the default
			return null;
		}
	}

	@Override
	public String getDescription(Object element) {
		if (element instanceof JcrNode) {
			JcrNode jcrNode = (JcrNode)element;
			return jcrNode.getDescription();
		} else {
			// fallback to the default
			return null;
		}
	}

}

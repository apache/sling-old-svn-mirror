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
package org.apache.sling.ide.eclipse.ui.nav.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.Node;

public class DirNode extends JcrNode {

	static boolean isDirNode(IResource resource) {
		if (resource==null) {
			return false;
		}
		final String resourceName = resource.getName();
		if (!resourceName.endsWith(".dir")) {
			return false;
		}
		if (!(resource instanceof IFolder)) {
			return false;
		}
		final IFolder folder = (IFolder)resource;
		final IContainer container = folder.getParent();
		if (container==null || !container.exists()) {
			return false;
		}
		final IResource peerNode = container.findMember(resourceName.substring(0, resourceName.length()-4));
		if (peerNode==null || !peerNode.exists()) {
			return false;
		}
		final IResource dotContextXml = folder.findMember(".content.xml");
		if (dotContextXml==null || !dotContextXml.exists()) {
			return false;
		}
		// then it is likely the pattern that corresponds to the case
		// which we want to handle with this DirNode
		return true;
	}
	
	DirNode(JcrNode parent, Node domNode, IResource resource) {
		super(parent, domNode, resource);
		if (!isDirNode(resource)) {
			throw new IllegalArgumentException("resource is not a DirNode: "+resource);
		}
	}
	
	@Override
	protected void addChild(JcrNode jcrNode) {
		final String shortName = getName().substring(0, getName().length()-4);
		Set<JcrNode> c = new HashSet<JcrNode>(parent.children);
		for (Iterator<JcrNode> it = c.iterator(); it.hasNext();) {
			JcrNode node = it.next();
			if (node.getName().equals(shortName)) {
				// excellent, the parent contains a child which 
				// matches the .dir pattern, so add this child there
				node.addChild(jcrNode);
				// but also hide this node from my parent
				parent.hide(this);
				return;
			}
		}
		super.addChild(jcrNode);
	}
	
	@Override
	public IFile getFileForEditor() {
		return null;
	}
	
	@Override
	public boolean canBeRenamed() {
		return false;
	}
	
}

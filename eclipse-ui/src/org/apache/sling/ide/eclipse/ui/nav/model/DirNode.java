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
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import de.pdark.decentxml.Element;

public class DirNode extends JcrNode {
	
    static String encode(String name) {
        int colon = name.indexOf(":");
        if (colon==-1) {
            return name;
        }
        return "_" + name.substring(0, colon) + "_" + name.substring(colon+1);
    }

    static String decode(String name) {
        if (name.endsWith(".dir")) {
            return name.substring(0, name.length()-4);
        } else if (!name.startsWith("_")) {
            return null;
        }
        name = name.substring(1);
        int pos = name.indexOf("_");
        if (pos==-1) {
            return null;
        }
        name = name.substring(0, pos) + ":" + name.substring(pos+1);
        return name;
    }

	static boolean isDirNode(IResource resource) {
		if (resource==null) {
			return false;
		}
		if (!(resource instanceof IFolder)) {
			return false;
		}
		final IFolder folder = (IFolder)resource;
		final String resourceName = resource.getName();
		final String decodedName = decode(resourceName);
		if (decodedName==null) {
			return false;
		}
		final IContainer container = folder.getParent();
		if (container==null || !container.exists()) {
			return false;
		}
		if (resourceName.endsWith(".dir")) {
			final IResource peerNode = container.findMember(decodedName);
			if (peerNode==null || !peerNode.exists()) {
				return false;
			}
		}
		// then it is likely the pattern that corresponds to the case
		// which we want to handle with this DirNode
		return true;
	}
	
	DirNode(JcrNode parent, Element domNode, IResource resource) {
		super(parent, domNode, resource);
		if (!isDirNode(resource)) {
			throw new IllegalArgumentException("resource is not a DirNode: "+resource);
		}
	}
	
	private String getDecodedName() {
		String name = getResource().getName();
		final String decodedName = decode(name);
		if (decodedName==null) {
			throw new IllegalStateException("Cannot decode node named '"+name+"'");
		}
		return decodedName;
	}
	
	@Override
	String getJcrPathName() {
		return getDecodedName();
	}
	
	@Override
	protected void addChild(JcrNode jcrNode) {
		JcrNode effectiveSibling = getEffectiveSibling();
		if (effectiveSibling!=this && effectiveSibling!=null) {
			// excellent, the parent contains a child which 
			// matches the .dir/_jcr_content pattern, so add this child there
			effectiveSibling.addChild(jcrNode);
			// but also hide this node from my parent
			effectiveSibling.getParent().hide(this);
			return;
		}
		super.addChild(jcrNode);
	}
	
	JcrNode getEffectiveSibling() {
		final String decodedName = getDecodedName();
		JcrNode nonDirNodeParent = parent;
		outerloop:while(nonDirNodeParent!=null && (nonDirNodeParent instanceof DirNode)) {
			final DirNode dirNodeParent = (DirNode)nonDirNodeParent;
			final String decodedParentName = dirNodeParent.getDecodedName();

			final Set<JcrNode> c = new HashSet<>(nonDirNodeParent.parent.children);
			for (JcrNode node : c) {
				if (node.getName().equals(decodedParentName)) {
					nonDirNodeParent = node;
					continue outerloop;
				}
			}
			nonDirNodeParent = nonDirNodeParent.parent;
		}
		return nonDirNodeParent.getChild(decodedName);
	}

	@Override
	public IFile getFileForEditor() {
		return null;
	}
	
	@Override
	public boolean canBeRenamed() {
		return false;
	}
	
	@Override
	public boolean canBeDeleted() {
        return true;
	}
	
	@Override
	public String getLabel() {
	    return getDecodedName();
	}
}

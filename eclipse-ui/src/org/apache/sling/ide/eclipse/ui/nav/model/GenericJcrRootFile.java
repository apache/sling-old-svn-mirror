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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.serialization.SerializationManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.xml.sax.SAXException;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLTokenizer.Type;

/** WIP: model object for a [.content.xml] shown in the content package view in project explorer **/
public class GenericJcrRootFile extends JcrNode {

	final IFile file;
	private final Document document;

	public GenericJcrRootFile(JcrNode parent, final IFile file) throws ParserConfigurationException, SAXException, IOException, CoreException {
		if (file==null) {
			throw new IllegalArgumentException("file must not be null");
		}
		this.file = file;
		setResource(file);
		if (parent==null) {
			throw new IllegalArgumentException("parent must not be null");
		}
		this.parent = parent;
		this.domElement = null;
		
        try (InputStream in = file.getContents()) {
            this.document = TolerantXMLParser.parse(in, file.getFullPath().toOSString());
            handleJcrRoot(this.document.getRootElement());
        }
	}
	
	@Override
	public int hashCode() {
		return file.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GenericJcrRootFile) {
			GenericJcrRootFile other = (GenericJcrRootFile) obj;
			return file.equals(other.file);
		}
		return false;
	}
	
	private void handleJcrRoot(Element element) {
		List<Element> children = element.getChildren();
		final JcrNode effectiveParent;
		if (isRootContentXml()) {
			if (parent instanceof DirNode) {
				DirNode dirNodeParent = (DirNode)parent;
				JcrNode effectiveSibling = dirNodeParent.getEffectiveSibling();
				if (effectiveSibling!=null) {
				    effectiveSibling.dirSibling = dirNodeParent;
				    handleProperties(element, effectiveSibling.properties);
				} else {
				    handleProperties(element, parent.properties);
				}
				effectiveParent = parent;
			} else {
				handleProperties(element, parent.properties);
				effectiveParent = parent;
			}
		} else {
			handleProperties(element, properties);
			effectiveParent = this;
			parent.addChild(this);
		}
		for (Iterator<Element> it = children.iterator(); it.hasNext();) {
			Element aChild = it.next();
			handleChild(effectiveParent, aChild);
		}
	}

	private boolean isRootContentXml() {
		return file.getName().equals(".content.xml");
	}
	
	private void handleProperties(Element domNode, ModifiableProperties properties) {
		properties.setNode(this, domNode);
//		NamedNodeMap attributes = domNode.getAttributes();
//		for(int i=0; i<attributes.getLength(); i++) {
//			Node attr = attributes.item(i);
//			properties.add(attr.getNodeName(), attr.getNodeValue());
//		}
	}

	@Override
	public String getLabel() {
		if (isRootContentXml()) {
			return "SHOULD NOT OCCUR";
		} else {
			// de-escape the file name
			
			String label = file.getName();

			// 1. remove the trailing .xml
			if (label.endsWith(".xml")) {
				label = label.substring(0, label.length()-4);
			}
			
			// 2. de-escape stuff like '_cq_' to 'cq:'
			if (label.startsWith("_")) {
				label = label.substring(1);
				int first = label.indexOf("_");
				if (first!=-1) {
					label = label.substring(0, first) + ":" + label.substring(first+1);
				}
			}
			return label;
		}
	}

	private void handleChild(JcrNode parent, Element domNode) {
		if (domNode.getType() == Type.TEXT) {
			// ignore
			return;
		}
		JcrNode childJcrNode = new JcrNode(parent, domNode, this, null);
		handleProperties(domNode, childJcrNode.properties);
		for (Element element : domNode.getChildren()) {
			handleChild(childJcrNode, element);
		}
	}

    public void pickResources(List<IResource> membersList) {
        
        final SerializationManager serializationManager = Activator.getDefault().getSerializationManager();

        for (Iterator<IResource> it = membersList.iterator(); it.hasNext();) {
            final IResource resource = it.next();
			final String resName = resource.getName();
            Iterator<JcrNode> it2;
			if (isRootContentXml()) {
				it2 = parent.children.iterator();
			} else {
				it2 = children.iterator();
			}
			while(it2.hasNext()) {
                JcrNode aChild = it2.next();
				if (resName.equals(serializationManager.getOsPath(aChild.getName()))) {
					// then pick this one
					it.remove();
					aChild.setResource(resource);
					break;
				}
			}
		}
		
	}
	
	@Override
	public IFile getFileForEditor() {
		return file;
	}
	
	@Override
	void createDomChild(String childNodeName, String childNodeType) {
        createChild(childNodeName, childNodeType, document.getRootElement(), underlying);
	}
	
	public void save() {
		try {
			String xml = document.toXML();
			file.setContents(new ByteArrayInputStream(xml.getBytes()), true, true, new NullProgressMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SyncDirManager.syncDirChanged(getSyncDir());
	}
	
	@Override
	public boolean canBeRenamed() {
	    return true;
	}
	
	@Override
	public boolean canBeDeleted() {
	    return true;
	}
	
	@Override
	public void rename(String string) {
        try {
            file.move(file.getParent().getFullPath().append(string+".xml"), true, new NullProgressMonitor());
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error("Error renaming resource ("+file+"): "+e, e);
        }
	}
	
	@Override
	public void delete() {
        try {
            file.delete(true, new NullProgressMonitor());
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error("Error deleting resource ("+file+"): "+e, e);
        }
	}
	
}

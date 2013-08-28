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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** WIP: model object for a [.content.xml] shown in the content package view in project explorer **/
public class GenericJcrRootFile extends JcrNode {

	final IFile file;
	private final Document document;

	public GenericJcrRootFile(JcrNode parent, IFile file) throws ParserConfigurationException, SAXException, IOException, CoreException {
		if (file==null) {
			throw new IllegalArgumentException("file must not be null");
		}
		this.file = file;
		setResource(file);
		if (parent==null) {
			throw new IllegalArgumentException("parent must not be null");
		}
		this.parent = parent;
		this.domNode = null;
		
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		this.document = docBuilder.parse(file.getContents());
		handleJcrRoot(this.document.getFirstChild());
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

	private void handleJcrRoot(Node domNode) {
		NodeList children = domNode.getChildNodes();
		final JcrNode effectiveParent;
		if (isRootContentXml()) {
			handleProperties(domNode, parent.properties);
			effectiveParent = parent;
		} else {
			handleProperties(domNode, properties);
			effectiveParent = this;
			parent.addChild(this);
		}
		for(int i=0; i<children.getLength(); i++) {
			handleChild(effectiveParent, children.item(i));
		}
	}

	private boolean isRootContentXml() {
		return file.getName().equals(".content.xml");
	}
	
	private void handleProperties(Node domNode, ModifiableProperties properties) {
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

	private void handleChild(JcrNode parent, Node domNode) {
		if (domNode.getNodeType() == Node.TEXT_NODE) {
			// ignore
			return;
		}
		if (domNode.getAttributes().getLength() == 0) {
			// then ignore this empty node 
			// either there is a file or a folder corresponding to this element
			// (in which case it will show up again anyway) 
			// or just an empty node without any further attributes such
			// as primaryType doesn't help a lot
			return;
		}
		JcrNode childJcrNode = new JcrNode(parent, domNode, this, null);
		handleProperties(domNode, childJcrNode.properties);
		NodeList children = domNode.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			handleChild(childJcrNode, children.item(i));
		}
	}

	public void pickResources(List<Object> membersList) {
		for (Iterator<Object> it = membersList.iterator(); it.hasNext();) {
			final IResource resource = (IResource) it.next();
			final String resName = resource.getName();
			Iterator it2;
			if (isRootContentXml()) {
				it2 = parent.children.iterator();
			} else {
				it2 = children.iterator();
			}
			while(it2.hasNext()) {
				JcrNode aChild = (JcrNode) it2.next();
				if (resName.equals(aChild.getName())) {
					// then pick this one
					it.remove();
					aChild.setResource(resource);
					break;
				}
			}
		}
		
	}
	
	@Override
	public boolean canBeOpenedInEditor() {
		return false;
	}

	public void save() {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
			file.setContents(new ByteArrayInputStream(out.toByteArray()), true, true, new NullProgressMonitor());
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
	}
	
}

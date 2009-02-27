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
package org.apache.sling.samples.postservletextensions.internal;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class LinkHelper {
	
	public LinkHelper() {
		
	}
	
	public void createSymetricLink(Node n1, Node n2, String name) throws PathNotFoundException, RepositoryException {
		createLink(n1, n2, name);
		createLink(n2, n1, name);
	}
	
	public void createLink(Node source, Node target, String name) throws PathNotFoundException, RepositoryException {
		String targetPath = target.getPath();
		ArrayList<String> newSourceLinks = new ArrayList<String>();
		if (source.hasProperty(name)) {
			Value[] sourceLinks = source.getProperty(name).getValues();
			for (Value sourceLink : sourceLinks) {
				newSourceLinks.add(sourceLink.getString());
			}
		}
		if (!newSourceLinks.contains(targetPath)) {
			newSourceLinks.add(targetPath);
		}
		String[] newSourceLinksArray = new String[newSourceLinks.size()];
		source.setProperty(name, newSourceLinks.toArray(newSourceLinksArray));
	}
	
	public void removeSymetricLink(Node n1, Node n2, String name) throws PathNotFoundException, RepositoryException {
		removeLink(n1, n2, name);
		removeLink(n2, n1, name);
	}
	
	public void removeLink(Node source, Node target, String name) throws PathNotFoundException, RepositoryException {
		String targetPath = target.getPath();
		Value[] sourceLinks = source.getProperty(name).getValues();
		ArrayList<String> newSourceLinks = new ArrayList<String>();
		for (Value sourceLink : sourceLinks) {
			String link = sourceLink.getString();
			if (!link.equals(target.getPath())) {
				newSourceLinks.add(link);
			}
		}
		String[] newSourceLinksArray = new String[newSourceLinks.size()];
		source.setProperty(name, newSourceLinks.toArray(newSourceLinksArray));
	}
	
}

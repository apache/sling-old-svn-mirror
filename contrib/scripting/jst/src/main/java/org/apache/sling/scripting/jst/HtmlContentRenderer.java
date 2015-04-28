/*
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
package org.apache.sling.scripting.jst;

import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.Resource;

public class HtmlContentRenderer {

	// TODO make this configurable
	private static final String BREADCRUMB_PREFIX = "/content/";

	public void render(PrintWriter pw, Resource r, Node n, String title)
	throws RepositoryException {
		pw.print("<h1 class='title'>");
		pw.print(escape(title));
		pw.println("</h1>");

		renderBreadcrumbs(pw, n);
		renderChildNodes(pw, n);

		for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
			final Property p = pi.nextProperty();
			if(displayProperty(p.getName())) {
				renderPropertyValue(pw, p);
			}
		}
	}

	protected void renderBreadcrumbs(PrintWriter pw, Node n) throws RepositoryException {
		final String path = n.getPath();
		pw.println("<div class='breadcrumbs'>");
		if (path.startsWith(BREADCRUMB_PREFIX) ) {
			final String [] crumbs = path.substring(BREADCRUMB_PREFIX.length()).split("/");
			// omit the last path element when iterating, it's this node's name
			for(int i=0; i < crumbs.length - 1; i++) {
				final String c = crumbs[i];
				pw.print("<a href='");
				pw.print(getDotDots(i, crumbs.length));
				pw.print(c);
				pw.print("'>");
				pw.print(c);
				pw.println("</a>");
			}
		}
		pw.println("</div>");
	}

	protected String getDotDots(int start, int len) {
		final StringBuffer sb = new StringBuffer();
		for(int i=start; i < len - 1; i++) {
			sb.append("../");
		}
		return sb.toString();
	}

	protected void renderChildNodes(PrintWriter pw, Node parent) throws RepositoryException {
		pw.println("<div class='childnodes'>");
		final String prefix = parent.getName() + "/";
		final NodeIterator it = parent.getNodes();
		while(it.hasNext()) {
			final Node kid = it.nextNode();
			pw.print("<a href='");
			pw.print(prefix);
			pw.print(kid.getName());
			pw.print("'>");
			pw.print(kid.getName());
			pw.println("</a>");
		}
		pw.println("</div>");
	}

	protected void renderPropertyValue(PrintWriter pw, Property p)
	throws RepositoryException {

		pw.print("<div class='" + p.getName() + "'>");

		if (p.getDefinition().isMultiple()) {
			Value[] values = p.getValues();
			pw.print('[');
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					pw.print(", ");
				}
				pw.print(escape(values[i].getString()));
			}
			pw.print(']');
		} else {
			pw.print(escape(p.getValue().getString()));
		}

		pw.println("</div>");
	}

	protected String escape(String str) {
		final StringBuffer sb = new StringBuffer();
		for(int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);
			if(c == '<') {
				sb.append("&lt;");
			} else if(c == '>') {
				sb.append("&gt;");
			} else if(c == '&') {
				sb.append("&amp;");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	protected boolean displayProperty(String name) {
		return !name.startsWith("jcr:") && !name.startsWith("sling:");
	}
}

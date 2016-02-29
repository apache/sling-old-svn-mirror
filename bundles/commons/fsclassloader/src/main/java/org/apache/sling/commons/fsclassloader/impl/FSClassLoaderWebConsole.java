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
package org.apache.sling.commons.fsclassloader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.service.component.ComponentContext;

/**
 * Web Console for the FileSystem Class Loader. Allows users to download Java
 * and Class files.
 */
@Component
@Service
@Properties({
		@Property(name = "service.description", value = "Web Console for the FileSystem Class Loader"),
		@Property(name = "service.vendor", value = "The Apache Software Foundation"),
		@Property(name = "felix.webconsole.label", value = FSClassLoaderWebConsole.APP_ROOT),
		@Property(name = "felix.webconsole.title", value = "File System Class Loader"),
		@Property(name = "felix.webconsole.css", value = { FSClassLoaderWebConsole.RES_LOC
				+ "/prettify.css" }),
		@Property(name = "felix.webconsole.category", value = "Sling") })
public class FSClassLoaderWebConsole extends AbstractWebConsolePlugin {

	static final String APP_ROOT = "fsclassloader";

	static final String RES_LOC = APP_ROOT + "/res/ui";

	/**
	 * Represents a set of class, java and deps files for a script.
	 */
	private static class ScriptFiles {

		/**
		 * Gets the script associated with the file.
		 * 
		 * @param file
		 *            the file to find the associate script
		 * @return the associated script
		 */
		public static String getScript(File file) {
			String relative = file.getAbsolutePath().substring(
					root.getAbsolutePath().length());
			String script = remove(relative, "/org/apache/jsp");
			script = remove(script, ".class");
			script = remove(script, ".java");
			script = remove(script, ".deps");
			if (File.separatorChar == '\\') {
				script = script.replace(File.separatorChar, '/');
			}
			return StringUtils.substringBeforeLast(script, "_") + "."
					+ StringUtils.substringAfterLast(script, "_");
		}

		private static String remove(String orig, String rem) {
			return orig.replace(rem, "");
		}

		private final String classFile;
		private final String depsFile;

		private final String javaFile;

		private final String script;

		public ScriptFiles(File file) {
			script = getScript(file);

			String relative = file.getAbsolutePath().substring(
					root.getAbsolutePath().length());

			relative = remove(relative, ".class");
			relative = remove(relative, ".deps");
			relative = remove(relative, ".java");
			classFile = relative + ".class";
			depsFile = relative + ".deps";
			javaFile = relative + ".java";
		}

		public String getClassFile() {
			return classFile;
		}

		public String getDepsFile() {
			return depsFile;
		}

		public String getJavaFile() {
			return javaFile;
		}

		public String getScript() {
			return script;
		}

	}

	/**
	 * The root under which the class files are under
	 */
	private static File root;

	/**
	 * The serialization UID
	 */
	private static final long serialVersionUID = -5728679635644481848L;

	/**
	 * The servlet configuration
	 */
	private ServletConfig config;

	/**
	 * Activate this component. Create the root directory.
	 * 
	 * @param componentContext the component context
	 * @throws MalformedURLException
	 */
	@Activate
	@SuppressWarnings("unused")
	protected void activate(final ComponentContext componentContext)
			throws MalformedURLException {
		// get the file root
		root = new File(componentContext.getBundleContext().getDataFile(""),
				"classes");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#destroy()
	 */
	public void destroy() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String file = request.getParameter("download");
		File toDownload = new File(root + file);
		if (!StringUtils.isEmpty(file)) {
			if (isValid(toDownload)) {
				InputStream is = null;
				try {
					is = new FileInputStream(toDownload);
					response.setHeader("Content-disposition",
							"attachment; filename=" + toDownload.getName());
					IOUtils.copy(is, response.getOutputStream());
				} finally {
					IOUtils.closeQuietly(is);
					IOUtils.closeQuietly(response.getOutputStream());
				}
			} else {
				response.sendError(404, "File " + file + " not found");
			}
		} else if (request.getRequestURI().endsWith(RES_LOC + "/prettify.css")) {
			response.setContentType("text/css");
			IOUtils.copy(
					getClass().getClassLoader().getResourceAsStream(
							"/res/ui/prettify.css"), response.getOutputStream());
		} else if (request.getRequestURI().endsWith(RES_LOC + "/prettify.js")) {
			response.setContentType("application/javascript");
			IOUtils.copy(
					getClass().getClassLoader().getResourceAsStream(
							"/res/ui/prettify.js"), response.getOutputStream());
		} else {
			super.doGet(request, response);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getLabel()
	 */
	@Override
	public String getLabel() {
		return "fsclassloader";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletConfig()
	 */
	public ServletConfig getServletConfig() {
		return this.config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletInfo()
	 */
	public String getServletInfo() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
	 */
	@Override
	public String getTitle() {
		return "File System Class Loader";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
	}

	/**
	 * Checks whether the specified file is a file and is underneath the root
	 * directory.
	 * 
	 * @param file
	 *            the file to check
	 * @return false if not a file or not under the root directory, true
	 *         otherwise
	 * @throws IOException
	 */
	private boolean isValid(File file) throws IOException {
		if (file.isFile()) {
			File parent = file.getCanonicalFile().getAbsoluteFile()
					.getParentFile();
			while (parent != null) {
				if (parent.getCanonicalPath().equals(root.getCanonicalPath())) {
					return true;
				}
				parent = parent.getParentFile();
			}
		}
		return false;
	}

	/**
	 * Reads all of the files under the current file.
	 * 
	 * @param file
	 *            the root file
	 * @param scripts
	 *            the map of scripts
	 * @throws IOException
	 *             an exception occurs reading the files
	 */
	private void readFiles(File file, Map<String, ScriptFiles> scripts)
			throws IOException {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File f : children) {
					readFiles(f, scripts);
				}
			}
		} else {
			String script = ScriptFiles.getScript(file);
			if (!scripts.containsKey(script)
					&& file.getName().endsWith(".java")) {
				scripts.put(script, new ScriptFiles(file));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax
	 * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void renderContent(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Map<String, ScriptFiles> scripts = new LinkedHashMap<String, ScriptFiles>();
		readFiles(root, scripts);

		Writer w = response.getWriter();

		w.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + RES_LOC
				+ "/prettify.css\"></link>");
		w.write("<script type=\"text/javascript\" src=\"" + RES_LOC
				+ "/prettify.js\"></script>");
		w.write("<script>$(document).ready(prettyPrint);</script>");
		w.write("<style>.prettyprint ol.linenums > li { list-style-type: decimal; } pre.prettyprint { white-space: pre-wrap; }</style>");
		String file = request.getParameter("view");
		File toView = new File(root + file);
		if (!StringUtils.isEmpty(file)) {
			if (isValid(toView)) {

				w.write("<p class=\"statline ui-state-highlight\">Viewing Script: "
						+ root + file + "</p><br/><br/>");
				
				ScriptFiles scriptFiles = new ScriptFiles(toView);

				w.write("<table class=\"nicetable ui-widget\">");
				w.write("<tr class=\"header ui-widget-header\">");
				w.write("<th>Script</th>");
				w.write("<th>Class</th>");
				w.write("<th>Deps</th>");
				w.write("<th>Java</th>");
				w.write("</tr>");
				w.write("<tr class=\"ui-state-default\">");
				w.write("<td>" + scriptFiles.getScript() + "</td>");
				w.write("<td>[<a href=\"?download="
						+ scriptFiles.getClassFile()
						+ "\" target=\"_blank\">download</a>]</td>");
				w.write("<td>[<a href=\"?download="
						+ scriptFiles.getDepsFile()
						+ "\" target=\"_blank\">download</a>]</td>");
				w.write("<td>[<a href=\"?download="
						+ scriptFiles.getJavaFile()
						+ "\" target=\"_blank\">download</a>]</td>");
				w.write("</tr>");
				w.write("</table><br/><br/>");
				InputStream is = null;
				try {
					is = new FileInputStream(toView);
					String contents = IOUtils.toString(is);
					w.write("<pre class=\"prettyprint linenums\">");
					StringEscapeUtils.escapeHtml(w, contents);
					w.write("</pre>");
				} finally {
					IOUtils.closeQuietly(is);
				}
			} else {
				response.sendError(404, "File " + file + " not found");
			}
		} else {

			w.write("<p class=\"statline ui-state-highlight\">File System ClassLoader Root: "
					+ root + "</p>");

			w.write("<table class=\"nicetable ui-widget\">");
			w.write("<tr class=\"header ui-widget-header\">");
			w.write("<th>View</th>");
			w.write("<th>Script</th>");
			w.write("</tr>");
			int i = 0;
			for (ScriptFiles scriptFiles : scripts.values()) {
				w.write("<tr class=\"" + (i % 2 == 0 ? "even" : "odd")
						+ " ui-state-default\">");
				w.write("<td>[<a href=\"?view=" + scriptFiles.getJavaFile()
						+ "\">view</a>]</td>");
				w.write("<td>" + scriptFiles.getScript() + "</td>");
				w.write("</tr>");
				i++;
			}
			w.write("</table>");
		}
	}
}

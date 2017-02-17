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

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a set of class, java and deps files for a script.
 */
public class ScriptFiles {

	/**
	 * Gets the script associated with the file.
	 *
	 * @param file
	 *            the file to find the associate script
	 * @return the associated script
	 */
	public static String getScript(File root, File file) {
		String relative = file.getAbsolutePath().substring(root.getAbsolutePath().length());
		String script = remove(relative, "/org/apache/jsp");
		script = remove(script, ".class");
		script = remove(script, ".java");
		script = remove(script, ".deps");
		if (File.separatorChar == '\\') {
			script = script.replace(File.separatorChar, '/');
		}
		return StringUtils.substringBeforeLast(script, "_") + "." + StringUtils.substringAfterLast(script, "_");
	}

	private static String remove(String orig, String rem) {
		return orig.replace(rem, "");
	}

	private final String classFile;
	private final String depsFile;

	private final String javaFile;

	private final String script;

	public ScriptFiles(final File root, final File file) {
		script = getScript(root, file);

		String relative = file.getAbsolutePath().substring(root.getAbsolutePath().length());

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
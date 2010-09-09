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
package org.apache.sling.scripting.javascript.helper;

import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ModuleScope extends ImporterTopLevel {

    private static final long serialVersionUID = -6063613037620927512L;

    private ExportsObject exports;
	private Scriptable module;
	private String name;

	public ModuleScope(Scriptable prototype, String moduleName) {
		this.setParentScope(null);
		this.setPrototype(prototype);
		this.activatePrototypeMap(3);

		this.name = moduleName;

		this.reset();
	}

	public Scriptable getExports() {
		return this.exports = (ExportsObject) get("exports", this);
	}

	public void reset() {
		this.module = new ModuleObject(this);
		int attr = READONLY | PERMANENT;
        ScriptableObject.defineProperty(this.module, "id", this.getModuleName(), attr);

		this.exports = new ExportsObject(this);
		this.defineProperty("exports", this.exports, DONTENUM);
	}

	public String getModuleName() {
		return this.name;
	}
}

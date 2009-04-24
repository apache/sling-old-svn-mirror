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
package org.apache.sling.scripting.xproc.xpl.impl;

import org.apache.sling.scripting.xproc.xpl.api.Environment;
import org.apache.sling.scripting.xproc.xpl.api.Step;

public abstract class AbstractStepImpl extends AbstractXplElementImpl implements Step {
	
	private Step container;
	private Environment env;
	
	public abstract void eval() throws Exception;
	
	public AbstractStepImpl() {
		this.env = new EnvironmentImpl();
	}
	
	public AbstractStepImpl(Environment env) {
		this.env = env;
	}
	
	public Step getContainer() {
		return container;
	}

	public void setContainer(Step container) {
		this.container = container;
	}
	
	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}
	
}

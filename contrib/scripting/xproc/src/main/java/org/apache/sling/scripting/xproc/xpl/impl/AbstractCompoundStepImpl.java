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

import java.util.Vector;

import org.apache.sling.scripting.xproc.xpl.api.CompoundStep;
import org.apache.sling.scripting.xproc.xpl.api.Step;
import org.apache.sling.scripting.xproc.xpl.api.XplElement;

public abstract class AbstractCompoundStepImpl extends AbstractStepImpl implements CompoundStep {

	private Vector<Step> subpipeline = new Vector<Step>();
	
	@Override
	public void eval() throws Exception {
		for (Step step : subpipeline) {
			step.eval();
		}
	}
	
	@Override
	public void addChild(XplElement child) {
		super.addChild(child);
		if (child instanceof Step) {
			Step stepChild = (Step) child;
			stepChild.setEnv(this.getEnv());
			subpipeline.add(stepChild);
		}
	}
	
	public Vector<Step> getSubpipeline() {
		return subpipeline;
	}

	public void setSubpipeline(Vector<Step> subpipeline) {
		this.subpipeline = subpipeline;
	}
	
}

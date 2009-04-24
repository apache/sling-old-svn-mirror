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

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.namespace.QName;

import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.component.sax.AbstractGenerator;
import org.apache.cocoon.pipeline.component.sax.XMLSerializer;
import org.apache.sling.scripting.xproc.cocoon.generator.SlingGenerator;
import org.apache.sling.scripting.xproc.xpl.XplConstants;
import org.apache.sling.scripting.xproc.xpl.api.Pipeline;
import org.apache.sling.scripting.xproc.xpl.api.Step;

public class PipelineImpl extends AbstractCompoundStepImpl implements Pipeline {

	@Override
	public void eval() throws Exception {		
		try {
			
			this.getEnv().setCcPipeline(new NonCachingPipeline());
			
			// generator
			AbstractGenerator generator = new SlingGenerator(this.getEnv().getSling());
			this.getEnv().getCcPipeline().addComponent(generator);
			
			// subpipeline evaluated
			for (Step step : this.getSubpipeline()) {
				step.eval();
			}
			
			this.getEnv().getCcPipeline().addComponent(new XMLSerializer());
			
			// Don't retrieve OutputStream from response until actually writing
			// to response, so that error handlers can retrieve it without getting
			// an error
			final OutputStream out = new OutputStreamWrapper() {
        @Override
        protected OutputStream getWrappedStream() throws IOException {
          return getEnv().getSling().getResponse().getOutputStream();
        }
			};
			
			this.getEnv().getCcPipeline().setup(out);
			this.getEnv().getCcPipeline().execute();
			
		} catch (Exception e) {
			String absPath = this.getEnv().getSling().getRequest().getResource().getPath();
			throw new Exception("Error in pipeline for resource: " + absPath, e);
		}
		
	}

	@Override
	public QName getQName() {
		return XplConstants.QNAME_PIPELINE;
	}
	
}
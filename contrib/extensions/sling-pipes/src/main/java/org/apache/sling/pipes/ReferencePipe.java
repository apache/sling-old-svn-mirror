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
package org.apache.sling.pipes;

import org.apache.sling.api.resource.Resource;

import java.util.Iterator;

/**
 * executes a pipe referred in the configuration
 */
public class ReferencePipe extends BasePipe {
    public static final String RESOURCE_TYPE = "slingPipes/reference";

    Pipe reference;

    public ReferencePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        Resource pipeResource = resolver.getResource(getPath());
        if (pipeResource == null){
            throw new Exception("Reference configuration error: There is no resource at " + getPath());
        }
        reference = plumber.getPipe(pipeResource);
        if (reference == null){
            throw new Exception("Unable to build out pipe out of " + getPath());
        }
    }

    @Override
    public Iterator<Resource> getOutput() {
        return reference.getOutput();
    }

    @Override
    public void setParent(ContainerPipe parent) {
        super.setParent(parent);
        reference.setParent(parent);
    }

    @Override
    public Object getOutputBinding() {
        return reference.getOutputBinding();
    }

    @Override
    public boolean modifiesContent() {
        return reference.modifiesContent();
    }

    @Override
    public Resource getInput() {
        return reference.getInput();
    }

    @Override
    public Resource getConfiguredInput() {
        return reference.getConfiguredInput();
    }
}
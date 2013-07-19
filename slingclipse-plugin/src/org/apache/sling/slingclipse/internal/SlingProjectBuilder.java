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
package org.apache.sling.slingclipse.internal;

import java.util.Map;

import org.apache.sling.slingclipse.SlingclipseListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class SlingProjectBuilder extends IncrementalProjectBuilder {

    public static final String SLING_BUILDER_ID = "org.apache.sling.slingclipse.SlingProjectBuilder";

    private static final IProject[] EMPTY_PROJECT_ARRAY = new IProject[0];

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        
        switch (kind) {
            case IncrementalProjectBuilder.AUTO_BUILD:
            case IncrementalProjectBuilder.INCREMENTAL_BUILD:
                return buildInternal(monitor);
        }
        
        return EMPTY_PROJECT_ARRAY;

    }

    private IProject[] buildInternal(IProgressMonitor monitor) throws CoreException {
        SlingclipseListener listener = new SlingclipseListener();

        IResourceDelta delta = getDelta(getProject());

        if (delta == null) {
            return EMPTY_PROJECT_ARRAY;
        }

        delta.accept(listener.buildVisitor());

        return new IProject[] { delta.getResource().getProject() };
    }
}

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
package org.apache.sling.ide.test.impl.helpers;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.rules.ExternalResource;

public class TemporaryProject extends ExternalResource {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private IProject project;

    @Override
    protected void before() throws Throwable {

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject("test-project-" + COUNTER.incrementAndGet());
        project.create(new NullProgressMonitor());
        project.open(new NullProgressMonitor());
    }

    @Override
    protected void after() {

        try {
            if (project != null) {
                project.delete(true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            // TODO - log?
        }
    }

    public IProject getProject() {
        return project;
    }

}

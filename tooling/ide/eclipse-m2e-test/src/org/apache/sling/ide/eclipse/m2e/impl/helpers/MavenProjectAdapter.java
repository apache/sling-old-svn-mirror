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
package org.apache.sling.ide.eclipse.m2e.impl.helpers;

import java.util.concurrent.Callable;

import org.apache.sling.ide.test.impl.helpers.Poller;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.actions.EnableNatureAction;
import org.hamcrest.CoreMatchers;

public class MavenProjectAdapter extends ProjectAdapter {

    public MavenProjectAdapter(IProject project) {
        super(project);
    }

    /**
     * Converts the wrapped project to a Maven project
     * 
     * <p>It waits for for the conversion to succeed, and fails with an unchecked exception
     * if the conversion does not succeed in the allocated time.</p>
     * 
     * @throws CoreException
     * @throws InterruptedException
     */
    public void convertToMavenProject() throws CoreException, InterruptedException {

        EnableNatureAction action = new EnableNatureAction();
        action.selectionChanged(null, new StructuredSelection(getProject()));
        action.run(null);
        
        new Poller().pollUntil(new Callable<Boolean> () {
            @Override
            public Boolean call() throws Exception {
                return getProject().hasNature(IMavenConstants.NATURE_ID);
            }
            
        }, CoreMatchers.equalTo(true));
    }

}

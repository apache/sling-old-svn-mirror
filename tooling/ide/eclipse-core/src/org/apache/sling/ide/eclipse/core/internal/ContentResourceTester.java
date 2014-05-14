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
package org.apache.sling.ide.eclipse.core.internal;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class ContentResourceTester extends PropertyTester {

    private static final String PN_CAN_BE_EXPORTED = "canBeExported";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (!PN_CAN_BE_EXPORTED.equals(property)) {
            return false;
        }

        // projects as such can always be exported
        if (receiver instanceof IProject) {
            return isContentProject(receiver);
        }

        // resources must be part of a content project and below the sync directory
        if (receiver instanceof IResource) {
            IResource resource = (IResource) receiver;
            IProject project = resource.getProject();
            boolean contentProject = isContentProject(project);

            if (!contentProject) {
                return false;
            }

            IFolder syncDirectory = ProjectUtil.getSyncDirectory(project);
            if (syncDirectory == null) {
                return false;
            }

            return syncDirectory.getFullPath().isPrefixOf(resource.getFullPath());
        }

        return false;
    }

    private boolean isContentProject(Object receiver) {
        IProject project = (IProject) receiver;

        return project != null && project.isOpen() && ProjectHelper.isContentProject(project);
    }

}

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
package org.apache.sling.ide.eclipse.m2e.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public abstract class AbstractProjectConfigurator extends org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator {

    private static final String MARKER_TYPE_PROJECT_CONFIGURATOR = "org.apache.sling.ide.eclipse-m2e-ui.projectconfiguratorproblem";

    /**
     * Adds a marker with the default type to the given resource.
     * 
     * @param resource
     *            the resource on which to set the marker (usually the pom.xml)
     * @param message
     *            message to connect with the marker
     * @param severity
     *            any of {@link IMarker#SEVERITY_ERROR}, {@link IMarker#SEVERITY_WARNING} or
     *            {@link IMarker#SEVERITY_INFO}
     */
    @SuppressWarnings("restriction")
    public void addMarker(IResource resource, String message, int severity) {
        markerManager.addMarker(resource, MARKER_TYPE_PROJECT_CONFIGURATOR, message, -1, severity);
    }

    /**
     * Deletes all markers of the default type in the given resource.
     * 
     * @param resource
     *            the resource from which to remove the markers
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    public void deleteMarkers(IResource resource) throws CoreException {
        markerManager.deleteMarkers(resource, true, MARKER_TYPE_PROJECT_CONFIGURATOR);
    }

}

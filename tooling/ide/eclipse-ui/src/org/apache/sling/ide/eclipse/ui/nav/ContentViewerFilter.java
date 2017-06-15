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
package org.apache.sling.ide.eclipse.ui.nav;

import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;;

/**
 * The <tt>ContentViewerFilter</tt> ensures that Sling content projects do not have 
 * the various WST-specific contributions present.
 *
 */
public class ContentViewerFilter extends ViewerFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        
        // the checks are not particularly robust but I was unable to find a better solution
        // see also https://www.eclipse.org/forums/index.php/t/1075144/
        switch ( element.getClass().getName()) {
            case "org.eclipse.jst.j2ee.navigator.internal.LoadingDDNode":
            case "org.eclipse.jst.j2ee.webapplication.internal.impl.WebAppImpl":
            case "org.eclipse.jst.jee.ui.internal.navigator.web.WebAppProvider":
            case "org.eclipse.jst.jee.ui.internal.navigator.LoadingGroupProvider":
            case "org.eclipse.jst.ws.jaxws.dom.integration.navigator.DOMAdapterFactoryContentProvider$LoadingWsProject":
            case "org.eclipse.m2e.wtp.internal.WTPResourcesNode":                

                IProject project = getProjectFromParent(parentElement);
                
                if ( ProjectHelper.isContentProject(project)) {
                    return false;
                }
                
                // intentional fall-through
            default:
                return true;
        }
    }

    private IProject getProjectFromParent(Object parentElement) {

        if (parentElement instanceof TreePath) {
            TreePath treePath = (TreePath) parentElement;
            // go through all segments from the bottom up until a project is found
            for (int n = treePath.getSegmentCount() - 1; n >= 0; n--) {
                Object segment = treePath.getSegment(n);
                if (segment instanceof IProject) {
                    return (IProject) segment;
                }
            }
        } else if (parentElement instanceof IProject) {
            return (IProject) parentElement;
        }
        return null;
    }

}

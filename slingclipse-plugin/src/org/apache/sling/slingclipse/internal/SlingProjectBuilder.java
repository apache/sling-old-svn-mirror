/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2012 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
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

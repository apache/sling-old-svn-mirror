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
package org.apache.sling.ide.eclipse.ui.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

public abstract class SelectionUtils {

    public static List<IServer> getServersLinkedToProject(IStructuredSelection sel, IProgressMonitor monitor) {

        if (sel.isEmpty()) {
            return Collections.emptyList();
        }

        Object first = sel.iterator().next();

        if (!(first instanceof IProject)) {
            return Collections.emptyList();
        }

        IProject project = (IProject) first;
        return getServersLinkedToProject(project, monitor);
    }

	public static List<IServer> getServersLinkedToProject(IProject project,
			IProgressMonitor monitor) {
		if (project == null) {
			return Collections.emptyList();
		}
		List<IServer> servers = new ArrayList<>();

        IModule[] modules = ServerUtil.getModules(project);

        for (IServer server : ServerCore.getServers()) {
            for (IModule module : modules) {
                if (ServerUtil.containsModule(server, module, monitor)) {
                    servers.add(server);
                }
            }
        }

        return servers;
	}
	
    /**
     * Returns the first object contained in the specified <tt>sel</tt>
     * 
     * @param selection the selection object
     * @param type the type of the selected object
     * @return the selected value, or <code>null</code>
     */
    @SuppressWarnings("unchecked") // cast guarded by type.isInstance
    public static <T> T getFirst(ISelection selection, Class<T> type) {

        if ( selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection) selection).getFirstElement();
            
            if ( type.isInstance(selected)) {
                return (T) selected;
            }
        }
        
        return null;
    }
    
    public static ISelection getSelectionFromEvaluationContext(Object evaluationContext) {
        if ( !(evaluationContext instanceof IEvaluationContext)) {
            return null;
        }
        
        IEvaluationContext ctx = (IEvaluationContext) evaluationContext;
        Object selection = ctx.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
        if ( !(selection instanceof ISelection)) {
            return null;
        }
        
        return (ISelection) selection;
    }

    private SelectionUtils() {

    }
}

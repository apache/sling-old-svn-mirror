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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.wst.server.core.IServer;

public class JVMDebuggerConnection {
	
	private ILaunch launch;

	boolean connectInDebugMode(ILaunch launch, IServer iServer, IProgressMonitor monitor)
			throws CoreException {
		this.launch = launch;
		boolean success = false;
		IVMConnector connector = null;
		connector = JavaRuntime.getVMConnector("org.eclipse.jdt.launching.socketAttachConnector");
		if (connector == null) {
			connector = JavaRuntime.getDefaultVMConnector();
		}
		if (connector == null) {
			throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.ide.eclipse.wst",
					"Could not get jvm connctor"));
		}
		
		ISlingLaunchpadServer launchpadServer = (ISlingLaunchpadServer) iServer.loadAdapter(SlingLaunchpadServer.class,
		        monitor);

		ISlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();
		
		int debugPort = configuration.getDebugPort();
		
		if (debugPort<=0) {
			throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.ide.eclipse.wst",
		            "debug port not configured"));
		}
		
		Map<String, String> connectMap = new HashMap<>();
		connectMap.put("hostname", iServer.getHost());
		connectMap.put("port", String.valueOf(debugPort));
		
//			Map argMap = null;//configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, (Map)null);
		
		int connectTimeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
		connectMap.put("timeout", Integer.toString(connectTimeout));  //$NON-NLS-1$

		// set the default source locator if required
		@SuppressWarnings("restriction")
		ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
		sourceLocator
				.setSourcePathComputer(DebugPlugin.getDefault().getLaunchManager()
						.getSourcePathComputer(
								"org.eclipse.jdt.launching.sourceLookup.javaSourcePathComputer")); //$NON-NLS-1$
		List<ISourceContainer> l = new LinkedList<>();
		IJavaProject[] javaProjects = ProjectHelper.getAllJavaProjects();
		if (javaProjects!=null) {
			for (int i = 0; i < javaProjects.length; i++) {
				IJavaProject javaProject = javaProjects[i];
				JavaProjectSourceContainer sc = new JavaProjectSourceContainer(javaProject);
				l.add(sc);
//					ISourceContainer[] scs = sc.getSourceContainers();
//					if (scs!=null && scs.length>0) {
//						for (int j = 0; j < scs.length; j++) {
//							ISourceContainer iSourceContainer = scs[j];
//							l.add(iSourceContainer);
//						}
//					} else {
//					}
			}
			ISourceContainer[] containers = l.toArray(new ISourceContainer[l.size()]);
			sourceLocator.setSourceContainers(containers);
			sourceLocator.initializeParticipants();
//			sourceLocator.initializeDefaults(configuration);
			launch.setSourceLocator(sourceLocator);
		}

//			setDefaultSourceLocator(getServer().getLaunch(), null);
		monitor.worked(1);		
		
		// connect to remote VM
		try{
			connector.connect(connectMap, monitor, launch);
			success = true;
		} catch(Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.ide.eclipse.wst",
		            "could not establish debug connection to "+iServer.getHost()+" : "+debugPort, e));
		}
		return success;
	}

	public void stop(boolean force) {
		IProcess[] processes = launch.getProcesses();
		if (processes!=null) {
			for (int i = 0; i < processes.length; i++) {
				IProcess iProcess = processes[i];
				try {
					iProcess.terminate();
				} catch (DebugException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		IDebugTarget[] debugTargets = launch.getDebugTargets();
		if (debugTargets != null) {
			for (int i = 0; i < debugTargets.length; i++) {
				IDebugTarget iDebugTarget = debugTargets[i];
				try {
					iDebugTarget.disconnect();
				} catch (DebugException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		IDebugTarget dt = launch.getDebugTarget();
		if (dt!=null) {
			try {
				dt.disconnect();
			} catch (DebugException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}



}

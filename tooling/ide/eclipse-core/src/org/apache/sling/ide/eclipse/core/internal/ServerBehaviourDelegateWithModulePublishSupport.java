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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

/**
 * Extension of ServerBehaviourDelegate which is capable of publishing
 * individual IModules - which the parent ServerBehaviourDelegate does
 * not support.
 * <p>
 * Copyright note: parts of this class have been migrated and adjusted from parent.
 * <p>
 * TODO remove once WST supports this
 */
public abstract class ServerBehaviourDelegateWithModulePublishSupport extends
        ServerBehaviourDelegate {

    private IAdaptable info3;
    private List<IModule[]> modules3;
    
    @Override
    public void publish(int kind, List<IModule[]> modules,
            IProgressMonitor monitor, IAdaptable info) throws CoreException {
        info3 = info;
        modules3 = modules==null ? null : new LinkedList<>(modules);
        super.publish(kind, modules, monitor, info);
    }
    
    // from WST's ServerBehavior
    private List<Integer> computeDelta(final List<IModule[]> moduleList) {

        final List<Integer> deltaKindList = new ArrayList<>();
        final Iterator<IModule[]> iterator = moduleList.iterator();
        while (iterator.hasNext()) {
            IModule[] module = iterator.next();
            if (hasBeenPublished(module)) {
                IModule m = module[module.length - 1];
                if ((m.getProject() != null && !m.getProject().isAccessible())
                        || getPublishedResourceDelta(module).length == 0) {
                    deltaKindList.add(new Integer(ServerBehaviourDelegate.NO_CHANGE));
                }
                else {
                    deltaKindList.add(new Integer(ServerBehaviourDelegate.CHANGED));
                }
            }
            else {
                deltaKindList.add(new Integer(ServerBehaviourDelegate.ADDED));
            }
        }
//        this.addRemovedModules(moduleList, null);
//        while (deltaKindList.size() < moduleList.size()) {
//            deltaKindList.add(new Integer(ServerBehaviourDelegate.REMOVED));
//        }
        return deltaKindList;
    }
    
    // from WST's ServerBehavior
    public IStatus publish(int kind, IProgressMonitor monitor) {
        Activator.getDefault().getPluginLogger().trace("-->-- Publishing to server: " + getServer().toString() + " -->--");
        
        if (getServer().getServerType().hasRuntime() && getServer().getRuntime() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "No runtime available", null);
        
        final List<IModule[]> moduleList = modules3==null ? getAllModules() : new LinkedList<>(modules3);//getAllModules();
        List<Integer> deltaKindList = computeDelta(moduleList);
        
        PublishOperation[] tasks = getTasks(kind, moduleList, deltaKindList);
        int size = 2000 + 3500 * moduleList.size() + 500 * tasks.length;
        
//        monitor = ProgressUtil.getMonitorFor(monitor); //TODO
        String mainTaskMsg = "Publishing to "+getServer().getName();//NLS.bind(Messages.publishing, getServer().getName());
        monitor.beginTask(mainTaskMsg, size);
        
        MultiStatus tempMulti = new MultiStatus(Activator.PLUGIN_ID, 0, "", null);
        
        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;
        
        try {
            Activator.getDefault().getPluginLogger().trace("Starting publish");
            publishStart(monitor);//ProgressUtil.getSubMonitorFor(monitor, 1000)); //TODO
            
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            
            // execute tasks
            MultiStatus taskStatus = performTasks(tasks, monitor);
            monitor.setTaskName(mainTaskMsg);
            if (taskStatus != null && !taskStatus.isOK())
                tempMulti.addAll(taskStatus);
            
            // execute publishers
            taskStatus = executePublishers(kind, moduleList, deltaKindList, monitor, info3);
            monitor.setTaskName(mainTaskMsg);
            if (taskStatus != null && !taskStatus.isOK())
                tempMulti.addAll(taskStatus);
            
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            
            // publish the server
            publishServer(kind, monitor);//ProgressUtil.getSubMonitorFor(monitor, 1000));//TODO
            monitor.setTaskName(mainTaskMsg);
            
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            
            // publish modules
            publishModules(kind, moduleList, deltaKindList, tempMulti, monitor);
            
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            
            monitor.done();
        } catch (CoreException ce) {
            Activator.getDefault().getPluginLogger().error("CoreException publishing to " + toString(), ce);
            return ce.getStatus();
        } catch (Exception e) {
            Activator.getDefault().getPluginLogger().error( "Error publishing  to " + toString(), e);
            tempMulti.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Error publishing", e));
        } finally {
            // end the publishing
            try {
                publishFinish(monitor);//ProgressUtil.getSubMonitorFor(monitor, 500));
            } catch (CoreException ce) {
                Activator.getDefault().getPluginLogger().error("CoreException publishing to " + toString(), ce);
                tempMulti.add(ce.getStatus());
            } catch (Exception e) {
                Activator.getDefault().getPluginLogger().error("Error stopping publish to " + toString(), e);
                tempMulti.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Error publishing", e));
            }
        }
        
        Activator.getDefault().getPluginLogger().trace("--<-- Done publishing --<--");
        
        if (tempMulti.getChildren().length == 1)
            return tempMulti.getChildren()[0];
        
        MultiStatus multi = null;
        if (tempMulti.getSeverity() == IStatus.OK)
            return Status.OK_STATUS;
        else if (tempMulti.getSeverity() == IStatus.INFO)
            multi = new MultiStatus(Activator.PLUGIN_ID, 0, "Publishing completed with information", null);
        else if (tempMulti.getSeverity() == IStatus.WARNING)
            multi = new MultiStatus(Activator.PLUGIN_ID, 0, "Publishing completed with a warning", null);
        else if (tempMulti.getSeverity() == IStatus.ERROR)
            multi = new MultiStatus(Activator.PLUGIN_ID, 0, "Publishing failed", null);
        
        if (multi != null)
            multi.addAll(tempMulti);
        
        return multi;
    }
    

}

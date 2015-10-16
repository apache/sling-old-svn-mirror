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
package org.apache.sling.ide.eclipse.ui.nav.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;

/**
 * TODO: Currently static - change into a service
 * <p>
 * This manager keeps track of SyncDir (models) and allows to 
 * register a listener that gets informed if the model changes.
 * <p>
 * Alternative would be to move the update/listener mechanism
 * into the SyncDir object itself..
 */
public class SyncDirManager {

    private static Map<IFolder,SyncDir> syncDirs = new HashMap<>();
    private static List<UpdateHandler> handlers = new LinkedList<>();
    
    public static void registerNewSyncDir(SyncDir syncDir) {
        syncDirs.put(syncDir.getFolder(), syncDir);
        syncDirChanged(syncDir);
    }

    public static void syncDirChanged(SyncDir syncDir) {
        List<UpdateHandler> handlersCopy;
        synchronized(handlers) {
            handlersCopy = new ArrayList<>(handlers);
        }
        for (UpdateHandler updateHandler : handlersCopy) {
            updateHandler.syncDirUpdated(syncDir);
        }
    }
    
    public static SyncDir getSyncDirOrNull(IFolder folder) {
        return syncDirs.get(folder);
    }
    
    public static void registerUpdateListener(UpdateHandler updateHandler) {
        synchronized(handlers) {
            handlers.add(updateHandler);
        }
    }

}

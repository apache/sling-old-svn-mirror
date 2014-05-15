package org.apache.sling.ide.eclipse.ui.nav.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    private static Map<IFolder,SyncDir> syncDirs = new HashMap<IFolder,SyncDir>();
    private static List<UpdateHandler> handlers = new LinkedList<UpdateHandler>();
    
    public static void registerNewSyncDir(SyncDir syncDir) {
        syncDirs.put(syncDir.getFolder(), syncDir);
        syncDirChanged(syncDir);
    }

    public static void syncDirChanged(SyncDir syncDir) {
        List<UpdateHandler> handlersCopy;
        synchronized(handlers) {
            handlersCopy = new ArrayList<UpdateHandler>(handlers);
        }
        for (Iterator it = handlersCopy.iterator(); it.hasNext();) {
            UpdateHandler updateHandler = (UpdateHandler) it.next();
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

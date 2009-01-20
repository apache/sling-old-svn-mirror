/*
 * $Url: $
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.osgi.framework.BundleException;

public class SlingFelix extends Felix {

    private final Notifiable notifiable;
    
    private Notifier notifierThread;
    
    public SlingFelix(Notifiable notifiable, Logger arg0, Map<?, ?> arg1, List<?> arg2) {
        super(arg0, arg1, arg2);
        this.notifiable = notifiable;
    }

    public SlingFelix(Notifiable notifiable, Map<?, ?> arg1, List<?> arg2) {
        super(arg1, arg2);
        this.notifiable = notifiable;
    }

    @Override
    public void update() throws BundleException {
        update(null);
    }
    
    @Override
    public void update(InputStream is) throws BundleException {
        // get the update file
        startNotifier(true, is);
        
        // just stop the framework now
        super.stop();
    }
    
    @Override
    public void stop() throws BundleException {
        startNotifier(false, null);
        super.stop();
    }

    public void stop(int status) throws BundleException {
        startNotifier(false, null);
        super.stop(/*TODO status*/);
    }
    
    private synchronized void startNotifier(boolean restart, InputStream ins) {
        if (notifierThread == null) {
            notifierThread = new Notifier(restart, ins);
            notifierThread.setDaemon(false);
            notifierThread.start();
        }
    }
    
    private class Notifier extends Thread {

        private final boolean restart;
        
        private final File updateFile;
        
        private Notifier(boolean restart, InputStream ins) {
            super("Sling Notifier");
            this.restart = restart;
            
            if (ins != null) {
                File tmpFile;
                try {
                    tmpFile = File.createTempFile("slingupdate", ".jar");
                    Loader.spool(ins, tmpFile);
                } catch (IOException ioe) {
                    // TOOD: log
                    tmpFile = null;
                }
                updateFile = tmpFile;
            } else {
                updateFile = null;
            }
        }
        
        @Override
        public void run() {
            stopAndWait();
            
            if (restart) {
                notifiable.updated(updateFile);
            } else {
                notifiable.stopped();
            }
        }
    }
}

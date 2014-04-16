/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.jarexec;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.exec.ProcessDestroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple ProcessDestroyer for a single process, meant to be used
 *  with our JarExecutor. 
 */
class ShutdownHookSingleProcessDestroyer implements ProcessDestroyer, Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Thread shutdownHookThread;
    private Process process;
    private final int timeoutSeconds;
    private final String processInfo;
    private boolean waitOnShutdown = false;
    
    public ShutdownHookSingleProcessDestroyer(String processInfo, int timeoutSeconds) {
        this.processInfo = processInfo;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public boolean getWaitOnShutdown() {
        return waitOnShutdown;
    }

    public void setWaitOnShutdown(boolean waitOnShutdown) {
        this.waitOnShutdown = waitOnShutdown;
    }

    public synchronized boolean add(Process p) {
        if(process != null) {
            throw new IllegalStateException("Process already set: " + process);
        }
        
        if(shutdownHookThread == null) {
            shutdownHookThread = new Thread(this, getClass().getSimpleName());
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        }
        
        process = p;
        return true;
    }

    public synchronized boolean remove(Process p) {
        p = null;
        return true;
    }

    public int size() {
        return 1;
    }
    
    public void run() {
        destroyProcess(waitOnShutdown);
    }
    
   public void destroyProcess(boolean waitForIt) {
       Process toDestroy = null;
       synchronized (this) {
           toDestroy = process;
           process = null;
       }
       
       if(toDestroy == null) {
           return;
       }
       
       toDestroy.destroy();
       
       if(waitForIt) {
           log.info("Waiting for destroyed process {} to exit (timeout={} seconds)", processInfo, timeoutSeconds);
           final Thread mainThread = Thread.currentThread();
           final Timer t = new Timer(true);
           final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mainThread.interrupt();
                }
           };
           t.schedule(task, timeoutSeconds * 1000L);
           try {
               toDestroy.waitFor();
               try {
                   final int exit = toDestroy.exitValue();
                   log.info("Process {} ended with exit code {}", processInfo, exit);
               } catch(IllegalStateException ise) {
                   log.error("Failed to destroy process " + processInfo);
               }
           } catch (InterruptedException e) {
               log.error("Timeout waiting for process " + processInfo + " to exit");
            } finally {
                t.cancel();
            }
       }
   }
}
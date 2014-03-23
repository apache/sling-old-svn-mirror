/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.threaddump.internal;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;

class BaseThreadDumper {

    boolean printThread(PrintWriter pw, long threadId, boolean withStackTrace) {
        // first get the root thread group
        ThreadGroup rootGroup = getRootThreadGroup();
        int numThreads = rootGroup.activeCount();
        Thread[] threads = new Thread[numThreads * 2];
        rootGroup.enumerate(threads);

        for (Thread thread : threads) {
            if (thread != null && thread.getId() == threadId) {
                printThread(pw, thread, withStackTrace);
                return true;
            }
        }

        return false;
    }

    void printThreads(PrintWriter pw, boolean withStackTrace) {
        // first get the root thread group
        ThreadGroup rootGroup = getRootThreadGroup();

        printThreadGroup(pw, rootGroup, withStackTrace);

        int numGroups = rootGroup.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        rootGroup.enumerate(groups);
        for (int i = 0; i < groups.length; i++) {
            printThreadGroup(pw, groups[i], withStackTrace);
        }

        pw.println();
    }

    private ThreadGroup getRootThreadGroup() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        return rootGroup;
    }

    private void printThreadGroup(PrintWriter pw, ThreadGroup group, boolean withStackTrace) {
        if (group != null) {
            StringBuffer info = new StringBuffer();
            info.append("ThreadGroup ").append(group.getName());
            info.append(" [");
            info.append("maxprio=").append(group.getMaxPriority());

            info.append(", parent=");
            if (group.getParent() != null) {
                info.append(group.getParent().getName());
            } else {
                info.append('-');
            }

            info.append(", isDaemon=").append(group.isDaemon());
            info.append(", isDestroyed=").append(group.isDestroyed());
            info.append(']');

            pw.println(info);

            int numThreads = group.activeCount();
            Thread[] threads = new Thread[numThreads * 2];
            group.enumerate(threads, false);
            for (int i = 0; i < threads.length; i++) {
                printThread(pw, threads[i], withStackTrace);
            }

            pw.println();
        }
    }

    private void printThread(PrintWriter pw, Thread thread, boolean withStackTrace) {
        if (thread != null) {
            StringBuffer info = new StringBuffer();
            info.append("  Thread ").append(thread.getId());
            info.append('/').append(thread.getName());
            info.append(" [");
            info.append("priority=").append(thread.getPriority());
            info.append(", alive=").append(thread.isAlive());
            info.append(", daemon=").append(thread.isDaemon());
            info.append(", interrupted=").append(thread.isInterrupted());
            info.append(", loader=").append(thread.getContextClassLoader());
            info.append(']');

            pw.println(info);

            if (withStackTrace) {
                printClassLoader(pw, thread.getContextClassLoader());
                printStackTrace(pw, thread.getStackTrace());
                pw.println();
            }
        }
    }

    private void printClassLoader(PrintWriter pw, ClassLoader classLoader) {
        if (classLoader != null) {
            pw.print("    ClassLoader=");
            pw.println(classLoader);
            pw.print("      Parent=");
            pw.println(classLoader.getParent());

            if (classLoader instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) classLoader;
                URL[] urls = loader.getURLs();
                if (urls != null && urls.length > 0) {
                    for (int i = 0; i < urls.length; i++) {
                        pw.print("      ");
                        pw.print(i);
                        pw.print(" - ");
                        pw.println(urls[i]);
                    }
                }
            }
        }
    }

    private void printStackTrace(PrintWriter pw, StackTraceElement[] stackTrace) {
        pw.println("    Stacktrace");
        if (stackTrace == null || stackTrace.length == 0) {
            pw.println("      -");
        } else {
            for (StackTraceElement stackTraceElement : stackTrace) {
                pw.print("      ");
                pw.println(stackTraceElement);
            }
        }
    }
}

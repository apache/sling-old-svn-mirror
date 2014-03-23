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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.felix.shell.Command;

public class ThreadDumpCommand extends BaseThreadDumper implements Command {

    private static final String CMD_NAME = "threads";

    private static final String OPT_STACK = "-s";

    public String getName() {
        return CMD_NAME;
    }

    public String getShortDescription() {
        return "dumps the JVM threads";
    }

    public String getUsage() {
        return CMD_NAME + " [" + OPT_STACK + "] <id> ...";
    }

    public void execute(String command, PrintStream out, PrintStream err) {

        // cut off leading command name
        if (command.startsWith(CMD_NAME)) {
            command = command.substring(CMD_NAME.length());
        }

        boolean longListing = false;
        LinkedList<Long> threadIds = new LinkedList<Long>();

        StringTokenizer tokener = new StringTokenizer(command, ", \t");
        while (tokener.hasMoreTokens()) {
            String token = tokener.nextToken().trim();
            if (OPT_STACK.equals(token)) {
                longListing = true;
            } else {
                try {
                    long threadId = Long.parseLong(token);
                    threadIds.add(threadId);
                } catch (NumberFormatException nfe) {
                    noSuchThread(err, token);
                }
            }
        }

        PrintWriter pw = new PrintWriter(out);

        if (threadIds.isEmpty()) {
            printThreads(pw, longListing);
        } else {
            while (!threadIds.isEmpty()) {
                Long threadId = threadIds.removeFirst();
                if (!printThread(pw, threadId, longListing)) {
                    noSuchThread(err, threadId);
                }
            }
        }

        pw.flush();
    }

    private void noSuchThread(PrintStream err, Object threadId) {
        err.println("No such Thread: " + threadId);
        err.flush();
    }

}

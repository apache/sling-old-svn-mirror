/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl.loop;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.sling.scripting.sightly.SightlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simulates an event loop for the Rhino JS engine.
 */
public class EventLoop {

    private static final Logger log = LoggerFactory.getLogger(EventLoop.class);

    private Queue<Task> taskQueue = new LinkedList<Task>();
    private boolean isRunning;

    /**
     * Add a task to the queue. If the queue is empty, start running tasks. If it
     * isn't empty, continue running the available tasks
     * @param task the task to be added
     */
    public void schedule(Task task) {
        taskQueue.offer(task);
        run();
    }

    private void run() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        try {
            // Holds the first exception encountered. If there is such a first exception, it will be
            // rethrown
            Exception thrownException = null;
            while (!taskQueue.isEmpty()) {
                Task task = taskQueue.poll();
                try {
                    task.run();
                } catch (Exception e) {
                    if (thrownException == null) {
                        thrownException = e; //first exception
                    } else {
                        log.error("Additional error occurred while running JS script: ", e);
                    }
                }
            }
            if (thrownException != null) {
                throw new SightlyException(thrownException);
            }
        } finally {
            isRunning = false;
        }
    }

}

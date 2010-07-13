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
package org.apache.sling.bgservlets.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.osgi.service.component.ComponentContext;

/** Simple ExecutionEngine
 * 	TODO should use Sling's thread pool, and check synergies
 * 	with scheduler services */
@Component
@Service
public class ExecutionEngineImpl implements ExecutionEngine {

	private Executor executor;
	
	@SuppressWarnings("serial")
	public static class QueueFullException extends SlingException {
		QueueFullException(Runnable r) {
			super("Execution queue is full, cannot execute " + r);
		}
	}
	
	public void activate(ComponentContext context) {
		// TODO configurable!
		final int corePoolSize = 2;
        int maximumPoolSize = 2;
        long keepAliveTime = 30;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(4); 
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				throw new QueueFullException(r);
			}
		};
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler); 
	}
	
	public void deactivate(ComponentContext context) {
		// TODO how to shutdown executor?
		executor = null;
	}
	
	public void queueForExecution(Runnable job) {
		// TODO wrap job in our own Runnable to detect start/end etc.
		executor.execute(job);
	}
}

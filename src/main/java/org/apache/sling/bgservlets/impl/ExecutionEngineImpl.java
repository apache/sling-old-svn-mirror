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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.Predicate;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple ExecutionEngine
 * 	TODO should use Sling's thread pool, and check synergies
 * 	with scheduler services */
@Component
@Service
public class ExecutionEngineImpl implements ExecutionEngine {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private Executor executor;
	private final Map<String, JobStatus> jobs = new HashMap<String, JobStatus>();
	
	private class RunnableWrapper implements Runnable {
		private final Runnable inputJob;
		private final JobStatus jobStatus;
		
		RunnableWrapper(Runnable inputJob) {
			this.inputJob = inputJob;
			jobStatus = (inputJob instanceof JobStatus ? (JobStatus)inputJob : null);
		}
		public void run() {
			if(jobStatus != null) {
				jobStatus.requestStateChange(JobStatus.State.RUNNING);
			}
			log.info("Starting job {}", inputJob);
			try {
				// TODO save Exceptions in job?
				inputJob.run();
			} finally {
				if(jobStatus != null) {
					jobStatus.requestStateChange(JobStatus.State.DONE);
				}
			}
			log.info("Done running job {}", inputJob);
		}
		
		JobStatus getJobStatus() {
			return jobStatus;
		}
	};
	
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
				onJobRejected(r);
			}
		};
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}
	
	public void deactivate(ComponentContext context) {
		// TODO how to shutdown executor?
		executor = null;
		
		// TODO cleanup jobs??
	}
	
	private void onJobRejected(Runnable r) {
		final RunnableWrapper w = (RunnableWrapper)r;
		if(w.getJobStatus() != null) {
			w.getJobStatus().requestStateChange(JobStatus.State.REJECTED);
		}
		log.info("Rejected job {}", r);
		throw new QueueFullException(r);
	}
	
	public void queueForExecution(final Runnable inputJob) {
		// Wrap job in our own Runnable to change its state as we execute it
		final RunnableWrapper w = new RunnableWrapper(inputJob);
		if(w.getJobStatus() != null) {
			w.getJobStatus().requestStateChange(JobStatus.State.QUEUED);
			// TODO when to cleanup?
			jobs.put(w.getJobStatus().getPath(), w.getJobStatus());
		}
		executor.execute(w);
	}
	
	public JobStatus getJobStatus(String path) {
		return jobs.get(path);
	}

	public Iterator<JobStatus> getMatchingJobStatus(Predicate<JobStatus> p) {
		// TODO take predicate into account
		// TODO sort by submission/execution time?
		return jobs.values().iterator();
	}
}
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
package org.apache.sling.commons.threads.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ThreadExpiringThreadPoolTest {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadExpiringThreadPoolTest.class);

    private static final int MAX_THREAD_AGE_MS = 90; // let threads expire after this many ms

    @Rule
    public ThreadPoolContext context = new ThreadPoolContext();


    /**
     * Attempts to isolate failures that happen > 0.2% of the time related to the
     * way in which the underlying thread pool behaves. This is not normally run as
     * a test , but use it if you want to isolate a rare failure.
     */
    //@Test
    public void shouldLetMultipleThreadsDieAfterExpiryMulti() {
        int fail = 0;
        int success = 0;
        for (int i = 0; i < 500; i++) {
            try {
                LOG.info("Running {} ", i);
                context = new ThreadPoolContext();
                context.before();
                shouldLetMultipleThreadsDieAfterExpiry();
                success++;
            } catch (Throwable e) {
                LOG.error("Failed ", e);
                fail++;
                fail("Race condition encountered");
            } finally {
                context.after();
            }
        }
        LOG.info("Failed {} sucess {}", fail, success);
        assertEquals(0, fail);
    }
    /**
     * Attempts to isolate failures that happen > 0.2% of the time related to the
     * way in which the underlying thread pool behaves. This is not normally run as
     * a test, but use it if you want to isolate a rare failure.
     */
    // @Test
    public void shouldCreateNewThreadAfterExpiryMulti() {

        int fail = 0;
        int success = 0;
        for (int i = 0; i < 500; i++) {
            try {
                LOG.info("Running {} ", i);
                context = new ThreadPoolContext();
                context.before();
                shouldCreateNewThreadAfterExpiry();
                success++;
            } catch (Throwable e ) {
                LOG.error("Failed ",e);
                fail++;
                fail("Race condition encountered");
            } finally {
                context.after();
            }
        }
        LOG.info("Failed {} sucess {}", fail, success);
        assertEquals(0, fail);
    }
    /**
     * Attempts to isolate failures that happen > 0.2% of the time related to the
     * way in which the underlying thread pool behaves. This is not normally run as
     * a test, but use it if you want to isolate a rare failure.
     */
    // @Test
    public void shouldCreateNewThreadAfterExpiryForFailingTasksMulti() {

        int fail = 0;
        int success = 0;
        for (int i = 0; i < 500; i++) {
            try {
                LOG.info("Running {} ", i);
                context = new ThreadPoolContext();
                context.before();
                shouldCreateNewThreadAfterExpiryForFailingTasks();
                success++;
            } catch (Throwable e ) {
                LOG.error("Failed ",e);
                fail++;
                fail("Race condition encountered");
            } finally {
                context.after();
            }
        }
        LOG.info("Failed {} sucess {}", fail, success);
        assertEquals(0, fail);

    }


    @Test
    public void shouldCreateNewThreadAfterExpiry() throws InterruptedException, ExecutionException {
        final TrackingThreadFactory threadFactory = context.getThreadFactory();
        final ThreadExpiringThreadPool pool = context.getPool();

        assertThat(threadFactory.getThreadCount(), is(0));

        assertExecutionByThread(pool, "test-thread-0");
        assertExecutionByThread(pool, "test-thread-0");
        assertExecutionByThread(pool, "test-thread-0");
        assertThat(threadFactory.getThreadCount(), is(1));

        letThreadsDie();

        // thread executes one more task after expiring
        assertExecutionByThread(pool, "test-thread-0");
        assertExecutionByThread(pool, "test-thread-1");
        assertThat(threadFactory.getThreadCount(), is(2));

        assertActiveThreads(threadFactory, "test-thread-1");
        assertExpiredThreads(threadFactory, "test-thread-0");
    }

    @Test
    public void shouldCreateNewThreadAfterExpiryForFailingTasks() throws InterruptedException, ExecutionException {
        final TrackingThreadFactory threadFactory = context.getThreadFactory();
        final ThreadExpiringThreadPool pool = context.getPool();

        assertThat(threadFactory.getThreadCount(), is(0));

        assertFailingSubmitThreadName(pool, "test-thread-0");
        assertFailingSubmitThreadName(pool, "test-thread-0");
        assertFailingSubmitThreadName(pool, "test-thread-0");
        assertThat(threadFactory.getThreadCount(), is(1));

        letThreadsDie();

        // thread executes one more task after expiring
        assertFailingSubmitThreadName(pool, "test-thread-0");
        assertFailingSubmitThreadName(pool, "test-thread-1");
        assertThat(threadFactory.getThreadCount(), is(2));

        assertActiveThreads(threadFactory, "test-thread-1");
        assertExpiredThreads(threadFactory, "test-thread-0");
    }

    @Test
    public void shouldLetMultipleThreadsDieAfterExpiry()
            throws ExecutionException, InterruptedException {

        final TrackingThreadFactory threadFactory = context.getThreadFactory();
        final ThreadExpiringThreadPool pool = context.getPool();
        pool.setCorePoolSize(3);
        pool.setMaximumPoolSize(3);

        assertParallelExecutionsByThread(pool, "test-thread-0", "test-thread-1", "test-thread-2");
        assertThat(threadFactory.getThreadCount(), is(3));

        letThreadsDie();
        // thread executes one more task after expiring
        executeParallelTasks(pool, 3);

        assertParallelExecutionsByThread(pool, "test-thread-3", "test-thread-4", "test-thread-5");
        assertThat(threadFactory.getThreadCount(), is(6));

        assertActiveThreads(threadFactory, "test-thread-3", "test-thread-4", "test-thread-5");
        assertExpiredThreads(threadFactory, "test-thread-0", "test-thread-1", "test-thread-2");
    }

    private void assertActiveThreads(final TrackingThreadFactory factory, final String... names) {
        assertThat("Active threads", factory.getActiveThreads(), equalTo(asSet(names)));
    }

    private void assertExpiredThreads(final TrackingThreadFactory factory, final String... names) {
        assertThat("Expired threads", factory.getExpiredThreads(), equalTo(asSet(names)));
    }

    private Set<String> asSet(final String... items) {
        return new HashSet<String>(asList(items));
    }

    private void assertParallelExecutionsByThread(final ExecutorService pool, final String... expectedThreads)
            throws InterruptedException {

        final Task[] tasks = executeParallelTasks(pool, 3);
        final List<String> threadNames = new ArrayList<String>();
        for (final Task task : tasks) {
            threadNames.add(task.executedBy);
        }
        for (final String expectedThread : expectedThreads) {
            assertTrue("No task was executed by " + expectedThread,
                    threadNames.remove(expectedThread));
            assertFalse("Multiple tasks were executed by " + expectedThread,
                    threadNames.contains(expectedThread));
        }
    }

    private Task[] executeParallelTasks(final ExecutorService pool, final int number)
            throws InterruptedException {
        final Task[] tasks = new Task[number];
        final CountDownLatch latch = new CountDownLatch(number);
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new Task(latch);
            pool.execute(tasks[i]);
        }
        pool.awaitTermination(MAX_THREAD_AGE_MS, TimeUnit.MILLISECONDS);
        return tasks;
    }

    private void assertExecutionByThread(final ExecutorService pool, final String expectedThread)
            throws ExecutionException, InterruptedException {
        final Task task = new Task();
        pool.submit(task).get();
        assertEquals("Thread name", expectedThread, task.executedBy);
    }

    private void assertFailingSubmitThreadName(final ExecutorService pool, final String expectedThread)
            throws ExecutionException, InterruptedException {
        final Task task = new ExceptionTask();
        try {
            pool.submit(task).get();
        } catch (ExecutionException e) {
            if (!e.getCause().getMessage().startsWith("ExceptionTask #")) {
                LOG.error("Unexpected exception: ", e);
                fail("Unexpected exception: " + e.getMessage());
            }
        }
        assertEquals("Thread name", expectedThread, task.executedBy);
    }

    private void letThreadsDie() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(MAX_THREAD_AGE_MS * 2);
    }

    private static class Task implements Runnable {

        private static int counter = 0;

        protected final int count;

        private final CountDownLatch mayFinish;

        protected String executedBy;

        Task() {
            this(new CountDownLatch(0));
        }

        Task(final CountDownLatch latch) {
            this.mayFinish = latch;
            this.count = counter++;
        }

        @Override
        public void run() {
            mayFinish.countDown();
            final Thread thread = Thread.currentThread();
            try {
                mayFinish.await();
            } catch (InterruptedException e) {
                thread.interrupt();
            }
            LOG.info("{} #{} running in thread {}",
                    new Object[] {getClass().getSimpleName(), count, thread});
            executedBy = thread.getName();
        }
    }

    private static class ExceptionTask extends Task {
        @Override
        public void run() {
            super.run();
            throw new RuntimeException("ExceptionTask #" + count);
        }
    }

    private static class TrackingThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger threadCount = new AtomicInteger(0);

        private final List<Thread> threadHistory = new CopyOnWriteArrayList<Thread>();

        public TrackingThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
        }

        public int getThreadCount() {
            return threadHistory.size();
        }

        public Set<String> getActiveThreads() {
            letThreadsDie();
            final HashSet<String> active = new HashSet<String>();
            for (final Thread thread : threadHistory) {
                if (thread.isAlive()) {
                    active.add(thread.getName());
                }
            }
            return active;
        }

        public Set<String> getExpiredThreads() {
            letThreadsDie();
            final HashSet<String> expired = new HashSet<String>();
            for (final Thread thread : threadHistory) {
                if (!thread.isAlive()) {
                    expired.add(thread.getName());
                }
            }
            return expired;
        }

        /**
         * This avoids a race condition where a thread has been evicted from the pool but is still alive becuase it evicted itself.
         * JDK8 java.util.concurrent.ThreadPoolExecutor does this. The 15ms assumes the process takes no more than 15ms to complete.
         * That is OS and VM dependent.
         */
        public void letThreadsDie() {
            try {
                Thread.sleep(15);
            } catch ( Exception e) {
                LOG.debug(e.getMessage(),e);
            }
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(group, r, "test-thread-" + threadCount.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            threadHistory.add(thread);
            LOG.info("Created thread {}", thread.getName());
            return thread;
        }
    }

    public static class ThreadPoolContext extends ExternalResource {

        public TrackingThreadFactory getThreadFactory() {
            return threadFactory;
        }

        public ThreadExpiringThreadPool getPool() {
            return pool;
        }

        private TrackingThreadFactory threadFactory;

        private ThreadExpiringThreadPool pool;

        @Override
        protected void before() throws Throwable {
            Task.counter = 0; // reset counter
            final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(20);
            final RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.AbortPolicy();
            threadFactory = new TrackingThreadFactory();
            pool = new ThreadExpiringThreadPool(
                    1, 1,
                    MAX_THREAD_AGE_MS, TimeUnit.MILLISECONDS,
                    1000, TimeUnit.MILLISECONDS,
                    queue, threadFactory, rejectionHandler);
        }

        @Override
        protected void after() {
            threadFactory = null;
            pool = null;
        }
    }
}


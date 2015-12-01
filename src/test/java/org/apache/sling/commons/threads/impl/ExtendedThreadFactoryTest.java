package org.apache.sling.commons.threads.impl;

import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority.MAX;
import static org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority.MIN;
import static org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority.NORM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtendedThreadFactoryTest {

    @Test
    public void informativeThreadNames() {
        final ExtendedThreadFactory tf = factory("Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", tf.newThread(null).getName());
        assertEquals("Thread name", "Sling - Test Pool #2", tf.newThread(null).getName());
    }

    @Test
    public void shouldStripSlingPrefixFromThreadNames() {
        final Thread thread = thread("Sling Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", thread.getName());
    }

    @Test
    public void shouldStripApacheSlingPrefixFromThreadNames() {
        final Thread thread = thread("Apache Sling Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", thread.getName());
    }

    @Test
    public void shouldSetCorrectPriority() {
        assertEquals("Thread min priority", Thread.MIN_PRIORITY, thread("Pool", MIN, false).getPriority());
        assertEquals("Thread normnal priority", Thread.NORM_PRIORITY, thread("Pool", NORM, false).getPriority());
        assertEquals("Thread max priority", Thread.MAX_PRIORITY, thread("Pool", MAX, false).getPriority());
    }

    @Test
    public void shouldSetDaemonStatusCorrectly() {
        assertFalse("Non-daemon thread", thread("Pool", NORM, false).isDaemon());
        assertTrue("Daemon thread", thread("Pool", NORM, true).isDaemon());
    }

    private Thread thread(final String poolName) {
        return factory(poolName).newThread(null);
    }

    private Thread thread(final String poolName,
                          final ThreadPoolConfig.ThreadPriority priority,
                          final boolean isDaemon) {
        return factory(poolName, priority, isDaemon).newThread(null);
    }

    private ExtendedThreadFactory factory(final String poolName) {
        return factory(poolName, NORM, false);
    }

    private ExtendedThreadFactory factory(final String poolName,
                                          final ThreadPoolConfig.ThreadPriority priority,
                                          final boolean isDaemon) {
        return new ExtendedThreadFactory(Executors.defaultThreadFactory(), poolName, priority, isDaemon);
    }
}

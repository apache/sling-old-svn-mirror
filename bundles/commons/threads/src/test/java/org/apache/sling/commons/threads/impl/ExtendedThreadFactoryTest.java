package org.apache.sling.commons.threads.impl;

import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ExtendedThreadFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExtendedThreadFactoryTest.class);

    @Test
    public void informativeThreadNames() {
        final ExtendedThreadFactory tf = createExtendedThreadFactory("Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", tf.newThread(null).getName());
        assertEquals("Thread name", "Sling - Test Pool #2", tf.newThread(null).getName());
    }

    @Test
    public void shouldStripSlingPrefixFromThreadNames() {
        final Thread thread = getFirstThreadFromNamedPool("Sling Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", thread.getName());
    }

    @Test
    public void shouldStripApacheSlingPrefixFromThreadNames() {
        final Thread thread = getFirstThreadFromNamedPool("Apache Sling Test Pool");
        assertEquals("Thread name", "Sling - Test Pool #1", thread.getName());
    }

    private Thread getFirstThreadFromNamedPool(final String poolName) {
        return createExtendedThreadFactory(poolName).newThread(null);
    }

    private ExtendedThreadFactory createExtendedThreadFactory(final String poolName) {
        return new ExtendedThreadFactory(
                Executors.defaultThreadFactory(),
                poolName,
                ThreadPoolConfig.ThreadPriority.NORM,
                false
        );
    }
}

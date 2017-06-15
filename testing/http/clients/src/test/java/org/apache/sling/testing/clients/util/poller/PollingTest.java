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
package org.apache.sling.testing.clients.util.poller;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PollingTest {
    @Test
    public void testCallOnce() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                return true;
            }
        };
        p.poll(500, 10);

        assertEquals(1, callCount.intValue());
    }

    @Test
    public void testCallTwice() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        final MutableBoolean called = new MutableBoolean(false);
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                boolean b = called.booleanValue();
                called.setTrue();
                return b;
            }
        };
        p.poll(500, 10);

        assertEquals(2, callCount.intValue());
    }

    @Test
    public void testCallTimeout() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                return false;
            }
        };

        try {
            p.poll(100, 10);
        } catch (TimeoutException e ) {
            assertTrue("Expected to execute call() at least 4 times, got instead only " + callCount.intValue() + " calls",
                    callCount.intValue() > 5);
            return;
        }

        fail("Did not reach timeout");
    }

    @Test
    public void testNegativeTimeout() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                return true;
            }
        };
        p.poll(-1, 10);

        assertEquals(1, callCount.intValue());
    }

    //
    // Tests with Callable
    //

    @Test
    public void testCallableOnce() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        final MutableBoolean called = new MutableBoolean(false);
        Polling p = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                return true;
            }
        });
        p.poll(500, 10);

        assertEquals(1, callCount.intValue());
    }

    @Test
    public void testCallableTwice() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        final MutableBoolean called = new MutableBoolean(false);
        Polling p = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                boolean b = called.booleanValue();
                called.setTrue();
                return b;
            }
        });
        p.poll(500, 10);

        assertEquals(2, callCount.intValue());
    }

    @Test
    public void testCallableTimeout() throws Exception {
        final MutableInt callCount = new MutableInt(0);
        Polling p = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                callCount.increment();
                return false;
            }
        });

        try {
            p.poll(100, 10);
        } catch (TimeoutException e ) {
            assertTrue("Expected to execute call() at least 4 times, got instead only " + callCount.intValue() + " calls",
                    callCount.intValue() > 5);
            return;
        }

        fail("Did not reach timeout");
    }


    @Test
    public void testCallPriority() throws Exception {
        Polling p = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return false;
            }
        }) {
            @Override
            public Boolean call() throws Exception {
                return true;
            }
        };

        // Should not reach timeout since overridden call() has priority over Callable param
        p.poll(100, 10);
    }
}
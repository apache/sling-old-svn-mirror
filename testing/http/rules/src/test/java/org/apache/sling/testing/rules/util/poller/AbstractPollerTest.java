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
package org.apache.sling.testing.rules.util.poller;

import org.apache.sling.testing.clients.util.poller.AbstractPoller;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractPollerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPollerTest.class);

    @Test
    public void testCallAndWaitSuccess() throws InterruptedException {
        AbstractPoller poller = new AbstractPoller(100, 5) {
            int callNumber = 0;

            @Override
            public boolean call() {
                return true;
            }

            @Override
            public boolean condition() {
                callNumber += 1;
                LOG.debug("Call nr " + callNumber);
                if (callNumber == 4) {
                    return true;
                }
                return false;
            }
        };
        Assert.assertTrue(poller.callAndWait());
    }

    @Test
    public void testCallAndWaitFailure() throws InterruptedException {
        AbstractPoller poller = new AbstractPoller(100, 5) {
            @Override
            public boolean call() {
                return true;
            }

            @Override
            public boolean condition() {
                return false;
            }
        };
        Assert.assertFalse(poller.callAndWait());
    }

    @Test
    public void testCallUntilSuccess() throws InterruptedException {
        AbstractPoller poller = new AbstractPoller(100, 5) {
            int callNumber = 0;

            @Override
            public boolean call() {
                callNumber += 1;
                LOG.debug("Call nr " + callNumber);
                return true;
            }

            @Override
            public boolean condition() {
                if (callNumber == 4) {
                    return true;
                }
                return false;
            }
        };
        Assert.assertTrue(poller.callUntilCondition());
    }

    @Test
    public void testCallUntilFailure() throws InterruptedException {
        AbstractPoller poller = new AbstractPoller(100, 5) {
            @Override
            public boolean call() {
                return true;
            }

            @Override
            public boolean condition() {
                return false;
            }
        };
        Assert.assertFalse(poller.callUntilCondition());
    }


}

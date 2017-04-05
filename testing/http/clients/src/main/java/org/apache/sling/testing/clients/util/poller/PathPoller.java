/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.clients.util.poller;

import org.apache.sling.testing.clients.AbstractSlingClient;
import org.apache.sling.testing.clients.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows polling for a resource
 */
public class PathPoller extends AbstractPoller {
    private static final Logger LOG = LoggerFactory.getLogger(PathPoller.class);
    private final AbstractSlingClient client;
    private final String path;
    private final int[] expectedStatus;
    private Exception exception;

    public PathPoller(AbstractSlingClient client, String path, long waitInterval, long waitCount, int... expectedStatus) {
        super(waitInterval, waitCount);
        this.client = client;
        this.path = path;
        if (null == expectedStatus || expectedStatus.length == 0) {
            this.expectedStatus = new int[]{200};
        } else {
            this.expectedStatus = expectedStatus;
        }
    }


    @Override
    public boolean call() {
        return true;
    }

    @Override
    public boolean condition() {
        try {
            client.doGet(path, expectedStatus);
        } catch (ClientException e) {
            LOG.warn("Get on {} failed: {}", path, e);
            this.exception = e;
            return false;
        }
        return true;
    }

    public Exception getException() {
        return exception;
    }
}

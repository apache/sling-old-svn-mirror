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
package org.apache.sling.testing.mock.osgi;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogService;

public class MockLogServiceTest {

    private LogService logService;

    @Before
    public void setUp() throws Exception {
        this.logService = new MockLogService(getClass());
    }

    @Test
    public void testLog() {
        this.logService.log(LogService.LOG_ERROR, "message 1");
        this.logService.log(LogService.LOG_WARNING, "message 1");
        this.logService.log(LogService.LOG_INFO, "message 1");
        this.logService.log(LogService.LOG_DEBUG, "message 1");

        this.logService.log(null, LogService.LOG_ERROR, "message 1");
        this.logService.log(null, LogService.LOG_WARNING, "message 1");
        this.logService.log(null, LogService.LOG_INFO, "message 1");
        this.logService.log(null, LogService.LOG_DEBUG, "message 1");
    }

    @Test
    public void testLogException() {
        this.logService.log(LogService.LOG_ERROR, "message 2", new Exception());
        this.logService.log(LogService.LOG_WARNING, "message 2", new Exception());
        this.logService.log(LogService.LOG_INFO, "message 2", new Exception());
        this.logService.log(LogService.LOG_DEBUG, "message 2", new Exception());

        this.logService.log(null, LogService.LOG_ERROR, "message 2", new Exception());
        this.logService.log(null, LogService.LOG_WARNING, "message 2", new Exception());
        this.logService.log(null, LogService.LOG_INFO, "message 2", new Exception());
        this.logService.log(null, LogService.LOG_DEBUG, "message 2", new Exception());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLogInvalidLevel() {
        this.logService.log(0, "message 1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLogExceptionInvalidLevel() {
        this.logService.log(0, "message 2", new Exception());
    }

}

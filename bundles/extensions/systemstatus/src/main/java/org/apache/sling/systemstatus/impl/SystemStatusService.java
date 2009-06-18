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

package org.apache.sling.systemstatus.impl;

import java.io.File;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.SystemStatus;

/** Default SystemStatus service - executes all scripts found
 *  under STATUS_PATH and considers system ready if none of
 *  them throws an Exception.
 *  
 * @scr.component metatype="no" immediate="true"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Default SystemStatus service"
 * @scr.service
 */
public class SystemStatusService implements SystemStatus {

    /** @inheritDoc */
    public void checkSystemReady() throws Exception {
        // TODO just a dummy implementation for now
        final File dummyTest = new File("/tmp/SystemStatusService.foo");
        if(dummyTest.exists()) {
            throw new SystemStatus.StatusException(
                    "Simulating 'system not ready' condition as "
                    + dummyTest.getAbsolutePath() + " file exists"
                    );
        }
    }

    /** @inheritDoc */
    public void clear() {
    }

    /** @inheritDoc */
    public void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws Exception {
        // TODO just a dummy implementation for now
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("System status will come here, work in progress - " + getClass().getName());
    }
    
}
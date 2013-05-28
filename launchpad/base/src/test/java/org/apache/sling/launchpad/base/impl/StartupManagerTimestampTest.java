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
package org.apache.sling.launchpad.base.impl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

/** Test the timestamp functions of the StartupManager */
public class StartupManagerTimestampTest {
    private StartupManager startupManager;
    
    @Before
    public void setup() throws IOException {
        final File tmpFile = File.createTempFile(getClass().getSimpleName(), "tmp");
        final String tmpDirName = tmpFile.getParentFile().getAbsolutePath();
        try {
            final Map<String, String> properties = new HashMap<String, String>();
            properties.put(SharedConstants.SLING_HOME, tmpDirName);
            properties.put(Constants.FRAMEWORK_STORAGE, tmpDirName);
            properties.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "42");
            
            final Logger logger = new Logger();
            startupManager = new StartupManager(properties, logger);
        } finally {
            tmpFile.delete();
        }
    }
    
    @Test
    public void testClassTimestamp() {
        final int defaultValue = Integer.MIN_VALUE;
        final long ts = startupManager.getTimeStampOfClass(getClass(), defaultValue);
        assertTrue("Expecting non-default timestamp, got " + ts, ts > defaultValue);
    }
}

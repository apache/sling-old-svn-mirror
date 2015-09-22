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
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import static org.junit.Assert.fail;
import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Test the teleporter client-side options */
public class TeleporterOptionsTest {

    public static final String OPTIONS = "TEST " + UUID.randomUUID();
    
    @SuppressWarnings("serial")
    public static class OptionsException extends RuntimeException {
        public OptionsException(String options) {
            super(options);
        }
    }
    
    @Test
    public void testOptions() {
        try {
            // The TeleporterRule options are usually meant to select which
            // server to run the tests on, for example - here we just verify
            // that the LaunchpadCustomizer gets our options
            final TeleporterRule r = TeleporterRule.forClass(getClass(), "Launchpad:" + OPTIONS);
            r.apply(null,  null);
            fail("Expecting an OptionsException");
        } catch(OptionsException oex) {
            assertEquals(OPTIONS, oex.getMessage());
        }
    }
}

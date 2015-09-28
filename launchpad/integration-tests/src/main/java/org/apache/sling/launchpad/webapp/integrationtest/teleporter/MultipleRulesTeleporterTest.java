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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.BundleContext;

/** Verify that additional JUnit Rules are handled correctly
 *  by the teleporter, by doing some funky stuff with a generator
 *  Rule and before/after statements. A custom Rule class is defined
 *  locally to also verify that it's correctly embedded in the generated
 *  test bundle.
 */
public class MultipleRulesTeleporterTest {

    private static int lastValue = 1;
    private static String lastTrace;
    private static int lastCounter;
    
    static class CounterRule extends ExternalResource {
        int counter = 0;

        @Override
        public Statement apply(Statement base, Description description) {
            counter++;
            return super.apply(base, description);
        }
    };

    /** Use a RuleChain to make sure the TeleporterRule runs first */
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    public final GeneratorRule<Integer> g = new GeneratorRule<Integer>(2,4,8);
    public final CounterRule counter = new CounterRule();
    
    @Rule
    public final RuleChain chain = RuleChain.outerRule(teleporter).around(counter).around(g);
    
    @Test
    public void testSquare() {
        assertEquals(lastValue * 2, g.getValue().intValue());
    }
    
    @Test
    public void testBundleContext() {
        final BundleContext bc = teleporter.getService(BundleContext.class);
        assertNotNull("Teleporter should provide a BundleContext", bc);
        assertEquals(lastValue * 2, g.getValue().intValue());
    }
    
    @After
    public void storeValues() {
        lastTrace = g.getTrace();
        lastCounter = counter.counter;
        lastValue = g.getValue();
    }
    
    @AfterClass
    public static void verifyTrace() {
        // This is where the abstraction is leaky - an AfterClass method will run
        // on both the client and server sides, but on the client no tests actually run,
        // so we need this condition here.
        if(TeleporterRule.isServerSide()) {
            assertEquals(1, lastCounter);
            final String expected ="-before-2-2--after-2--before-4-4--after-4--before-8";
            assertEquals(expected, lastTrace);
        }
    }
}
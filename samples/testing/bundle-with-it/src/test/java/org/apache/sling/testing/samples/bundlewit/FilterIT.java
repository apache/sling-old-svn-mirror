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
package org.apache.sling.testing.samples.bundlewit;

import org.junit.Assert;
import org.apache.sling.testing.junit.rules.SlingClassRule;
import org.apache.sling.testing.junit.rules.SlingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class FilterIT {

    @ClassRule
    public static SlingClassRule classRule = new SlingClassRule();

    @Rule
    public SlingRule methodRule = new SlingRule();

    @Test
    public void testShouldRun() {
        System.out.println("test should run");
    }

    @Test
    // This test should be skipped by the FilterRule inside the SlingRule
    // The "sling.it.ignorelist" property should be set to "*.FilterIT#testShouldNotRun"
    public  void testShouldNotRun() {
        System.out.println("ignorelist: " + System.getProperty("sling.it.ignorelist"));
        Assert.fail("Test should be filtered out by SlingRule");
    }
}

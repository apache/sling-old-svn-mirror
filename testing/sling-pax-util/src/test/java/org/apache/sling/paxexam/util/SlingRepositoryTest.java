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
package org.apache.sling.paxexam.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/** Verify that our tests have access to a functional Sling instance,
 *  and demonstrate how a simple test is setup.
 *  
 *  Note how little boilerplate this example test has. You basically just 
 *  define which version of the Sling launchpad bundle list you want to use, 
 *  and you can then @Inject any OSGi services to be tested, along with the 
 *  BundleContext.  
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingRepositoryTest {
    @Inject
    private SlingRepository repository;
    
    /** Use a released launchpad for this example */
    public static final String SLING_LAUNCHPAD_VERSION = "6";
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return SlingPaxOptions.defaultLaunchpadOptions(SLING_LAUNCHPAD_VERSION).getOptions();
    }

    @Test
    public void testNameDescriptor() {
        // We could use JUnit categories to select tests, as we
        // do in our integration, but let's avoid a dependency on 
        // that in this module
        if(System.getProperty("sling.run.modes", "").contains("oak")) {
            assertEquals("Apache Jackrabbit Oak", repository.getDescriptor("jcr.repository.name"));
        } else {
            assertEquals("Jackrabbit", repository.getDescriptor("jcr.repository.name"));
        }
    }
    
    @Test
    public void testLogin() throws RepositoryException {
        final Session s = repository.loginAdministrative(null);
        assertNotNull(s);
        s.logout();
    }
 }

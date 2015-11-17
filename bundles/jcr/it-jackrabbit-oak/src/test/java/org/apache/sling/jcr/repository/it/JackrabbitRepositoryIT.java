/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.jcr.repository.it;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JackrabbitRepositoryIT extends CommonTests {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final String jackrabbitVersion = System.getProperty("jackrabbit.version", "NO_JACKRABBIT_VERSION??");

        final List<Option> opt = new LinkedList<Option>();
        opt.addAll(commonOptions());

        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-api", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.derby", "derby", "10.12.1.1"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.jackrabbit.server", "2.3.1-SNAPSHOT"));

        return opt.toArray(new Option[]{});
    }

    @Test
    public void doCheckRepositoryDescriptors() {
        assertEquals("Jackrabbit", repository.getDescriptor("jcr.repository.name"));
    }

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
    }
}

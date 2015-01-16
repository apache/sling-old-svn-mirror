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

        final List<Option> opt = new LinkedList<Option>();
        opt.addAll(commonOptions());

        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-api", "2.6.5"));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.6.5"));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", "2.6.5"));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", "2.6.5"));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", "2.6.5"));
        opt.add(mavenBundle("org.apache.derby", "derby", "10.5.3.0_1"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.jackrabbit.server", "2.2.0"));

        return opt.toArray(new Option[]{});
    }

    @Override
    protected void doCheckRepositoryDescriptors() {
        assertEquals("Jackrabbit", repository.getDescriptor("jcr.repository.name"));
    }

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
    }
}

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
package org.apache.sling.launchpad.karaf;

import java.io.File;

import javax.inject.Inject;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BootstrapTest extends KarafTestSupport {

    @Inject
    @Filter(timeout = 120000)
    public SlingRepository slingRepository;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            karafDistributionConfiguration().frameworkUrl(maven().groupId(karafGroupId()).artifactId(karafArtifactId()).version(karafVersion()).type("tar.gz")).karafVersion(karafVersion()).name(karafName()).unpackDirectory(new File("target/paxexam/")),
            keepRuntimeFolder(),
            logLevel(LogLevelOption.LogLevel.INFO),
            editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresRepositories", ",mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.0-SNAPSHOT/xml/features"),
            editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresBoot", ",sling-karaf"),
            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j.rootLogger", "log4j.rootLogger=DEBUG, sift, osgi:*")
        };
    }

    @Ignore
    @Test
    public void testSlingRepository() throws Exception {
        assertNotNull(slingRepository);
    }

}

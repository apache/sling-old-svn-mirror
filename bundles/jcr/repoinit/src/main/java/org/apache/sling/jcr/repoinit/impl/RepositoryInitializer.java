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
package org.apache.sling.jcr.repoinit.impl;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SlingRepositoryInitializer that executes repoinit statements read
 *  from a configurable URL.
 */
@Designate(ocd = RepositoryInitializer.Config.class)
@Component(service = SlingRepositoryInitializer.class,
    configurationPolicy=ConfigurationPolicy.REQUIRE,
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
            // SlingRepositoryInitializers are executed in ascending
            // order of their service ranking
            Constants.SERVICE_RANKING + ":Integer=100"
    })
public class RepositoryInitializer implements SlingRepositoryInitializer {

    @ObjectClassDefinition(name = "Apache Sling Repository Initializer",
        description="Initializes the JCR content repository using repoinit statements")
    public @interface Config {

        @AttributeDefinition(name="Repoinit references",
            description=
                 "References to the source text that provides repoinit statements."
                + " format is either model@repoinit:<provisioning model URL> or raw:<raw URL>")
        String[] references() default {};
    }
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Reference
    private RepoInitParser parser;

    @Reference
    private JcrRepoInitOpsProcessor processor;

    private Config config;

    @Activate
    public void activate(Config config) {
        this.config = config;
        log.debug("Activated: {}", this.toString());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", references=" + Arrays.toString(config.references());
    }

    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        if ( config.references() != null && config.references().length > 0 ) {
            // loginAdministrative is ok here, definitely an admin operation
            final Session s = repo.loginAdministrative(null);
            try {
                final RepoinitTextProvider p = new RepoinitTextProvider();
                for(String reference : config.references()) {
                    final String repoinitText = p.getRepoinitText(reference);
                    final List<Operation> ops = parser.parse(new StringReader(repoinitText));
                    log.info("Executing {} repoinit operations", ops.size());
                    processor.apply(s, ops);
                    s.save();
                }
            } finally {
                s.logout();
            }
        }
    }
}

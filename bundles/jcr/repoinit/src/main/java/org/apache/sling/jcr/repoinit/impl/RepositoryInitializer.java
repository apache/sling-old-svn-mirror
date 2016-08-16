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
import java.util.Map;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SlingRepositoryInitializer that executes repoinit statements read
 *  from a configurable URL.
 */
@Component(
        label="Apache Sling Repository Initializer",
        description="Initializes the JCR content repository using repoinit statements",
        metatype=true)
@Service(SlingRepositoryInitializer.class)
@Properties({
    // SlingRepositoryInitializers are executed in ascending
    // order of their service ranking
    @Property(name=Constants.SERVICE_RANKING, intValue=100)
})
public class RepositoryInitializer implements SlingRepositoryInitializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_REFERENCE = "model@repoinit:context:/resources/provisioning/model.txt";
    
    @Property(
            label="Repoinit references", 
            description=
                 "References to the source text that provides repoinit statements."
                + " format is either model@repoinit:<provisioning model URL> or raw:<raw URL>"
            ,
            cardinality=Integer.MAX_VALUE,
            value={ DEFAULT_REFERENCE })
    public static final String PROP_REFERENCES = "references";
    private String [] references;
    
    @Reference
    private RepoInitParser parser;
    
    @Reference
    private JcrRepoInitOpsProcessor processor;
    
    @Activate
    public void activate(Map<String, Object> config) {
        warnForOldConfigParameters(config);
        references = PropertiesUtil.toStringArray(config.get(PROP_REFERENCES));
        log.debug("Activated: {}", this.toString());
    }
    
    /** Some config parameters are not used anymore as of V1.0.2, this logs 
     *  warnings if they are still used.
     */
    private void warnForOldConfigParameters(Map<String, Object> config) {
        final String [] names = {
                "text.url",
                "text.format",
                "model.section.name"
        };
        for(String name : names) {
            if(config.containsKey(name)) {
                log.warn("Configuration parameter '{}' is not used anymore, will be ignored", name);
            }
        }
        }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ", references=" + Arrays.asList(references);
    }

    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        // loginAdministrative is ok here, definitely an admin operation
        final Session s = repo.loginAdministrative(null);
        try {
            final RepoinitTextProvider p = new RepoinitTextProvider();
            for(String reference : references) {
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
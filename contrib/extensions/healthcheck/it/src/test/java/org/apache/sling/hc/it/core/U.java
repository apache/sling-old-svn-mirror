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
package org.apache.sling.muppet.it.core;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.sling.muppet.api.EvaluationResult;
import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.RulesEngine;
import org.ops4j.pax.exam.Option;

/** Test utilities */
public class U {
    
    static Option[] config(boolean includeRules) {
        final String coreVersion = System.getProperty("muppet.core.version");

        if(includeRules) {
            return options(
                    junitBundles(),
                    provision(
                            mavenBundle("org.apache.sling", "org.apache.sling.muppet.core", coreVersion),
                            mavenBundle("org.apache.sling", "org.apache.sling.muppet.rules", coreVersion)
                    )
            );
        } else {
            return options(
                    junitBundles(),
                    provision(
                            mavenBundle("org.apache.sling", "org.apache.sling.muppet.core", coreVersion)
                    )
            );
        }
    }
    
    static List<EvaluationResult> evaluateRules(MuppetFacade facade, String [] rules) throws IOException {
        final RulesEngine e = facade.getNewRulesEngine();
        final StringBuilder b = new StringBuilder();
        for(String line : rules) {
            b.append(line).append("\n");
        }
        e.addRules(facade.parseSimpleTextRules(new StringReader(b.toString())));
        return e.evaluateRules();
    }
    
    static void assertResult(EvaluationResult rr, EvaluationResult.Status status, String ruleString) {
        assertEquals("Expecting " + rr.getRule() + " result to match", status, rr.getStatus());
        assertEquals("Expecting " + rr.getRule() + " string to match", ruleString, rr.getRule().toString());
    }
}

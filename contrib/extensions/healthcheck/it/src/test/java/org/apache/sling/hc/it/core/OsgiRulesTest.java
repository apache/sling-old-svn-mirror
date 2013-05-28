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
package org.apache.sling.hc.it.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class OsgiRulesTest {
    
    @Inject
    private HealthCheckFacade facade;
    
    @Configuration
    public Option[] config() {
        return U.config(true);
    }
    
    
    @Test
    public void testBundleStateRules() throws IOException {
        // There should be at least one rule builder, but not a lot
        final String [] rules = { 
            "osgi:bundle.state:org.apache.sling.hc.core:active",
            "osgi:bundle.state:some.nonexistenbundle:active"
        };
        final List<EvaluationResult> r = U.evaluateRules(facade, rules);
        
        assertEquals(2, r.size());
        int i=0;
        U.assertResult(r.get(i++), true, "Rule: bundle.state:org.apache.sling.hc.core active");
        U.assertResult(r.get(i++), false, "Rule: bundle.state:some.nonexistenbundle active");
    }
}

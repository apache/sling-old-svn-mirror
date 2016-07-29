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
package org.apache.sling.jcr.repoinit;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.repoinit.impl.JcrRepoInitOpsProcessorImpl;
import org.apache.sling.jcr.repoinit.impl.RepositoryInitializer;
import org.apache.sling.jcr.repoinit.impl.TestUtil;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Test the two ways in which our RepositoryInitializer
 *  can read repoinit statements: either from a provisioning
 *  model file or directly as raw repoinit statements. 
 */
@RunWith(Parameterized.class)
public class RepositoryInitializerTest {
    
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private RepositoryInitializer initializer;
    private Map<String, Object> config;
    private TestUtil U;
    private final String repoInitText;
    private final String url;
    private final String modelSection;
    private final boolean testLogin;
    private final String serviceUser;
    
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<Object[]>();
        
        result.add(new Object[] { "All empty, just setup + parsing", "", false });
        result.add(new Object[] { "Using provisioning model", "SECTION_" + UUID.randomUUID(), true }); 
        result.add(new Object[] { "Raw repoinit/empty section", "", true}); 
        result.add(new Object[] { "Raw repoinit/null section", "", true}); 
        return result;
    }
    
    public RepositoryInitializerTest(String description, String modelSection, boolean testLogin) throws IOException {
        serviceUser = getClass().getSimpleName() + "-" + UUID.randomUUID();
        
        String txt = "create service user " + serviceUser; 
        if(modelSection != null && modelSection.length() > 0) {
            txt = "[feature name=foo]\n[:" + modelSection + "]\n" + txt; 
        }
        this.repoInitText = txt + "\n";
        this.url = getTestUrl(repoInitText);
        this.modelSection = modelSection;
        this.testLogin = testLogin;
    }
    
    @Before
    public void setup() throws Exception {
        U = new TestUtil(context);

        initializer = new RepositoryInitializer();
        config = new HashMap<String, Object>();
        config.put(RepositoryInitializer.PROP_TEXT_URL, url);
        config.put(RepositoryInitializer.PROP_MODEL_SECTION_NAME, modelSection);
        initializer.activate(config);
        
        context.registerInjectActivateService(new RepoInitParserService());
        context.registerInjectActivateService(new JcrRepoInitOpsProcessorImpl());
        context.registerInjectActivateService(initializer, config);
        
        // Mock environment doesn't cause this to be called
        initializer.processRepository(context.getService(SlingRepository.class));
    }
    
    @Test
    public void testLogin() throws Exception {
        if(testLogin) {
            try {
                U.loginService(serviceUser);
            } catch(Exception e) {
                fail("Login failed for " + serviceUser + " repoinit statements (" + repoInitText + ") not applied?");
            }
        }
    }
    
    /** Return the URL of a temporary file that contains repoInitText */
    private String getTestUrl(String repoInitText) throws IOException {
        final File tmpFile = File.createTempFile(getClass().getSimpleName(), "txt");
        tmpFile.deleteOnExit();
        final FileWriter w = new FileWriter(tmpFile);
        w.write(repoInitText);
        w.flush();
        w.close();
        return "file://" + tmpFile.getAbsolutePath();
    }
}
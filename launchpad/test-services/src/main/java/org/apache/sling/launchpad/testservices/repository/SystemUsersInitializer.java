/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.apache.sling.repoinit.jcr.JcrRepoInitOpVisitor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SlingRepositoryInitializer that creates system users and sets their ACLs.
 * Meant to be used for our integration tests until we can create those from
 * the provisioning model.
 */
@Component
@Service(SlingRepositoryInitializer.class)
public class SystemUsersInitializer implements SlingRepositoryInitializer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String REPOINIT_FILE = "/repoinit.txt";

    @Reference
    private RepoInitParser parser;
    
    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        final Session s = repo.loginAdministrative(null);
        final InputStream is = getClass().getResourceAsStream(REPOINIT_FILE);
        try {
            if(is == null) {
                throw new IOException("Class Resource not found:" + REPOINIT_FILE);
            }
            final Reader r = new InputStreamReader(is, "UTF-8");
            JcrRepoInitOpVisitor v = new JcrRepoInitOpVisitor(s);
            int count = 0;
            for(Operation op : parser.parse(r)) {
                op.accept(v);
                count++;
            }
            s.save();
            log.info("{} repoinit Operations executed", count);
        } finally {
            s.logout();
            is.close();
        }
    }
}

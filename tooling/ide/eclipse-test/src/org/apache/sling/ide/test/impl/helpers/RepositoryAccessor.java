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
package org.apache.sling.ide.test.impl.helpers;

import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.hamcrest.CoreMatchers;

/**
 * The <tt>RepositoryAccessor</tt> makes it simple to access and validate the contents of a Sling repository during
 * testing
 *
 */
public class RepositoryAccessor {

    private final LaunchpadConfig config;
    private final HttpClient client;
    private Repository repository;
    private Credentials credentials;

    public RepositoryAccessor(LaunchpadConfig config) {
        this.config = config;

        client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(new AuthScope(config.getHostname(), config.getPort()),
                new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
    }

    public void assertGetIsSuccessful(String path, String expectedResult) throws HttpException, IOException {

        GetMethod m = new GetMethod(config.getUrl() + path);
        try {
            int result = client.executeMethod(m);

            assertThat("Unexpected status call for " + m.getURI(), result, CoreMatchers.equalTo(200));
            assertThat("Unexpected response for " + m.getURI(), m.getResponseBodyAsString(),
                    CoreMatchers.equalTo(expectedResult));
        } finally {
            m.releaseConnection();
        }
    }

    public void assertGetReturns404(String path) throws HttpException, IOException {

        GetMethod m = new GetMethod(config.getUrl() + path);
        try {
            int result = client.executeMethod(m);

            assertThat("Unexpected status call for " + m.getURI(), result, CoreMatchers.equalTo(404));
        } finally {
            m.releaseConnection();
        }
    }

    public void tryDeleteResource(String path) throws RepositoryException {

        // PostMethod pm = new PostMethod(config.getUrl() + "hello.txt");
        // Part[] parts = { new StringPart(":operation", "delete") };
        // pm.setRequestEntity(new MultipartRequestEntity(parts, pm.getParams()));
        // try {
        // client.executeMethod(pm);
        // } finally {
        // pm.releaseConnection();
        // }

        Session session = login();
        if (session.nodeExists(path)) {
            session.removeItem(path);
            session.save();
        }
    }


    public Node getNode(String nodePath) throws RepositoryException {

        return login().getNode(nodePath);
    }

    private Session login() throws RepositoryException {
        
        RepositoryInfo repositoryInfo = new RepositoryInfo(config.getUsername(), config.getPassword(), config.getUrl());

        if (repository == null) {
            repository = RepositoryUtils.getRepository(repositoryInfo);
        }

        if (credentials == null) {
            credentials = RepositoryUtils.getCredentials(repositoryInfo);
        }
        
        return repository.login(credentials);
    }

}

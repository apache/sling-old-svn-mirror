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
package org.apache.sling.ide.osgi.impl;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osgi.framework.Version;

public class HttpOsgiClient implements OsgiClient {

    private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;

    private RepositoryInfo repositoryInfo;

    public HttpOsgiClient(RepositoryInfo repositoryInfo) {

        this.repositoryInfo = repositoryInfo;
    }

    @Override
    public Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException {

        GetMethod method = new GetMethod(repositoryInfo.appendPath("system/console/bundles.json"));
        HttpClient client = getHttpClient();

        try {
            int result = client.executeMethod(method);
            if (result != HttpStatus.SC_OK) {
                throw new HttpException("Got status code " + result + " for call to " + method.getURI());
            }

            try ( InputStream input = method.getResponseBodyAsStream() ) {

                JSONObject object = new JSONObject(new JSONTokener(new InputStreamReader(input)));
    
                JSONArray bundleData = object.getJSONArray("data");
                for (int i = 0; i < bundleData.length(); i++) {
                    JSONObject bundle = bundleData.getJSONObject(i);
                    String remotebundleSymbolicName = bundle.getString("symbolicName");
                    Version bundleVersion = new Version(bundle.getString("version"));
    
                    if (bundleSymbolicName.equals(remotebundleSymbolicName)) {
                        return bundleVersion;
                    }
                }
    
                return null;
            }
        } catch (IOException | JSONException e) {
            throw new OsgiClientException(e);
        } finally {
            method.releaseConnection();
        }
    }

    private HttpClient getHttpClient() {

        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS * 1000);
        client.getHttpConnectionManager().getParams().setSoTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS * 1000);
        client.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials(repositoryInfo.getUsername(),
                repositoryInfo.getPassword());
        client.getState().setCredentials(
                new AuthScope(repositoryInfo.getHost(), repositoryInfo.getPort(), AuthScope.ANY_REALM), defaultcreds);
        return client;
    }

    @Override
    public void installBundle(InputStream in, String fileName) throws OsgiClientException {

        if (in == null) {
            throw new IllegalArgumentException("in may not be null");
        }

        if (fileName == null) {
            throw new IllegalArgumentException("fileName may not be null");
        }

        // append pseudo path after root URL to not get redirected for nothing
        final PostMethod filePost = new PostMethod(repositoryInfo.appendPath("system/console/install"));

        try {
            // set referrer
            filePost.setRequestHeader("referer", "about:blank");

            List<Part> partList = new ArrayList<>();
            partList.add(new StringPart("action", "install"));
            partList.add(new StringPart("_noredir_", "_noredir_"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(in, baos);
            PartSource partSource = new ByteArrayPartSource(fileName, baos.toByteArray());
            partList.add(new FilePart("bundlefile", partSource));
            partList.add(new StringPart("bundlestart", "start"));

            Part[] parts = partList.toArray(new Part[partList.size()]);

            filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));

            int status = getHttpClient().executeMethod(filePost);
            if (status != 200) {
                throw new OsgiClientException("Method execution returned status " + status);
            }
        } catch (IOException e) {
            throw new OsgiClientException(e);
        } finally {
            filePost.releaseConnection();
        }
    }

    @Override
    public void installLocalBundle(final String explodedBundleLocation) throws OsgiClientException {

        if (explodedBundleLocation == null) {
            throw new IllegalArgumentException("explodedBundleLocation may not be null");
        }

        new LocalBundleInstaller(getHttpClient(), repositoryInfo) {

            @Override
            void configureRequest(PostMethod method) {
                method.addParameter("dir", explodedBundleLocation);
            }
        }.installBundle();
    }

    @Override
    public void installLocalBundle(final InputStream jarredBundle, String sourceLocation) throws OsgiClientException {

        if (jarredBundle == null) {
            throw new IllegalArgumentException("jarredBundle may not be null");
        }
        
        new LocalBundleInstaller(getHttpClient(), repositoryInfo) {

            @Override
            void configureRequest(PostMethod method) throws IOException {

                Part[] parts = new Part[] { new FilePart("bundle", new ByteArrayPartSource("bundle.jar",
                        IOUtils.toByteArray(jarredBundle))) };
                method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
            }
        }.installBundle();        
    }

    static abstract class LocalBundleInstaller {

        private final HttpClient httpClient;
        private final RepositoryInfo repositoryInfo;

        public LocalBundleInstaller(HttpClient httpClient, RepositoryInfo repositoryInfo) {
            this.httpClient = httpClient;
            this.repositoryInfo = repositoryInfo;
        }

        void installBundle() throws OsgiClientException {

            PostMethod method = new PostMethod(repositoryInfo.appendPath("system/sling/tooling/install"));

            try {
                configureRequest(method);

                int status = httpClient.executeMethod(method);
                if (status != 200) {
                    try {
                        JSONObject result = parseResult(method);
                        if (result.has("message")) {
                            throw new OsgiClientException(result.getString("message"));
                        }
                    } catch (JSONException e) {
                        // ignore, fallback to status code reporting
                    }
                    throw new OsgiClientException("Method execution returned status " + status);
                }

                JSONObject obj = parseResult(method);

                if ("OK".equals(obj.getString("status"))) {
                    return;
                }

                String errorMessage = obj.has("message") ? "Bundle deployment failed, please check the Sling logs"
                        : obj.getString("message");

                throw new OsgiClientException(errorMessage);

            } catch (IOException e) {
                throw new OsgiClientException(e);
            } catch (JSONException e) {
                throw new OsgiClientException(
                        "Response is not valid JSON. The InstallServlet is probably not installed at the expected location",
                        e);
            } finally {
                method.releaseConnection();
            }
        }

        private JSONObject parseResult(PostMethod method) throws IOException, JSONException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(method.getResponseBodyAsStream(), out);

            return new JSONObject(new String(out.toByteArray(), "UTF-8"));
        }

        abstract void configureRequest(PostMethod method) throws IOException;
    }
}

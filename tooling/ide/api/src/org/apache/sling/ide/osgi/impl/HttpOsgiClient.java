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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
import org.apache.sling.ide.osgi.SourceReference;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.framework.Version;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

public class HttpOsgiClient implements OsgiClient {

    private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;

    private RepositoryInfo repositoryInfo;

    public HttpOsgiClient(RepositoryInfo repositoryInfo) {

        this.repositoryInfo = repositoryInfo;
    }
    
    private static final class BundleInfo {
        private String symbolicName;
        private String version;

        public String getSymbolicName() {
            return symbolicName;
        }

        public Version getVersion() {
            return new Version(version);
        }
    }

    static Version getBundleVersionFromReader(String bundleSymbolicName, Reader reader) throws IOException {
        Gson gson = new Gson();
        try (JsonReader jsonReader = new JsonReader(reader)) {
            // wait for 'data' attribute
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals("data")) {
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        // read json for individual bundle
                        BundleInfo bundleInfo = gson.fromJson(jsonReader, BundleInfo.class);
                        if (bundleSymbolicName.equals(bundleInfo.getSymbolicName())) {
                            return bundleInfo.getVersion();
                        }
                    }
                    jsonReader.endArray();
                } else {
                    jsonReader.skipValue();
                }
            }
        }
        return null;
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

            try ( InputStream input = method.getResponseBodyAsStream();
                  Reader reader = new InputStreamReader(input, StandardCharsets.US_ASCII)) {
                return getBundleVersionFromReader(bundleSymbolicName, reader);
            }
        } catch (IOException e) {
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
    
    @Override
    public List<SourceReference> findSourceReferences() throws OsgiClientException {
        GetMethod method = new GetMethod(repositoryInfo.appendPath("system/sling/tooling/sourceReferences.json"));
        HttpClient client = getHttpClient();

        try {
            int result = client.executeMethod(method);
            if (result != HttpStatus.SC_OK) {
                throw new HttpException("Got status code " + result + " for call to " + method.getURI());
            }
            return parseSourceReferences(method.getResponseBodyAsStream());
        } catch (IOException e) {
            throw new OsgiClientException(e);
        } finally {
            method.releaseConnection();
        }
    }

    // visible for testing
    static List<SourceReference> parseSourceReferences(InputStream response) throws IOException {

        try (JsonReader jsonReader = new JsonReader(
                new InputStreamReader(response, StandardCharsets.US_ASCII))) {
            
            SourceBundleData[] refs = new Gson().fromJson(jsonReader, SourceBundleData[].class);
            List<SourceReference> res = new ArrayList<>(refs.length);
            for ( SourceBundleData sourceData : refs ) {
                for (  SourceReferenceFromJson ref : sourceData.sourceReferences ) {
                    if ( ref.isMavenType() ) {
                        res.add(ref.getMavenSourceReference());
                    }
                }
            }
            
            return res;
        }
    }

    /**
     * Encapsulates the JSON response from the tooling.installer
     */
    private static final class BundleInstallerResult {
        private String status; // either OK or FAILURE
        private String message;
        
        public boolean hasMessage() {
            if (message != null && message.length() > 0) {
                return true;
            }
            return false;
        }

        public String getMessage() {
            return message;
        }
        
        public boolean isSuccessful() {
            return "OK".equalsIgnoreCase(status);
        }
    }
    
    private static final class SourceBundleData {
        
        @SerializedName("Bundle-SymbolicName")
        private String bsn;
        @SerializedName("Bundle-Version")
        private String version;
        
        private List<SourceReferenceFromJson> sourceReferences;
    }
    
    private static final class SourceReferenceFromJson {
        @SerializedName("__type__")
        private String type; // should be "maven" 
        private String groupId;
        private String artifactId;
        private String version;
        
        public boolean isMavenType() {
            return "maven".equals(type);
        }
        
        public MavenSourceReferenceImpl getMavenSourceReference() {
            if (!isMavenType()) {
                throw new IllegalStateException("The type is not a Maven source reference but a " + type);
            }
            return new MavenSourceReferenceImpl(groupId, artifactId, version);
        }
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
                Gson gson = new Gson();
                int status = httpClient.executeMethod(method);
                
                try (JsonReader jsonReader = new JsonReader(
                        new InputStreamReader(method.getResponseBodyAsStream(), StandardCharsets.UTF_8))) {
                    BundleInstallerResult result = null;
                    if (status != 200) {
                        try {
                            result = gson.fromJson(jsonReader, BundleInstallerResult.class);
                            if (result.hasMessage()) {
                                throw new OsgiClientException(result.getMessage());
                            }
                        } catch (JsonParseException e) {
                            // ignore, fallback to status code reporting
                        }
                        throw new OsgiClientException("Method execution returned status " + status);
                    }
                    result = gson.fromJson(jsonReader, BundleInstallerResult.class);
                    if (!result.isSuccessful()) {
                        String errorMessage = !result.hasMessage() ? "Bundle deployment failed, please check the Sling logs"
                                : result.getMessage();
                        throw new OsgiClientException(errorMessage);
                    }
                }
            } catch (IOException e) {
                throw new OsgiClientException(e);
            } finally {
                method.releaseConnection();
            }
        }

        abstract void configureRequest(PostMethod method) throws IOException;
    }
}

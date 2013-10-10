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
import org.osgi.framework.Version;

public class HttpOsgiClient implements OsgiClient {

    private RepositoryInfo repositoryInfo;

    public HttpOsgiClient(RepositoryInfo repositoryInfo) {

        this.repositoryInfo = repositoryInfo;
    }

    @Override
    public Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException {

        GetMethod method = new GetMethod(repositoryInfo.getUrl() + "system/console/bundles.json");
        HttpClient client = getHttpClient();

        try {
            int result = client.executeMethod(method);
            if (result != HttpStatus.SC_OK) {
                throw new HttpException("Got status code " + result + " for call to " + method.getURI());
            }

            JSONObject object = new JSONObject(method.getResponseBodyAsString());

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
        } catch (HttpException e) {
            throw new OsgiClientException(e);
        } catch (IOException e) {
            throw new OsgiClientException(e);
        } catch (JSONException e) {
            throw new OsgiClientException(e);
        } finally {
            method.releaseConnection();
        }
    }

    private HttpClient getHttpClient() {

        HttpClient client = new HttpClient();
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
        final PostMethod filePost = new PostMethod(repositoryInfo.getUrl()+"system/console/install");

        try {
            // set referrer
            filePost.setRequestHeader("referer", "about:blank");

            List<Part> partList = new ArrayList<Part>();
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
    public void installLocalBundle(String explodedBundleLocation) throws OsgiClientException {

        if (explodedBundleLocation == null) {
            throw new IllegalArgumentException("explodedBundleLocation may not be null");
        }

        PostMethod method = new PostMethod(repositoryInfo.getUrl() + "system/sling/tooling/install");
        method.addParameter("dir", explodedBundleLocation);

        try {
            int status = getHttpClient().executeMethod(method);
            if (status != 200) {
                throw new OsgiClientException("Method execution returned status " + status);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(method.getResponseBodyAsStream(), out);

            System.out.println(new String(out.toByteArray(), "UTF-8"));

        } catch (IOException e) {
            throw new OsgiClientException(e);
        } finally {
            method.releaseConnection();
        }

    }

}

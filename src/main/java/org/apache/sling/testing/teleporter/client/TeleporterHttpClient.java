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
package org.apache.sling.testing.teleporter.client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.MultipleFailureException;

/** Barebones HTTP client that supports just what the teleporter needs,
 *  with no dependencies outside of java.* and org.junit. Prevents us 
 *  from imposing a particular HTTP client version. 
 */
class TeleporterHttpClient {
    private final String CHARSET = "UTF-8";
    private final String baseUrl;
    private String credentials = null;
    private final String testServletPath;
    
    TeleporterHttpClient(String baseUrl, String testServletPath) {
        this.baseUrl = baseUrl;
        if(!testServletPath.endsWith("/")) {
            testServletPath += "/";
        }
        this.testServletPath = testServletPath;
    }

    void setCredentials(String cred) {
        credentials = cred;
    }
    
    public void setConnectionCredentials(URLConnection c) {
        if(credentials != null && !credentials.isEmpty()) {
            final String basicAuth = "Basic " + new String(DatatypeConverter.printBase64Binary(credentials.getBytes()));
            c.setRequestProperty ("Authorization", basicAuth);
        }
    }

    /** Wait until specified URL returns specified status */
    public void waitForStatus(String url, int expectedStatus, int timeoutMsec) throws IOException {
        final long end = System.currentTimeMillis() + timeoutMsec;
        final Set<Integer> statusSet = new HashSet<Integer>();
        final ExponentialBackoffDelay d = new ExponentialBackoffDelay(50,  250);
        while(System.currentTimeMillis() < end) {
            try {
                final int status = getHttpGetStatus(url);
                statusSet.add(status);
                if(status == expectedStatus) {
                    return;
                }
                d.waitNextDelay();
            } catch(Exception ignore) {
            }
        }
        throw new IOException("Did not get status " + expectedStatus + " at " + url + " after " + timeoutMsec + " msec, got " + statusSet);
    }
    
    void installBundle(InputStream bundle, String bundleSymbolicName, int webConsoleReadyTimeoutSeconds) throws MalformedURLException, IOException {
        // Equivalent of
        // curl -u admin:admin -F action=install -Fbundlestart=1 -Fbundlefile=@somefile.jar http://localhost:8080/system/console/bundles
        final String url = baseUrl + "/system/console/bundles";
        final String contentType = "application/octet-stream";
        final HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        
        waitForStatus(url, 200, webConsoleReadyTimeoutSeconds * 1000);
        
        try {
            setConnectionCredentials(c);
            new MultipartAdapter(c, CHARSET)
            .parameter("action", "install")
            .parameter("bundlestart", "1")
            .file("bundlefile", bundleSymbolicName + ".jar", contentType, bundle)
            .close();
            final int status = c.getResponseCode();
            if(status != 302) {
                throw new IOException("Got status code " + status + " for " + url);
            }
        } finally {
            cleanup(c);
        }
    }

    void uninstallBundle(String bundleSymbolicName, int webConsoleReadyTimeoutSeconds) throws MalformedURLException, IOException {
        // equivalent of
        // curl -u admin:admin -F action=uninstall http://localhost:8080/system/console/bundles/$N
        final String url = baseUrl + "/system/console/bundles/" + bundleSymbolicName;
        final HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        
        waitForStatus(url, 200, webConsoleReadyTimeoutSeconds * 1000);
        
        try {
            setConnectionCredentials(c);
            new MultipartAdapter(c, CHARSET)
            .parameter("action", "uninstall")
            .close();
            final int status = c.getResponseCode();
            if(status != 200) {
                throw new IOException("Got status code " + status + " for " + url);
            }
        } finally {
            cleanup(c);
        }
    }
    
    public int getHttpGetStatus(String url) throws MalformedURLException, IOException {
        final HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        setConnectionCredentials(c);
        c.setUseCaches(false);
        c.setDoOutput(true);
        c.setDoInput(true);
        c.setInstanceFollowRedirects(false);
        try {
            return c.getResponseCode();
        } finally {
            cleanup(c);
        }
    }

    void runTests(String testSelectionPath, int testReadyTimeoutSeconds) throws MalformedURLException, IOException, MultipleFailureException {
        final String testUrl = baseUrl + "/" + testServletPath + testSelectionPath + ".junit_result";
        
        // Wait for non-404 response that signals that test bundle is ready
        final long timeout = System.currentTimeMillis() + (testReadyTimeoutSeconds * 1000L);
        final ExponentialBackoffDelay delay = new ExponentialBackoffDelay(25, 1000);
        while(true) {
            if(getHttpGetStatus(testUrl) == 200) {
                break;
            }
            if(System.currentTimeMillis() > timeout) {
                fail("Timeout waiting for test at " + testUrl + " (" + testReadyTimeoutSeconds + " seconds)");
                break;
            }
            delay.waitNextDelay();
        }
        
        final HttpURLConnection c = (HttpURLConnection)new URL(testUrl).openConnection();
        try {
        	setConnectionCredentials(c);
            c.setRequestMethod("POST");
            c.setUseCaches(false);
            c.setDoOutput(true);
            c.setDoInput(true);
            c.setInstanceFollowRedirects(false);
            final int status = c.getResponseCode();
            if(status != 200) {
                throw new IOException("Got status code " + status + " for " + testUrl);
            }
        
            final Result result = (Result)new ObjectInputStream(c.getInputStream()).readObject();
            if(result.getFailureCount() > 0) {
                final List<Throwable> failures = new ArrayList<Throwable>(result.getFailureCount());
                for (Failure f : result.getFailures()) {
                    failures.add(f.getException());
                }
                throw new MultipleFailureException(failures);
            }
        } catch(ClassNotFoundException e) {
            throw new IOException("Exception reading test results:" + e, e);
        } finally {
            cleanup(c);
        }
    }
    
    private void consumeAndClose(InputStream is) throws IOException {
        if(is == null) {
            return;
        }
        final byte [] buffer = new byte[16384];
        while(is.read(buffer) != -1) {
            // nothing to do, just consume the stream
        }
        is.close();
    }
    
    private void cleanup(HttpURLConnection c) {
        try {
            consumeAndClose(c.getInputStream());
        } catch(IOException ignored) {
        }
        try {
            consumeAndClose(c.getErrorStream());
        } catch(IOException ignored) {
        }
        c.disconnect();
    }
}

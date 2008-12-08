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
package org.apache.sling.jcr.jcrinstall.integrationtest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.jcr.jcrinstall.integrationtest.util.BundleCloner;
import org.osgi.framework.Bundle;

/** Base class for jcrinstall test cases */
public class JcrinstallTestBase extends HttpTestBase {
	
	public static final String JCRINSTALL_STATUS_PATH = "/system/sling/jcrinstall";
	public static final String DEFAULT_INSTALL_PATH = "/libs/integration-testing/install";
	public static final String DEFAULT_BUNDLE_NAME_PATTERN = "observer";
	private static long bundleCounter = System.currentTimeMillis();
	private static Set<String> installedClones;
	public static final String SCALE_FACTOR_PROP = "sling.test.scale.factor";
	public static final String DEFAULT_TIMEOUT_PROP = "sling.test.bundles.wait.seconds";
	protected int scaleFactor;
	protected int defaultBundlesTimeout;
	
    private class ShutdownThread extends Thread {
        @Override
        public void run() {
            try {
                System.out.println("Deleting " + installedClones.size() + " cloned bundles...");
                for(String path : installedClones) {
                	testClient.delete(WEBDAV_BASE_URL + path);
                }
            } catch(Exception e) {
                System.out.println("Exception in ShutdownThread:" + e);
            }
        }
        
    };
    
    @Override
	protected void setUp() throws Exception {
		super.setUp();
		scaleFactor = requireIntProperty(SCALE_FACTOR_PROP);
		defaultBundlesTimeout = requireIntProperty(DEFAULT_TIMEOUT_PROP);
		enableJcrinstallService(true);
    }
    
    @Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
		enableJcrinstallService(true);
	}



	protected int requireIntProperty(String systemPropertyKey) throws Exception {
    	final String s = System.getProperty(systemPropertyKey);
    	if(s == null) {
    		throw new Exception("Missing system property '" + systemPropertyKey + "'");
    	}
    	return Integer.valueOf(s);
    }
    
    /** Fail test if active bundles count is not expectedCount, after 
     * 	at most timeoutSeconds */
    protected void assertActiveBundleCount(String message, int expectedCount, int timeoutSeconds) throws IOException {
    	final long start = System.currentTimeMillis();
    	final long timeout = start + timeoutSeconds * 1000L;
    	int count = 0;
    	int lastCount = -1;
    	while(System.currentTimeMillis() < timeout) {
    		count = getActiveBundlesCount();
    		if(count != lastCount) {
    			// continue until the count is stable for at least one second
    			lastCount = count;
        		sleep(1000);
    			continue;
    		} else if(count == expectedCount) {
    			return;
    		}
    		sleep(500);
    	}
    	final long delta = System.currentTimeMillis() - start;
    	fail(message + ": expected " + expectedCount + " active bundles, found " + count
    			+ " after waiting " + delta / 1000.0 + " seconds");
    }
    
    protected void sleep(long millis) {
    	try {
    		Thread.sleep(millis);
    	} catch(InterruptedException ignore) {
    	}
    }
    
    protected int getActiveBundlesCount() throws IOException {
    	final String key = "bundles.in.state." + Bundle.ACTIVE;
    	final Properties props = getJcrInstallProperties();
    	int result = 0;
    	if(props.containsKey(key)) {
    		result = Integer.valueOf(props.getProperty(key));
    	}
    	return result;
    }
    
    protected boolean getJcrinstallServiceEnabled() throws IOException {
    	final Properties props = getJcrInstallProperties();
    	return "true".equals(props.get("jcrinstall.enabled"));
    }
    
    protected void enableJcrinstallService(boolean enable) throws IOException {
    	if(enable != getJcrinstallServiceEnabled()) {
            final PostMethod post = new PostMethod(HTTP_BASE_URL + JCRINSTALL_STATUS_PATH);
            post.setFollowRedirects(false);
            post.addParameter("enabled", String.valueOf(enable));
            final int status = httpClient.executeMethod(post);
            assertEquals("Expected status 200 in enableJcrinstallService", 200, status);
            assertEquals("Expected jcrinstall.enabled to be " + enable, enable, getJcrinstallServiceEnabled());
    	}
    }
    
    /** Return the Properties found at /system/sling/jcrinstall */ 
    protected Properties getJcrInstallProperties() throws IOException {
    	final String content = getContent(HTTP_BASE_URL + JCRINSTALL_STATUS_PATH, CONTENT_TYPE_PLAIN);
    	final Properties result = new Properties();
    	result.load(new ByteArrayInputStream(content.getBytes("UTF-8")));
    	return result;
    }

    /** Remove a cloned bundle that had been installed before */ 
    protected void removeClonedBundle(String path) throws IOException {
    	testClient.delete(WEBDAV_BASE_URL + path);
    	installedClones.remove(path);
    }
    
	/** Generate a clone of one of our test bundles, with unique bundle name and
	 * 	symbolic name, and install it via WebDAV. 
	 * @param bundleNamePattern The first test bundle that contains this pattern
	 * 	is used as a source. If null, uses DEFAULT_BUNDLE_NAME_PATTERN
	 * @param installPath if null, use DEFAULT_INSTALL_PATH
	 * @return the path of the installed bundle
	 */
	protected String installClonedBundle(String bundleNamePattern, String installPath) throws Exception {
		if(bundleNamePattern == null) {
			bundleNamePattern = DEFAULT_BUNDLE_NAME_PATTERN;
		}
		if(installPath == null) {
			installPath = DEFAULT_INSTALL_PATH;
		}
		
		// find test bundle to clone
		final File testBundlesDir = new File(System.getProperty("sling.testbundles.path"));
		if(!testBundlesDir.isDirectory()) {
			throw new IOException(testBundlesDir.getAbsolutePath() + " is not a directory");
		}
		File bundleSrc = null;
		for(String bundle : testBundlesDir.list()) {
			if(bundle.contains(bundleNamePattern)) {
				bundleSrc = new File(testBundlesDir, bundle);
				break;
			}
		}
		
		// clone bundle
		final File outputDir = new File(testBundlesDir, "cloned-bundles");
		outputDir.mkdirs();
		final String bundleId = bundleNamePattern + "_clone_" + bundleCounter++;
		final File clone = new File(outputDir, bundleId + ".jar");
		new BundleCloner().cloneBundle(bundleSrc, clone, bundleId, bundleId);
		
		// install clone by copying to repository - jcrinstall should then pick it up
		FileInputStream fis = new FileInputStream(clone);
		final String path = installPath + "/" + clone.getName();
		final String url = WEBDAV_BASE_URL + path;
		try {
			testClient.mkdirs(WEBDAV_BASE_URL, installPath);
			testClient.upload(url, fis);
			setupBundlesCleanup();
			installedClones.add(path);
		} finally {
			if(fis != null) {
				fis.close();
			}
		}
		
		return path;
	}
	
	/** If not done yet, register a shutdown hook to delete cloned bundles that
	 * 	we installed.
	 */
	private void setupBundlesCleanup() {
		synchronized (JcrinstallTestBase.class) {
			if(installedClones == null) {
				installedClones = new HashSet<String>();
				Runtime.getRuntime().addShutdownHook(new ShutdownThread());
			}
		}
	}
}
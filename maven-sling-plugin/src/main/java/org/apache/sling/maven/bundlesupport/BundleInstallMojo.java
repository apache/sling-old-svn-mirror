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

package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * Install an OSGi bundle to a running Sling instance.
 *
 * @goal install
 * @phase install
 * @description install an OSGi bundle jar to a running Sling instance
 */
public class BundleInstallMojo extends AbstractMojo {

    /**
     * Whether to skip this step even though it has been configured in the
     * project to be executed. This property may be set by the
     * <code>sling.install.skip</code> comparable to the <code>maven.test.skip</code>
     * property to prevent running the unit tests.
     * 
     * @parameter expression="${sling.install.skip}" default-value="false"
     * @required
     */
    private boolean skip;
    
	/**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * The name of the generated JAR file.
     *
     * @parameter alias="jarName" expression="${project.build.finalName}.jar"
     * @required
     */
    private String jarName;

    /**
     * The URL of the running Sling instance.
     *
     * @parameter expression="${sling.url}" default-value="http://localhost:8080/sling"
     * @required
     */
    private String slingUrl;
    
    /**
     * The user name to authenticate at the running Sling instance.
     *
     * @parameter expression="${sling.user}" default-value="admin"
     * @required
     */
    private String user;
    
    /**
     * The password to authenticate at the running Sling instance.
     *
     * @parameter expression="${sling.password}" default-value="admin"
     * @required
     */
    private String password;
    
    /**
     * The startlevel for the uploaded bundle
     *
     * @parameter expression="${sling.bundle.startlevel}" default-value="20"
     * @required
     */
    private String bundleStartLevel;
    
    /**
     * Whether to start the uploaded bundle or not
     *
     * @parameter expression="${sling.bundle.start}" default-value="true"
     * @required
     */
    private boolean bundleStart;

	/**
	 * Execute this Mojo
	 */
	public void execute() {
	    // don't do anything, if this step is to be skipped
	    if (skip) {
	        getLog().debug("Skipping bundle installation as instructed");
	        return;
	    }

        // only upload if packaging as an osgi-bundle
        File jarFile = new File(buildDirectory, jarName);
        String bundleName = isBundle(jarFile);
        if (bundleName == null) {
            getLog().info(jarFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        getLog().info("Installing Bundle " + bundleName + "(" + jarFile + ") to " + slingUrl);
        post(slingUrl, jarFile);
	}

	private void post(String targetURL, File file) {

        // append pseudo path after root URL to not get redirected for nothing
        PostMethod filePost = new PostMethod(targetURL + "/install");

        try {

            List<Part> partList = new ArrayList<Part>();
            partList.add(new StringPart("action", "install"));
            partList.add(new StringPart("_noredir_", "_noredir_"));
            partList.add(new FilePart("bundlefile", new FilePartSource(file.getName(), file)));
            partList.add(new StringPart("bundlestartlevel", bundleStartLevel));
            
            if (bundleStart) {
                partList.add(new StringPart("bundlestart", "start"));
            }

            Part[] parts = partList.toArray(new Part[partList.size()]);

            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(
                5000);
            
            // authentication stuff
            client.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
            client.getState().setCredentials(AuthScope.ANY, defaultcreds);
            
            int status = client.executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                getLog().info("Bundle installed");
            } else {
                getLog().error(
                    "Install failed, cause: " + HttpStatus.getStatusText(status));
            }
        } catch (ConnectException ce) {
            getLog().info("Install on " + targetURL + " failed, cause: " + ce.getMessage());
            getLog().debug(ce); // dump on debug
        } catch (Exception ex) {
            getLog().error(ex.getClass().getName() + " " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            filePost.releaseConnection();
        }
    }

    private String isBundle(File jarFile) {
        if (!jarFile.exists()) {
            getLog().debug("isBundle: " + jarFile + " does not exist");
            return null;
        }

        JarFile jaf = null;
        try {
            jaf = new JarFile(jarFile);
            Manifest manif = jaf.getManifest();
            if (manif == null) {
                getLog().debug("isBundle: Missing manifest in " + jarFile);
                return null;
            }

            String symbName =
                manif.getMainAttributes().getValue("Bundle-SymbolicName");
            if (symbName == null) {
                getLog().debug("isBundle: No Bundle-SymbolicName in " + jarFile);
                return null;
            }

            return symbName;
        } catch (IOException ioe) {
            getLog().warn("isBundle: Problem checking " + jarFile, ioe);
        } finally {
            if (jaf != null) {
                try {
                    jaf.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
        }

        // fall back to not being a bundle
        return null;
    }
}
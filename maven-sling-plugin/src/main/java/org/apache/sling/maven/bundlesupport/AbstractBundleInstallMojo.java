/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public abstract class AbstractBundleInstallMojo extends AbstractBundlePostMojo {

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

    public AbstractBundleInstallMojo() {
        super();
    }

    protected abstract String getBundleFileName() throws MojoExecutionException;
    
    public void execute() throws MojoExecutionException {

        // get the file to upload
        String bundleFileName = getBundleFileName();

        // only upload if packaging as an osgi-bundle
        File bundleFile = new File(bundleFileName);
        String bundleName = getBundleSymbolicName(bundleFile);
        if (bundleName == null) {
            getLog().info(bundleFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        getLog().info("Installing Bundle " + bundleName + "(" + bundleFile + ") to " + slingUrl);
        post(slingUrl, bundleFile);
    }

    protected void post(String targetURL, File file) {
    
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
                    "Installation failed, cause: " + HttpStatus.getStatusText(status));
            }
        } catch (ConnectException ce) {
            getLog().info("Installation on " + targetURL + " failed, cause: " + ce.getMessage());
            getLog().debug(ce); // dump on debug
        } catch (Exception ex) {
            getLog().error(ex.getClass().getName() + " " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            filePost.releaseConnection();
        }
    }

}
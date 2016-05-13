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

package org.apache.sling.testing.clients.osgi;

import org.apache.http.Header;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.poller.AbstractPoller;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.HttpUtils;
import org.apache.sling.testing.clients.util.poller.PathPoller;
import org.codehaus.jackson.JsonNode;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * A client that wraps the Felix OSGi Web Console REST API calls.
 * @see <a href=http://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html>
 *     Web Console RESTful API</a>
 */
public class OsgiConsoleClient extends SlingClient {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiConsoleClient.class);
    /**
     * All System Console REST API calls go to /system/console and below
     */
    private final String CONSOLE_ROOT_URL = "/system/console";

    /**
     * The URL for configuration requests
     */
    private final String URL_CONFIGURATION = CONSOLE_ROOT_URL + "/configMgr";

    /**
     * The URL for bundle requests
     */
    private final String URL_BUNDLES = CONSOLE_ROOT_URL + "/bundles";

    /**
     * The URL for components requests
     */
    private final String URL_COMPONENTS = CONSOLE_ROOT_URL + "/components";


    public static final String JSON_KEY_ID = "id";
    public static final String JSON_KEY_VERSION = "version";
    public static final String JSON_KEY_DATA = "data";
    public static final String JSON_KEY_STATE = "state";

    /**
     * Default constructor. Simply calls {@link SlingClient#SlingClient(URI, String, String)}
     *
     * @param serverUrl the URL to the server under test
     * @param userName the user name used for authentication
     * @param password the password for this user
     * @throws ClientException if the client cannot be instantiated
     */
    public OsgiConsoleClient(URI serverUrl, String userName, String password) throws ClientException {
        super(serverUrl, userName, password);
    }

    /**
     * Constructor used by adaptTo() and InternalBuilder classes. Should not be called directly in the code
     *
     * @param http http client to be used for requests
     * @param config sling specific configs
     * @throws ClientException if the client cannot be instantiated
     */
    public OsgiConsoleClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    /**
     * Returns the wrapper for the bundles info json
     *
     * @param expectedStatus list of accepted statuses of the response
     * @return all the bundles info
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public BundlesInfo getBundlesInfo(int... expectedStatus) throws ClientException {
        // request the bundles information
        SlingHttpResponse resp = this.doGet(URL_BUNDLES + ".json", HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
        // return the wrapper
        return new BundlesInfo(JsonUtils.getJsonNodeFromString(resp.getContent()));
    }

    /**
     * Returns the wrapper for the bundle info json
     *
     * @param id the id of the bundle
     * @param expectedStatus list of accepted statuses of the response
     * @return the bundle info
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public BundleInfo getBundleInfo(String id, int... expectedStatus) throws ClientException {
        SlingHttpResponse resp = this.doGet(URL_BUNDLES + "/" + id + ".json");
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
        return new BundleInfo(JsonUtils.getJsonNodeFromString(resp.getContent()));
    }

    /**
     * Returns the wrapper for the components info json
     *
     * @param expectedStatus list of accepted statuses of the response
     * @return the components info
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public ComponentsInfo getComponentsInfo(int... expectedStatus) throws ClientException {
        SlingHttpResponse resp = this.doGet(URL_COMPONENTS + ".json");
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
        return new ComponentsInfo(JsonUtils.getJsonNodeFromString(resp.getContent()));
    }

    /**
     * Returns the wrapper for the component info json
     *
     * @param id the id of the component
     * @param expectedStatus list of accepted statuses of the response
     * @return the component info
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public ComponentInfo getComponentInfo(String id, int expectedStatus) throws ClientException {
        SlingHttpResponse resp = this.doGet(URL_COMPONENTS + "/" + id + ".json");
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
        return new ComponentInfo(JsonUtils.getJsonNodeFromString(resp.getContent()));
    }

    //
    // OSGi configurations
    //

    /**
     * Returns a map of all properties set for the config referenced by the PID, where the map keys
     * are the property names.
     *
     * @param pid the pid of the configuration
     * @param expectedStatus list of accepted statuses of the response
     * @return the properties as a map
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public Map<String, Object> getConfiguration(String pid, int... expectedStatus) throws ClientException {
        // make the request
        SlingHttpResponse resp = this.doPost(URL_CONFIGURATION + "/" + pid, null);
        // check the returned status
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
        // get the JSON node
        JsonNode rootNode = JsonUtils.getJsonNodeFromString(resp.getContent());
        // go through the params
        Map<String, Object> props = new HashMap<String, Object>();
        if(rootNode.get("properties") == null)
            return props;
        JsonNode properties = rootNode.get("properties");
        for(Iterator<String> it = properties.getFieldNames(); it.hasNext();) {
            String propName = it.next();
            JsonNode value = properties.get(propName).get("value");
            if(value != null) {
                props.put(propName, value.getValueAsText());
                continue;
            }
            value = properties.get(propName).get("values");
            if(value != null) {
                Iterator<JsonNode> iter = value.getElements();
                List<String> list = new ArrayList<String>();
                while(iter.hasNext()) {
                    list.add(iter.next().getValueAsText());
                }
                props.put(propName, list.toArray(new String[list.size()]));
            }
        }
        return props;
    }

    /**
     * Returns a map of all properties set for the config referenced by the PID, where the map keys
     * are the property names. The method waits until the configuration has been set.
     *
     * @param waitCount The number of maximum wait intervals of 500ms.
     *                  Between each wait interval, the method polls the backend to see if the configuration ahs been set.
     * @param pid pid
     * @param expectedStatus expected response status
     * @return the config properties
     * @throws ClientException if the response status does not match any of the expectedStatus
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public Map<String, Object> getConfigurationWithWait(long waitCount, String pid, int... expectedStatus)
            throws ClientException, InterruptedException {
        ConfigurationPoller poller = new ConfigurationPoller(500L, waitCount, pid, expectedStatus);
        if (!poller.callUntilCondition())
            return getConfiguration(pid, expectedStatus);
        return poller.getConfig();
    }

    /**
     * Sets properties of a config referenced by its PID. the properties to be edited are passed as
     * a map of property name,value pairs.
     *
     * @param PID Persistent identity string
     * @param factoryPID Factory persistent identity string or {@code null}
     * @param configProperties map of properties
     * @param expectedStatus expected response status
     * @return the location of the config
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public String editConfiguration(String PID, String factoryPID, Map<String, Object> configProperties, int... expectedStatus)
            throws ClientException {
        FormEntityBuilder builder = FormEntityBuilder.create();
        builder.addParameter("apply", "true");
        builder.addParameter("action", "ajaxConfigManager");
        // send factory PID if set
        if (factoryPID != null) {
            builder.addParameter("factoryPid", factoryPID);
        }
        // add properties to edit
        StringBuilder propertyList = new StringBuilder("");
        for (String propName : configProperties.keySet()) {
            Object o = configProperties.get(propName);
            if (o instanceof String) {
                builder.addParameter(propName, (String)o);
            } else if (o instanceof String[]) {
                for (String s : (String[])o) {
                    builder.addParameter(propName, s);
                }
            }
            propertyList.append(propName).append(",");
        }
        // cut off the last comma
        builder.addParameter("propertylist", propertyList.substring(0, propertyList.length() - 1));
        // make the request
        SlingHttpResponse resp = this.doPost(URL_CONFIGURATION + "/" + PID, builder.build());
        // check the returned status
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(SC_MOVED_TEMPORARILY, expectedStatus));

        Header[] locationHeader = resp.getHeaders("Location");
        if (locationHeader!=null && locationHeader.length==1) {
        	return locationHeader[0].getValue().substring(URL_CONFIGURATION.length()+1);
        } else {
        	return null;
        }
    }

    /**
     * Sets properties of a config referenced by its PID. the properties to be edited are passed as
     * a map of property (name,value) pairs. The method waits until the configuration has been set.
     *
     * @param waitCount The number of maximum wait intervals of 500ms.
     *                  Between each wait interval, the method polls the backend to see if the configuration ahs been set.
     * @param PID Persistent identity string
     * @param factoryPID Factory persistent identity string or {@code null}
     * @param configProperties map of properties
     * @param expectedStatus expected response status
     * @return the pid
     * @throws ClientException if the response status does not match any of the expectedStatus
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public String editConfigurationWithWait(int waitCount, String PID, String factoryPID, Map<String, Object> configProperties,
                                            int... expectedStatus) throws ClientException, InterruptedException {
        String pid = editConfiguration(PID, factoryPID, configProperties, expectedStatus);
        getConfigurationWithWait(waitCount, pid);
        return pid;
    }

    /**
     * Delete the config referenced by the PID
     *
     * @param pid pid
     * @param expectedStatus expected response status
     * @return the sling response
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public SlingHttpResponse deleteConfiguration(String pid, int... expectedStatus) throws ClientException {
        FormEntityBuilder builder = FormEntityBuilder.create();
        builder.addParameter("apply", "1");
        builder.addParameter("delete", "1");
        // make the request
        SlingHttpResponse resp = this.doPost(URL_CONFIGURATION + "/" + pid, builder.build());
        // check the returned status
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(200, expectedStatus));
        return resp;
    }

    //
    // Bundles
    //

    /**
     * Uninstall a bundle
     * @param symbolicName
     * @return the sling response
     * @throws ClientException
     */
    public SlingHttpResponse uninstallBundle(String symbolicName) throws ClientException {
        final long bundleId = getBundleId(symbolicName);
        LOG.info("Uninstalling bundle {} with bundleId {}", symbolicName, bundleId);
        FormEntityBuilder builder = FormEntityBuilder.create();
        builder.addParameter("action", "uninstall");
        return this.doPost(getBundlePath(symbolicName), builder.build(), 200);
    }

    /**
     * Install a bundle using the Felix webconsole HTTP interface
     * @param f the bundle file
     * @param startBundle whether to start the bundle or not
     * @return the sling response
     * @throws ClientException
     */
    public SlingHttpResponse installBundle(File f, boolean startBundle) throws ClientException {
        return installBundle(f, startBundle, 0);
    }

    /**
     * Install a bundle using the Felix webconsole HTTP interface, with a specific start level
     * @param f
     * @param startBundle
     * @param startLevel
     * @return the sling response
     * @throws ClientException
     */
    public SlingHttpResponse installBundle(File f, boolean startBundle, int startLevel) throws ClientException {
        // Setup request for Felix Webconsole bundle install
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody("action", "install")
                .addBinaryBody("bundlefile", f);
        if (startBundle) {
            builder.addTextBody("bundlestart", "true");
        }
        if (startLevel > 0) {
            builder.addTextBody("bundlestartlevel", String.valueOf(startLevel));
            LOG.info("Installing bundle {} at start level {}", f.getName(), startLevel);
        } else {
            LOG.info("Installing bundle {} at default start level", f.getName());
        }

        return this.doPost(URL_BUNDLES, builder.build(), 302);

    }

    /**
     * Install a bundle using the Felix webconsole HTTP interface and wait for it to be installed
     * @param f the bundle file
     * @param startBundle whether to start the bundle or not
     * @param startLevel the start level of the bundle. negative values mean default start level
     * @param waitTime how long to wait between retries of checking the bundle
     * @param retries how many times to check for the bundle to be installed, until giving up
     * @return true if the bundle was successfully installed, false otherwise
     * @throws ClientException
     */
    public boolean installBundleWithRetry(File f, boolean startBundle, int startLevel, int waitTime, int retries)
            throws ClientException, InterruptedException {
        installBundle(f, startBundle, startLevel);
        try {
            return this.checkBundleInstalled(OsgiConsoleClient.getBundleSymbolicName(f), waitTime, retries);
        } catch (IOException e) {
            throw new ClientException("Cannot get bundle symbolic name", e);
        }
    }

    /**
     * Check that specified bundle is installed and retries every {{waitTime}} milliseconds, until the
     * bundle is installed or the number of retries was reached
     * @param symbolicName the name of the bundle
     * @param waitTime How many milliseconds to wait between retries
     * @param retries the number of retries
     * @return true if the bundle was installed until the retries stop, false otherwise
     * @throws InterruptedException
     */
    public boolean checkBundleInstalled(String symbolicName, int waitTime, int retries) throws InterruptedException {
        final String path = getBundlePath(symbolicName, ".json");
        return new PathPoller(this, path, waitTime, retries).callAndWait();
    }

    /**
     * Get the id of the bundle
     * @param symbolicName
     * @return
     * @throws Exception
     */
    public long getBundleId(String symbolicName) throws ClientException {
        final JSONObject bundle = getBundleData(symbolicName);
        try {
            return bundle.getLong(JSON_KEY_ID);
        } catch (JSONException e) {
            throw new ClientException("Cannot get id from json", e);
        }
    }

    /**
     * Get the version of the bundle
     * @param symbolicName
     * @return
     * @throws ClientException
     */
    public String getBundleVersion(String symbolicName) throws ClientException {
        final JSONObject bundle = getBundleData(symbolicName);
        try {
            return bundle.getString(JSON_KEY_VERSION);
        } catch (JSONException e) {
            throw new ClientException("Cannot get version from json", e);
        }
    }

    /**
     * Get the state of the bundle
     * @param symbolicName
     * @return
     * @throws Exception
     */
    public String getBundleState(String symbolicName) throws ClientException {
        final JSONObject bundle = getBundleData(symbolicName);
        try {
            return bundle.getString(JSON_KEY_STATE);
        } catch (JSONException e) {
            throw new ClientException("Cannot get state from json", e);
        }
    }

    /**
     * Starts a bundle
     * @param symbolicName the name of the bundle
     * @throws ClientException
     */
    public void startBundle(String symbolicName) throws ClientException {
        // To start the bundle we POST action=start to its URL
        final String path = getBundlePath(symbolicName);
        LOG.info("Starting bundle {} via {}", symbolicName, path);
        this.doPost(path, FormEntityBuilder.create().addParameter("action", "start").build(), SC_OK);
    }

    /**
     * Starts a bundle and waits for it to be started
     * @param symbolicName the name of the bundle
     * @param waitTime How many milliseconds to wait between retries
     * @param retries the number of retries
     * @throws ClientException, InterruptedException
     */
    public void startBundlewithWait(String symbolicName, int waitTime, int retries)
            throws ClientException, InterruptedException {
        // start a bundle
        startBundle(symbolicName);
        // wait for it to be in the started state
        checkBundleInstalled(symbolicName, waitTime, retries);
    }

    /**
     * Calls PackageAdmin.refreshPackages to force re-wiring of all the bundles.
     * @throws ClientException
     */
    public void refreshPackages() throws ClientException {
        LOG.info("Refreshing packages.");
        FormEntityBuilder builder = FormEntityBuilder.create();
        builder.addParameter("action", "refreshPackages");
        this.doPost(URL_BUNDLES, builder.build(), 200);
    }


    //
    // private methods
    //

    private String getBundlePath(String symbolicName, String extension) {
        return getBundlePath(symbolicName) + extension;
    }

    private String getBundlePath(String symbolicName) {
        return URL_BUNDLES + "/" + symbolicName;
    }

    private JSONObject getBundleData(String symbolicName) throws ClientException {
        // This returns a data structure like
        // {"status":"Bundle information: 173 bundles in total - all 173 bundles active.","s":[173,171,2,0,0],"data":
        //  [
        //      {"id":0,"name":"System Bundle","fragment":false,"stateRaw":32,"state":"Active","version":"3.0.7","symbolicName":"org.apache.felix.framework","category":""},
        //  ]}
        final String path = getBundlePath(symbolicName, ".json");
        final String content = this.doGet(path, SC_OK).getContent();

        try {
            final JSONObject root = new JSONObject(content);

            if (!root.has(JSON_KEY_DATA)) {
                throw new ClientException(path + " does not provide '" + JSON_KEY_DATA + "' element, JSON content=" + content);
            }

            final JSONArray data = root.getJSONArray(JSON_KEY_DATA);
            if (data.length() < 1) {
                throw new ClientException(path + "." + JSON_KEY_DATA + " is empty, JSON content=" + content);
            }

            final JSONObject bundle = data.getJSONObject(0);
            if (!bundle.has(JSON_KEY_STATE)) {
                throw new ClientException(path + ".data[0].state missing, JSON content=" + content);
            }

            return bundle;
        } catch (JSONException e) {
            throw new ClientException("Cannot get json", e);
        }
    }

    //
    // static methods
    //

    /**
     * Get the symbolic name from a bundle file
     * @param bundleFile
     * @return
     * @throws IOException
     */
    public static String getBundleSymbolicName(File bundleFile) throws IOException {
        String name = null;
        final JarInputStream jis = new JarInputStream(new FileInputStream(bundleFile));
        try {
            final Manifest m = jis.getManifest();
            if (m == null) {
                throw new IOException("Manifest is null in " + bundleFile.getAbsolutePath());
            }
            name = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        } finally {
            jis.close();
        }
        return name;
    }

    /**
     * Get the version form a bundle file
     * @param bundleFile
     * @return
     * @throws IOException
     */
    public static String getBundleVersionFromFile(File bundleFile) throws IOException {
        String version = null;
        final JarInputStream jis = new JarInputStream(new FileInputStream(bundleFile));
        try {
            final Manifest m = jis.getManifest();
            if(m == null) {
                throw new IOException("Manifest is null in " + bundleFile.getAbsolutePath());
            }
            version = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        } finally {
            jis.close();
        }
        return version;
    }


    class ConfigurationPoller extends AbstractPoller {

        private final String pid;
        int[] expectedStatus;
        public Map<String, Object> config;

        public ConfigurationPoller(long waitInterval, long waitCount, String pid, int... expectedStatus) {
            super(waitInterval, waitCount);
            this.pid = pid;
            this.config = null;
            this.expectedStatus = expectedStatus;
        }

        @Override
        public boolean call() {
            try {
                config = getConfiguration(pid, expectedStatus);
            } catch (ClientException e) {
                LOG.warn("Couldn't get config " + pid, e);
            }
            return true;
        }

        @Override
        public boolean condition() {
            return null != config;
        }

        public Map<String, Object> getConfig() {
            return config;
        }
    }
}
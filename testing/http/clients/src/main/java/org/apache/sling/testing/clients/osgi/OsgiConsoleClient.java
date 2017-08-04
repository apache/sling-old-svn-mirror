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
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.HttpUtils;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.poller.PathPoller;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.codehaus.jackson.JsonNode;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
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
     * @deprecated use {@link #waitGetConfiguration(long, String, int...)}
     *
     * @param waitCount The number of maximum wait intervals of 500ms.
     *                  Between each wait interval, the method polls the backend to see if the configuration ahs been set.
     * @param pid pid
     * @param expectedStatus expected response status
     * @return the config properties
     * @throws ClientException if the response status does not match any of the expectedStatus
     * @throws InterruptedException to mark this operation as "waiting"
     */
    @Deprecated
    public Map<String, Object> getConfigurationWithWait(long waitCount, String pid, int... expectedStatus)
            throws ClientException, InterruptedException {
        ConfigurationPoller poller = new ConfigurationPoller(pid, expectedStatus);
        try {
            poller.poll(500L * waitCount, 500);
        } catch (TimeoutException e) {
            throw new ClientException("Cannot retrieve configuration.", e);
        }
        return poller.getConfig();
    }

    /**
     * Returns a map of all properties set for the config referenced by the PID, where the map keys
     * are the property names. The method waits until the configuration has been set.
     *
     * @param timeout Maximum time to wait for the configuration to be available, in ms.
     * @param pid service pid
     * @param expectedStatus expected response status
     * @return the config properties
     * @throws ClientException if the response status does not match any of the expectedStatus
     * @throws InterruptedException to mark this operation as "waiting"
     * @throws TimeoutException if the timeout was reached
     */
    public Map<String, Object> waitGetConfiguration(long timeout, String pid, int... expectedStatus)
            throws ClientException, InterruptedException, TimeoutException {

        ConfigurationPoller poller = new ConfigurationPoller(pid, expectedStatus);
        poller.poll(timeout, 500);

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
     * @deprecated use {@link #waitEditConfiguration(long, String, String, Map, int...)}
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
    @Deprecated
    public String editConfigurationWithWait(int waitCount, String PID, String factoryPID, Map<String, Object> configProperties,
                                            int... expectedStatus) throws ClientException, InterruptedException {
        String pid = editConfiguration(PID, factoryPID, configProperties, expectedStatus);
        getConfigurationWithWait(waitCount, pid);
        return pid;
    }

    /**
     * Sets properties of a config referenced by its PID. the properties to be edited are passed as
     * a map of property (name,value) pairs. The method waits until the configuration has been set.
     *
     * @param timeout Max time to wait for the configuration to be set, in ms
     * @param PID Persistent identity string
     * @param factoryPID Factory persistent identity string or {@code null}
     * @param configProperties map of properties
     * @param expectedStatus expected response status
     * @return the pid
     * @throws ClientException if the response status does not match any of the expectedStatus
     * @throws InterruptedException to mark this operation as "waiting"
     * @throws TimeoutException if the timeout was reached
     */
    public String waitEditConfiguration(long timeout, String PID, String factoryPID, Map<String, Object> configProperties,
                                        int... expectedStatus)
            throws ClientException, InterruptedException, TimeoutException {
        String pid = editConfiguration(PID, factoryPID, configProperties, expectedStatus);
        waitGetConfiguration(timeout, pid);
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
     * @param symbolicName bundle symbolic name
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
     * @param f bundle file
     * @param startBundle whether to start or just install the bundle
     * @param startLevel start level
     * @return the sling response
     * @throws ClientException if the request failed
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
     * Check that specified bundle is installed and retries every {{waitTime}} milliseconds, until the
     * bundle is installed or the number of retries was reached
     * @deprecated does not respect polling practices; use {@link #waitBundleInstalled(String, long, long)} instead
     * @param symbolicName the name of the bundle
     * @param waitTime How many milliseconds to wait between retries
     * @param retries the number of retries
     * @return true if the bundle was installed until the retries stop, false otherwise
     * @throws InterruptedException
     */
    @Deprecated
    public boolean checkBundleInstalled(String symbolicName, int waitTime, int retries) throws InterruptedException {
        final String path = getBundlePath(symbolicName, ".json");
        return new PathPoller(this, path, waitTime, retries).callAndWait();
    }

    /**
     * Install a bundle using the Felix webconsole HTTP interface and wait for it to be installed
     * @deprecated {@link #waitInstallBundle(File, boolean, int, long, long)}
     * @param f the bundle file
     * @param startBundle whether to start the bundle or not
     * @param startLevel the start level of the bundle. negative values mean default start level
     * @param waitTime how long to wait between retries of checking the bundle
     * @param retries how many times to check for the bundle to be installed, until giving up
     * @return true if the bundle was successfully installed, false otherwise
     * @throws ClientException
     */
    @Deprecated
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
     * Install a bundle using the Felix webconsole HTTP interface and wait for it to be installed
     * @param f the bundle file
     * @param startBundle whether to start the bundle or not
     * @param startLevel the start level of the bundle. negative values mean default start level
     * @param timeout how much to wait for the bundle to be installed before throwing a {@code TimeoutException}
     * @param delay time to wait between checks of the state
     * @throws ClientException
     * @throws TimeoutException if the bundle did not install before timeout was reached
     * @throws InterruptedException
     */
    public void waitInstallBundle(File f, boolean startBundle, int startLevel, long timeout, long delay)
            throws ClientException, InterruptedException, TimeoutException {

        installBundle(f, startBundle, startLevel);
        try {
            waitBundleInstalled(getBundleSymbolicName(f), timeout, delay);
        } catch (IOException e) {
            throw new ClientException("Cannot get bundle symbolic name", e);
        }
    }

    public void waitBundleInstalled(final String symbolicName, final long timeout, final long delay)
            throws TimeoutException, InterruptedException {

        final String path = getBundlePath(symbolicName);
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                return exists(path);
            }

            @Override
            protected String message() {
                return "Bundle " + symbolicName + " did not install in %1$ ms";
            }
        };

        p.poll(timeout, delay);
    }

    /**
     * Get the id of the bundle
     * @param symbolicName bundle symbolic name
     * @return the id
     * @throws ClientException if the id cannot be retrieved
     */
    public long getBundleId(String symbolicName) throws ClientException {
        final JsonNode bundle = getBundleData(symbolicName);
        final JsonNode idNode = bundle.get(JSON_KEY_ID);

        if (idNode == null) {
            throw new ClientException("Cannot get id from bundle json");
        }

        return idNode.getLongValue();
    }

    /**
     * Get the version of the bundle
     * @param symbolicName bundle symbolic name
     * @return bundle version
     * @throws ClientException
     */
    public String getBundleVersion(String symbolicName) throws ClientException {
        final JsonNode bundle = getBundleData(symbolicName);
        final JsonNode versionNode = bundle.get(JSON_KEY_VERSION);

        if (versionNode == null) {
            throw new ClientException("Cannot get version from bundle json");
        }

        return versionNode.getTextValue();
    }

    /**
     * Get the state of the bundle
     * @param symbolicName bundle symbolic name
     * @return the state of the bundle
     * @throws ClientException if the state cannot be retrieved
     */
    public String getBundleState(String symbolicName) throws ClientException {
        final JsonNode bundle = getBundleData(symbolicName);
        final JsonNode stateNode = bundle.get(JSON_KEY_STATE);

        if (stateNode == null) {
            throw new ClientException("Cannot get state from bundle json");
        }

        return stateNode.getTextValue();
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
     * Stop a bundle
     * @param symbolicName the name of the bundle
     * @throws ClientException
     */
    public void stopBundle(String symbolicName) throws ClientException {
        // To stop the bundle we POST action=stop to its URL
        final String path = getBundlePath(symbolicName);
        LOG.info("Stopping bundle {} via {}", symbolicName, path);
        this.doPost(path, FormEntityBuilder.create().addParameter("action", "stop").build(), SC_OK);
    }


    /**
     * Starts a bundle and waits for it to be started
     * @deprecated use {@link #waitStartBundle(String, long, long)}
     * @param symbolicName the name of the bundle
     * @param waitTime How many milliseconds to wait between retries
     * @param retries the number of retries
     * @throws ClientException, InterruptedException
     */
    @Deprecated
    public void startBundlewithWait(String symbolicName, int waitTime, int retries)
            throws ClientException, InterruptedException {
        // start a bundle
        startBundle(symbolicName);
        // wait for it to be in the started state
        checkBundleInstalled(symbolicName, waitTime, retries);
    }

    /**
     * Starts a bundle and waits for it to be started
     * @param symbolicName the name of the bundle
     * @param timeout max time to wait for the bundle to start, in ms
     * @param delay time to wait between status checks, in ms
     * @throws ClientException, InterruptedException, TimeoutException
     */
    public void waitStartBundle(String symbolicName, long timeout, long delay)
            throws ClientException, InterruptedException, TimeoutException {
        startBundle(symbolicName);
        // FIXME this should wait for the started state
        waitBundleInstalled(symbolicName, timeout, delay);
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

    /**
     * Returns a data structure like:
     *
     * {
     *   "status" : "Bundle information: 173 bundles in total - all 173 bundles active.",
     *   "s" : [173,171,2,0,0],
     *   "data": [{
     *     "id":0,
     *     "name":"System Bundle",
     *     "fragment":false,
     *     "stateRaw":32,
     *     "state":"Active",
     *     "version":"3.0.7",
     *     "symbolicName":"org.apache.felix.framework",
     *     "category":""
     *   }]
     * }
     */
    private JsonNode getBundleData(String symbolicName) throws ClientException {
        final String path = getBundlePath(symbolicName, ".json");
        final String content = this.doGet(path, SC_OK).getContent();
        final JsonNode root = JsonUtils.getJsonNodeFromString(content);

        if (root.get(JSON_KEY_DATA) == null) {
            throw new ClientException(path + " does not provide '" + JSON_KEY_DATA + "' element, JSON content=" + content);
        }

        Iterator<JsonNode> data = root.get(JSON_KEY_DATA).getElements();
        if (!data.hasNext()) {
            throw new ClientException(path + "." + JSON_KEY_DATA + " is empty, JSON content=" + content);
        }

        final JsonNode bundle = data.next();
        if (bundle.get(JSON_KEY_STATE) == null) {
            throw new ClientException(path + ".data[0].state missing, JSON content=" + content);
        }

        return bundle;
    }

    //
    // static methods
    //

    /**
     * Get the symbolic name from a bundle file by looking at the manifest
     * @param bundleFile bundle file
     * @return the name extracted from the manifest
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
     * Get the version form a bundle file by looking at the manifest
     * @param bundleFile bundle file
     * @return the version
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


    class ConfigurationPoller extends Polling {

        private final String pid;
        private final int[] expectedStatus;
        private Map<String, Object> config;

        public ConfigurationPoller(String pid, int... expectedStatus) {
            super();

            this.pid = pid;
            this.expectedStatus = expectedStatus;
            this.config = null;
        }

        @Override
        public Boolean call() throws Exception {
            config = getConfiguration(pid, expectedStatus);
            return config != null;
        }

        public Map<String, Object> getConfig() {
            return config;
        }
    }
}

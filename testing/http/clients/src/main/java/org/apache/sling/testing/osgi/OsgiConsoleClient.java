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

package org.apache.sling.testing.osgi;

import org.apache.http.Header;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.ClientException;
import org.apache.sling.testing.SlingClient;
import org.apache.sling.testing.SlingClientConfig;
import org.apache.sling.testing.SlingHttpResponse;
import org.apache.sling.testing.util.FormEntityBuilder;
import org.apache.sling.testing.util.HttpUtils;
import org.apache.sling.testing.util.JsonUtils;
import org.apache.sling.testing.util.poller.AbstractPoller;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * A client that wraps the Felix OSGi Web Console REST API calls.
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
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public void deleteConfiguration(String pid, int... expectedStatus) throws ClientException {
        FormEntityBuilder builder = FormEntityBuilder.create();
        builder.addParameter("apply", "1");
        builder.addParameter("delete", "1");
        // make the request
        SlingHttpResponse resp = this.doPost(URL_CONFIGURATION + "/" + pid, builder.build());
        // check the returned status         
        HttpUtils.verifyHttpStatus(resp, HttpUtils.getExpectedStatus(200, expectedStatus));
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
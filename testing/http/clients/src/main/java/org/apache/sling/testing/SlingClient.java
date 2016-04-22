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
package org.apache.sling.testing;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.sling.testing.interceptors.DelayRequestInterceptor;
import org.apache.sling.testing.util.FormEntityBuilder;
import org.apache.sling.testing.util.HttpUtils;
import org.apache.sling.testing.util.JsonUtils;
import org.apache.sling.testing.util.poller.AbstractPoller;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * <p>The Base class for all Integration Test Clients. It provides generic methods to send HTTP requests to a server. </p>
 *
 * <p>It has methods to perform simple node operations on the server like creating and deleting nodes, etc.
 * on the server using requests. </p>
 */
public class SlingClient extends AbstractSlingClient {

    public static final String DEFAULT_NODE_TYPE = "sling:OrderedFolder";

    /**
     * Constructor used by Builders and adaptTo(). <b>Should never be called directly from the code.</b>
     *
     * @param http the underlying HttpClient to be used
     * @param config sling specific configs
     * @throws ClientException if the client could not be created
     *
     * @see AbstractSlingClient#AbstractSlingClient(CloseableHttpClient, SlingClientConfig)
     */
    public SlingClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    /**
     * <p>Handy constructor easy to use in simple tests. Creates a client that uses basic authentication.</p>
     *
     * <p>For constructing clients with complex configurations, use a {@link InternalBuilder}</p>
     *
     * <p>For constructing clients with the same configuration, but a different class, use {@link #adaptTo(Class)}</p>
     *
     * @param url url of the server (including context path)
     * @param user username for basic authentication
     * @param password password for basic authentication
     * @throws ClientException never, kept for uniformity with the other constructors
     */
    public SlingClient(URI url, String user, String password) throws ClientException {
        super(Builder.create(url, user, password).buildHttpClient(), Builder.create(url, user, password).buildSlingClientConfig());
    }

    /**
     * Moves a sling path to a new location (:operation move)
     *
     * @param srcPath source path
     * @param destPath destination path
     * @param expectedStatus list of accepted status codes in response
     * @return the response
     * @throws ClientException if an error occurs during operation
     */
    public SlingHttpResponse move(String srcPath, String destPath, int... expectedStatus) throws ClientException {
        UrlEncodedFormEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "move")
                .addParameter(":dest", destPath)
                .build();

        return this.doPost(srcPath, entity, expectedStatus);
    }

    /**
     * Deletes a sling path (:operation delete)
     *
     * @param path path to be deleted
     * @param expectedStatus list of accepted status codes in response
     * @return the response
     * @throws ClientException if an error occurs during operation
     */
    public SlingHttpResponse deletePath(String path, int... expectedStatus) throws ClientException {
        HttpEntity entity = FormEntityBuilder.create().addParameter(":operation", "delete").build();

        return this.doPost(path, entity, expectedStatus);
    }

    /**
     * Recursively creates all the none existing nodes in the given path using the {@link SlingClient#createNode(String, String)} method.
     * All the created nodes will have the given node type.
     *
     * @param path the path to use for creating all the none existing nodes
     * @param nodeType the node type to use for the created nodes
     * @return the response to the creation of the leaf node
     * @throws ClientException if one of the nodes can't be created
     */
    public SlingHttpResponse createNodeRecursive(final String path, final String nodeType) throws ClientException {
        final String parentPath = getParentPath(path);
        if (!parentPath.isEmpty() && !exists(parentPath)) {
            createNodeRecursive(parentPath, nodeType);
        }

        return createNode(path, nodeType);
    }

    /**
     * Creates the node specified by a given path with the given node type.<br>
     * If the given node type is {@code null}, the node will be created with the default type: {@value DEFAULT_NODE_TYPE}.<br>
     * If the node already exists, the method will return null, with no errors.<br>
     * The method ignores trailing slashes so a path like this <i>/a/b/c///</i> is accepted and will create the <i>c</i> node if the rest of
     * the path exists.
     * 
     * @param path the path to the node to create
     * @param nodeType the type of the node to create
     * @return the sling HTTP response or null if the path already existed
     * @throws ClientException if the node can't be created
     */
    public SlingHttpResponse createNode(final String path, final String nodeType) throws ClientException {
        if (!exists(path)) {

            String nodeTypeValue = nodeType;
            if (nodeTypeValue == null) {
                nodeTypeValue = DEFAULT_NODE_TYPE;
            }

            // Use the property for creating the actual node for working around the Sling issue with dot containing node names.
            // The request will be similar with doing:
            // curl -F "nodeName/jcr:primaryType=nodeTypeValue" -u admin:admin http://localhost:8080/nodeParentPath
            final String nodeName = getNodeNameFromPath(path);
            final String nodeParentPath = getParentPath(path);
            final HttpEntity entity = FormEntityBuilder.create().addParameter(nodeName + "/jcr:primaryType", nodeTypeValue).build();
            return this.doPost(nodeParentPath, entity, SC_OK, SC_CREATED);
        } else {
            return null;
        }
    }

    /**
     * <p>Checks whether a path exists or not by making a GET request to that path with the {@code json} extension</p>
     * @param path path to be checked
     * @return true if GET response returns 200
     * @throws ClientException if the request could not be performed
     */
    public boolean exists(String path) throws ClientException {
        SlingHttpResponse response = this.doGet(path + ".json");
        final int status = response.getStatusLine().getStatusCode();
        return status == SC_OK;
    }

    /**
     * Extracts the parent path from the given String
     *
     * @param path string containing the path
     * @return the parent path if exists or empty string otherwise
     */
    protected String getParentPath(final String path) {
        // TODO define more precisely what is the parent of a folder and of a file
        final String normalizedPath = StringUtils.removeEnd(path, "/");  // remove trailing slash in case of folders
        return StringUtils.substringBeforeLast(normalizedPath, "/");
    }

    /**
     * Extracts the node from path
     *
     * @param path string containing the path
     * @return the node without parent path
     */
    protected String getNodeNameFromPath(final String path) {
        // TODO define the output for all the cases (e.g. paths with trailing slash)
        final String normalizedPath = StringUtils.removeEnd(path, "/");  // remove trailing slash in case of folders
        final int pos = normalizedPath.lastIndexOf('/');
        if (pos != -1) {
            return normalizedPath.substring(pos + 1, normalizedPath.length());
        }
        return normalizedPath;
    }

    /**
     * <p>Checks whether a path exists or not by making a GET request to that path with the {@code json extension} </p>
     * <p>It polls the server and waits until the path exists </p>
     * @param path path to be checked
     * @param waitMillis time to wait between retries
     * @param retryCount number of retries before throwing an exception
     * @throws ClientException if the path was not found
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public void waitUntilExists(final String path, final long waitMillis, int retryCount)
            throws ClientException, InterruptedException {
        AbstractPoller poller =  new AbstractPoller(waitMillis, retryCount) {
            boolean found = false;
            public boolean call() {
                try {
                    found = exists(path);
                } catch (ClientException e) {
                    // maybe log
                    found = false;
                }
                return true;
            }

            public boolean condition() {
                return found;
            }
        };

        boolean found = poller.callUntilCondition();
        if (!found) {
            throw new ClientException("path " + path + " does not exist after " + retryCount + " retries");
        }
    }

    /**
     * Sets String component property on a node.
     *
     * @param nodePath       path to the node to be edited
     * @param propName       name of the property to be edited
     * @param propValue      value of the property to be edited
     * @param expectedStatus list of expected HTTP Status to be returned, if not set, 200 is assumed.
     * @return the response object
     * @throws ClientException if something fails during the request/response cycle
     */
    public SlingHttpResponse setPropertyString(String nodePath, String propName, String propValue, int... expectedStatus)
            throws ClientException {
        // prepare the form
        HttpEntity formEntry = FormEntityBuilder.create().addParameter(propName, propValue).build();
        // send the request
        return this.doPost(nodePath, formEntry, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
    }

    /**
     * Sets a String[] component property on a node.
     *
     * @param nodePath         path to the node to be edited
     * @param propName         name of the property to be edited
     * @param propValueList    List of String values
     * @param expectedStatus   list of expected HTTP Status to be returned, if not set, 200 is assumed.
     * @return                 the response
     * @throws ClientException if something fails during the request/response cycle
     */
    public SlingHttpResponse setPropertyStringArray(String nodePath, String propName, List<String> propValueList, int... expectedStatus)
            throws ClientException {
        // prepare the form
        FormEntityBuilder formEntry = FormEntityBuilder.create();
        for (String propValue : (propValueList != null) ? propValueList : new ArrayList<String>(0)) {
            formEntry.addParameter(propName, propValue);
        }
        // send the request and return the sling response
        return this.doPost(nodePath, formEntry.build(), HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
    }

    /**
     * Sets multiple String properties on a node in a single request
     * @param nodePath path to the node to be edited
     * @param properties list of NameValue pairs with the name and value for each property. String[] properties can be defined
     *                   by adding multiple time the same property name with different values
     * @param expectedStatus list of expected HTTP Status to be returned, if not set, 200 is assumed.
     * @return the response
     * @throws ClientException if the operation could not be completed
     */
    public SlingHttpResponse setPropertiesString(String nodePath, List<NameValuePair> properties, int... expectedStatus)
            throws ClientException {
        // prepare the form
        HttpEntity formEntry = FormEntityBuilder.create().addAllParameters(properties).build();
        // send the request and return the sling response
        return this.doPost(nodePath, formEntry, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));
    }

    /**
     * Returns the JSON content of a node already mapped to a {@link org.codehaus.jackson.JsonNode}.<br>
     * Waits max 10 seconds for the node to be created.
     *
     * @param path  the path to the content node
     * @param depth the number of levels to go down the tree, -1 for infinity
     * @return a {@link org.codehaus.jackson.JsonNode} mapping to the requested content node.
     * @throws ClientException if something fails during request/response processing
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public JsonNode getJsonNode(String path, int depth) throws ClientException, InterruptedException {
        return getJsonNode(path, depth, 500, 20);
    }

    /**
     * Returns JSON format of a content node already mapped to a {@link org.codehaus.jackson.JsonNode}.
     *
     * @param path                 the path to the content node
     * @param depth                the number of levels to go down the tree, -1 for infinity
     * @param waitMillis           how long it should wait between requests
     * @param retryNumber          number of retries before throwing an exception
     * @param expectedStatus       list of allowed HTTP Status to be returned. If not set,
     *                             http status 200 (OK) is assumed.
     * @return a {@link org.codehaus.jackson.JsonNode} mapping to the requested content node.
     * @throws ClientException if something fails during request/response cycle
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public JsonNode getJsonNode(String path, int depth, final long waitMillis, final int retryNumber, int... expectedStatus)
            throws ClientException, InterruptedException {

        // check if path exist and wait if needed
        waitUntilExists(path, waitMillis, retryNumber);

        // check for infinity
        if (depth == -1) {
            path += ".infinity.json";
        } else {
            path += "." + depth + ".json";
        }

        // request the JSON for the page node
        SlingHttpResponse response = this.doGet(path);
        HttpUtils.verifyHttpStatus(response, HttpUtils.getExpectedStatus(SC_OK, expectedStatus));

        return JsonUtils.getJsonNodeFromString(response.getContent());
    }

    /**
     * Uploads a file to the repository. It creates a leaf node typed {@code nt:file}. The intermediary nodes are created with
     * type "sling:OrderedFolder" if parameter {@code createFolders} is true
     *
     * @param file           the file to be uploaded
     * @param mimeType       the MIME Type of the file
     * @param toPath         the complete path of the file in the repository including file name
     * @param createFolders  if true, all non existing parent nodes will be created using node type {@code sling:OrderedFolder}
     * @param expectedStatus list of expected HTTP Status to be returned, if not set, 201 is assumed.
     * @return               the response
     * @throws ClientException if something fails during the request/response cycle
     */
    public SlingHttpResponse upload(File file, String mimeType, String toPath, boolean createFolders, int... expectedStatus)
            throws ClientException {
        // Determine filename and parent folder, depending on whether toPath is a folder or a file
        String toFileName;
        String toFolder;
        if (toPath.endsWith("/")) {
            toFileName = file.getName();
            toFolder = toPath;
        } else {
            toFileName = getNodeNameFromPath(toPath);
            toFolder = getParentPath(toPath);
        }

        if (createFolders) {
            createNodeRecursive(toFolder, "sling:OrderedFolder");
        }

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody(toFileName, file, ContentType.create(mimeType), toFileName)
                .build();

        // return the sling response
        return this.doPost(toFolder, entity, HttpUtils.getExpectedStatus(SC_CREATED, expectedStatus));
    }

    /**
     * Creates a new Folder of type sling:OrderedFolder. Same as using {@code New Folder...} in the Site Admin.
     *
     * @param folderName     The name of the folder to be used in the URL.
     * @param folderTitle    Title of the Folder to be set in jcr:title
     * @param parentPath     The parent path where the folder gets added.
     * @param expectedStatus list of expected HTTP Status to be returned, if not set, 201 is assumed.
     * @return the response
     * @throws ClientException if something fails during the request/response cycle
     */
    public SlingHttpResponse createFolder(String folderName, String folderTitle, String parentPath, int... expectedStatus)
            throws ClientException {
        // we assume the parentPath is a folder, even though it doesn't end with a slash
        parentPath = StringUtils.appendIfMissing(parentPath, "/");
        String folderPath = parentPath + folderName;
        HttpEntity feb = FormEntityBuilder.create()
                .addParameter("./jcr:primaryType", "sling:OrderedFolder")  // set primary type for folder node
                .addParameter("./jcr:content/jcr:primaryType", "nt:unstructured")  // add jcr:content as sub node
                .addParameter("./jcr:content/jcr:title", folderTitle)  //set the title
                .build();

        // execute request and return the sling response
        return this.doPost(folderPath, feb, HttpUtils.getExpectedStatus(SC_CREATED, expectedStatus));
    }

    /**
     * Get uuid from any repository path
     *
     * @param repPath path in repository
     * @return uuid as String
     * @throws ClientException if something fails during request/response cycle
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public String getUUID(String repPath) throws ClientException, InterruptedException {
        // TODO review if this check is necessary. Maybe rewrite getJsonNode to wait only if requested
        if (!exists(repPath)) {
            return null;
        }
        JsonNode jsonNode = getJsonNode(repPath, -1);
        return getUUId(jsonNode);
    }

    /**
     * Get uuid from any repository path
     *
     * @param jsonNode {@link JsonNode} in repository
     * @return uuid as String or null if jsonNode is null or if the uuid was not found
     * @throws ClientException if something fails during request/response cycle
     */
    public String getUUId(JsonNode jsonNode) throws ClientException {
        // TODO review if this check is necessary. Maybe rewrite getJsonNode to wait only if requested
        if (jsonNode == null) {
            return null;  // node does not exist
        }

        JsonNode uuidNode = jsonNode.get("jcr:uuid");

        if (uuidNode == null) {
            return null;
        }

        return uuidNode.getValueAsText();
    }

    //
    // InternalBuilder class and builder related methods
    //

    /**
     * <p>Extensible InternalBuilder for SlingClient. Can be used by calling: {@code SlingClient.builder().create(...).build()}.
     * Between create() and build(), any number of <i>set</i> methods can be called to customize the client.<br>
     * It also exposes the underling httpClientBuilder through {@link #httpClientBuilder()} which can be used to customize the client
     * at http level.
     * </p>
     *
     * <p>The InternalBuilder is created to be easily extensible. A class, e.g. {@code MyClient extends SlingClient}, can have its own InternalBuilder.
     * This is worth creating if MyClient has fields that need to be initialized. The Skeleton of such InternalBuilder (created inside MyClient) is:
     * </p>
     * <blockquote><pre>
     * {@code
     * public static abstract class InternalBuilder<T extends MyClient> extends SlingClient.InternalBuilder<T> {
     *     private String additionalField;
     *
     *     public InternalBuilder(URI url, String user, String password) { super(url, user, password); }
     *
     *     public InternalBuilder<T> setAdditionalField(String s) { additionalField = s; }
     * }
     * }
     * </pre></blockquote>
     * <p>Besides this, two more methods need to be implemented directly inside {@code MyClient}: </p>
     * <blockquote><pre>
     * {@code
     * public static InternalBuilder<?> builder(URI url, String user, String password) {
     *     return new InternalBuilder<MyClient>(url, user, password) {
     *         {@literal @}Override
     *         public MyClient build() throws ClientException { return new MyClient(this); }
     *     };
     * }
     *
     * protected MyClient(InternalBuilder<MyClient> builder) throws ClientException {
     *   super(builder);
     *   additionalField = builder.additionalField;
     * }
     * }
     * </pre></blockquote>
     * Of course, the Clients and InternalBuilder are extensible on several levels, so MyClient.InternalBuilder can be further extended.
     *
     * @param <T> type extending SlingClient
     */
    public static abstract class InternalBuilder<T extends SlingClient> {

        private final SlingClientConfig.Builder configBuilder;

        private final HttpClientBuilder httpClientBuilder;

        protected InternalBuilder(URI url, String user, String password) {
            this.httpClientBuilder = HttpClientBuilder.create();
            this.configBuilder = SlingClientConfig.Builder.create().setUrl(url).setUser(user).setPassword(password);

            setDefaults();
        }

        public InternalBuilder<T> setUrl(URI url) {
            this.configBuilder.setUrl(url);
            return this;
        }

        public InternalBuilder<T> setUser(String user) {
            this.configBuilder.setUser(user);
            return this;
        }

        public InternalBuilder<T> setPassword(String password) {
            this.configBuilder.setPassword(password);
            return this;
        }

        public InternalBuilder<T> setCredentialsProvider(CredentialsProvider cp) {
            this.configBuilder.setCredentialsProvider(cp);
            return this;
        }

        public InternalBuilder<T> setCookieStore(CookieStore cs) {
            this.configBuilder.setCookieStore(cs);
            return this;
        }

        public HttpClientBuilder httpClientBuilder() {
            return httpClientBuilder;
        }

        public abstract T build() throws ClientException;

        protected CloseableHttpClient buildHttpClient() {
            return httpClientBuilder.build();
        }

        protected SlingClientConfig buildSlingClientConfig() {
            return configBuilder.build();
        }

        /**
         * Sets defaults to the builder.
         *
         * @return this
         */
        private InternalBuilder setDefaults() {
            httpClientBuilder.useSystemProperties();
            httpClientBuilder.setUserAgent("Java");
            // Connection
            httpClientBuilder.setMaxConnPerRoute(10);
            httpClientBuilder.setMaxConnTotal(100);
            // Interceptors
            httpClientBuilder.addInterceptorLast(new DelayRequestInterceptor(Constants.HTTP_DELAY));

            return this;
        }

        //
        // HttpClientBuilder delegating methods
        //

        public final InternalBuilder<T> addInterceptorFirst(final HttpResponseInterceptor itcp) {
            httpClientBuilder.addInterceptorFirst(itcp);
            return this;
        }

        /**
         * Adds this protocol interceptor to the tail of the protocol processing list.
         * <p>
         * Please note this value can be overridden by the {@link HttpClientBuilder#setHttpProcessor(
         * org.apache.http.protocol.HttpProcessor)} method.
         * </p>
         *
         * @param itcp the interceptor
         * @return this
         */
        public final InternalBuilder<T> addInterceptorLast(final HttpResponseInterceptor itcp) {
            httpClientBuilder.addInterceptorLast(itcp);
            return this;
        }

        /**
         * Adds this protocol interceptor to the head of the protocol processing list.
         * <p>
         * Please note this value can be overridden by the {@link HttpClientBuilder#setHttpProcessor(
         * org.apache.http.protocol.HttpProcessor)} method.
         * </p>
         *
         * @param itcp the interceptor
         * @return this
         */
        public final InternalBuilder<T> addInterceptorFirst(final HttpRequestInterceptor itcp) {
            httpClientBuilder.addInterceptorFirst(itcp);
            return this;
        }

        /**
         * Adds this protocol interceptor to the tail of the protocol processing list.
         * <p>
         * Please note this value can be overridden by the {@link HttpClientBuilder#setHttpProcessor(
         * org.apache.http.protocol.HttpProcessor)} method.
         * </p>
         *
         * @param itcp the interceptor
         * @return this
         */
        public final InternalBuilder<T> addInterceptorLast(final HttpRequestInterceptor itcp) {
            httpClientBuilder.addInterceptorLast(itcp);
            return this;
        }

        /**
         * Assigns {@link RedirectStrategy} instance.
         * <p>Please note this value can be overridden by the {@link #disableRedirectHandling()} method.</p>
         *
         * @param redirectStrategy custom redirect strategy
         * @return this
         */
        public final InternalBuilder<T> setRedirectStrategy(final RedirectStrategy redirectStrategy) {
            httpClientBuilder.setRedirectStrategy(redirectStrategy);
            return this;
        }

        /**
         * Disables automatic redirect handling.
         *
         * @return this
         */
        public final InternalBuilder<T> disableRedirectHandling() {
            httpClientBuilder.disableRedirectHandling();
            return this;
        }

    }

    public final static class Builder extends InternalBuilder<SlingClient> {

        private Builder(URI url, String user, String password) {
            super(url, user, password);
        }

        @Override
        public SlingClient build() throws ClientException {
            return new SlingClient(buildHttpClient(), buildSlingClientConfig());
        }

        public static Builder create(URI url, String user, String password) {
            return new Builder(url, user, password);
        }
    }
}

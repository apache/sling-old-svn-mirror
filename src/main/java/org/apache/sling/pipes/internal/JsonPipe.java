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
package org.apache.sling.pipes.internal;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipe outputting binding related to a json stream: either an object
 */
public class JsonPipe extends BasePipe {
    private static Logger logger = LoggerFactory.getLogger(JsonPipe.class);
    public static final String RESOURCE_TYPE = RT_PREFIX + "json";

    HttpClient client;

    JsonArray array;
    Object binding;
    int index = -1;

    public final String REMOTE_START = "http";

    public JsonPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        configureHttpClient();
    }

    /**
     * Configure http client
     */
    private void configureHttpClient(){
        HttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        manager.setParams(params);
        client = new HttpClient(manager);
        client.getParams().setAuthenticationPreemptive(false);
    }

    @Override
    public Object getOutputBinding() {
        return binding;
    }

    /**
     * Retrieve remote / expression JSON String, or null if any problem occurs
     * @return JSON serialization of the result
     */
    private String retrieveJSONString()  {
        String json = null;
        String expression = getExpr();
        if (expression.startsWith(REMOTE_START)){
            GetMethod method = null;
            HttpState httpState = new HttpState();
            InputStream responseInputStream = null;
            try {
                String url = getExpr();
                if (StringUtils.isNotBlank(url)) {
                    method = new GetMethod(url);
                    logger.debug("Executing GET {}", url);
                    int status = client.executeMethod(null, method, httpState);
                    if (status == HttpStatus.SC_OK) {
                        logger.debug("200 received, streaming content");
                        responseInputStream = method.getResponseBodyAsStream();
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(responseInputStream, writer, "utf-8");
                        json = writer.toString();
                    }
                }
            }
            catch(Exception e){
                logger.error("unable to retrieve the data", e);
            }finally{
                if (method != null) {
                    method.releaseConnection();
                }
                IOUtils.closeQuietly(responseInputStream);
            }
        } else {
            //other try: given expression *is* json
            json = expression;
        }
        return json;
    }


    /**
     * in case there is no successful retrieval of some JSON data, we cut the pipe here
     * @return input resource of the pipe, can be reouputed N times in case output json binding is an array of
     * N element (output binding would be here each time the Nth element of the array)
     */
    public Iterator<Resource> getOutput() {
        Iterator<Resource> output = EMPTY_ITERATOR;
        binding = null;
        String jsonString = retrieveJSONString();
        if (StringUtils.isNotBlank(jsonString)){
            try {
                JsonStructure json; 
                try {
                    json = JsonUtil.parse(jsonString);
                } catch (JsonException ex) {
                    json = null;
                }
                if (json == null) {
                    binding = jsonString.trim();
                    output = super.getOutput();
                } 
                else if (json.getValueType() != ValueType.ARRAY) {
                    binding = JsonUtil.unbox(json);
                    output = super.getOutput();
                }
                else {
                    binding = array = (JsonArray) json;
                    index = 0;
                    output = new Iterator<Resource>() {
                        @Override
                        public boolean hasNext() {
                            return index < array.size();
                        }

                        @Override
                        public Resource next() {
                            try {
                                binding = JsonUtil.unbox(array.get(index));
                            } catch(Exception e){
                                logger.error("Unable to retrieve {}nth item of jsonarray", index, e);
                            }
                            index++;
                            return getInput();
                        }
                    };
                } 
            } catch (JsonException e) {
                logger.error("unable to parse JSON {} ", jsonString, e);
            }
        }
        return output;
    }
}

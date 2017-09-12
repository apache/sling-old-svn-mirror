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
package org.apache.sling.pipes;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Input Stream based pipe, coming from web, from request, resource tree, web
 */
public abstract class AbstractInputStreamPipe extends BasePipe {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractInputStreamPipe.class);

    public final String REMOTE_START = "http";

    protected final Pattern VALID_PATH = Pattern.compile("/([\\w\\d]+/)+[\\w\\d]+");

    public static final Object BINDING_IS = "org.apache.sling.pipes.RequestInputStream";

    HttpClient client;

    protected Object binding;

    GetMethod method = null;

    InputStream is;

    public AbstractInputStreamPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        configureHttpClient();
        binding = null;
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

    InputStream getInputStream() throws IOException {
        String expr = getExpr();
        if (expr.startsWith(REMOTE_START)) {
            //first look at
            HttpState httpState = new HttpState();
            String url = getExpr();
            if (StringUtils.isNotBlank(url)) {
                method = new GetMethod(url);
                LOGGER.debug("Executing GET {}", url);
                int status = client.executeMethod(null, method, httpState);
                if (status == HttpStatus.SC_OK) {
                    LOGGER.debug("200 received, streaming content");
                    return method.getResponseBodyAsStream();
                }
            }
        } else if (VALID_PATH.matcher(expr).find()
                && resolver.getResource(expr) != null
                && resolver.getResource(expr).adaptTo(File.class) != null) {
            return new FileInputStream(resolver.getResource(expr).adaptTo(File.class));
        } else if (getBindings().getBindings().get(BINDING_IS) != null){
            return (InputStream)getBindings().getBindings().get(BINDING_IS);
        }
        return new ByteArrayInputStream(expr.getBytes(StandardCharsets.UTF_8));
    }


    @Override
    public Object getOutputBinding() {
        return binding;
    }

    abstract public Iterator<Resource> getOutput(InputStream inputStream);

    @Override
    public Iterator<Resource> getOutput() {
        try {
            is = getInputStream();
            return getOutput(is);
        } catch (Exception e){
            LOGGER.error("unable to fecth input stream", e);
        } finally {
            IOUtils.closeQuietly(is);
            if (method != null){
                method.releaseConnection();
            }
        }
        return EMPTY_ITERATOR;
    }
}

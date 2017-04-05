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
package org.apache.sling.ide.impl.resource.transport;

import java.io.InputStreamReader;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

class ListChildrenCommand extends AbstractCommand<ResourceProxy> {

    ListChildrenCommand(RepositoryInfo repositoryInfo, HttpClient httpClient, String relativePath) {
        super(repositoryInfo, httpClient, relativePath);
    }

    
    private static final class ResourceWithPrimaryType {
        @SerializedName(Repository.JCR_PRIMARY_TYPE)
        private String primaryType;
        
        String getPrimaryType() {
            return primaryType;
        }
    }
    @Override
    public Result<ResourceProxy> execute() {
        GetMethod get = new GetMethod(getPath());
    	try{
    		httpClient.getParams().setAuthenticationPreemptive(true);
    	    Credentials defaultcreds = new UsernamePasswordCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword());
    	    httpClient.getState().setCredentials(new AuthScope(repositoryInfo.getHost(),repositoryInfo.getPort(), AuthScope.ANY_REALM), defaultcreds);
    		int responseStatus=httpClient.executeMethod(get);

    		//TODO change responseAsString with something like
    		//return EncodingUtil.getString(rawdata, m.getResponseCharSet());
            if (!isSuccessStatus(responseStatus))
                return failureResultForStatusCode(responseStatus);

            ResourceProxy resource = new ResourceProxy(path);
            Gson gson = new Gson();
            try (JsonReader jsonReader = new JsonReader(
                    new InputStreamReader(get.getResponseBodyAsStream(), get.getResponseCharSet()))) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    JsonToken token = jsonReader.peek();
                    if (token == JsonToken.BEGIN_OBJECT) {
                        ResourceProxy child = new ResourceProxy(PathUtil.join(path, name));
                        ResourceWithPrimaryType resourceWithPrimaryType = gson.fromJson(jsonReader, ResourceWithPrimaryType.class);
                        // evaluate its jcr:primaryType as well!
                        child.addProperty(Repository.JCR_PRIMARY_TYPE, resourceWithPrimaryType.getPrimaryType());
                        resource.addChild(child);
                    } else if (token == JsonToken.STRING) {
                        if (Repository.JCR_PRIMARY_TYPE.equals(name)) {
                            String primaryType = jsonReader.nextString();
                            if (primaryType != null) { // TODO - needed?
                                resource.addProperty(Repository.JCR_PRIMARY_TYPE, primaryType);
                            }
                        }
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            }
            return AbstractResult.success(resource);
    	} catch (Exception e) {
    		return AbstractResult.failure(new RepositoryException(e));
    	}finally{
    		get.releaseConnection();
    	}
    }

    @Override
    public String toString() {
    	
        return String.format("%8s %s", "LISTCH", path);
    }
}
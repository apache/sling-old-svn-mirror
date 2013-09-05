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


import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.json.JSONArray;
import org.json.JSONObject;

class GetNodeContentCommand extends AbstractCommand<ResourceProxy> {

    GetNodeContentCommand(RepositoryInfo repositoryInfo, HttpClient httpClient, String relativePath) {
        super(repositoryInfo, httpClient, relativePath);
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
    		// return EncodingUtil.getString(rawdata, m.getResponseCharSet());
            if (!isSuccessStatus(responseStatus))
                return failureResultForStatusCode(responseStatus);

            JSONObject result = new JSONObject(get.getResponseBodyAsString());

            ResourceProxy resource = new ResourceProxy(path);
            JSONArray names = result.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                Object object = result.get(name);
                if (object instanceof String) {
                    resource.addProperty(name, object);
                } else {
                    System.out.println("Property '" + name + "' of type '" + object.getClass().getName()
                            + " is not handled");
                }
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
    	
        return String.format("%8s %s", "GETCONT", path);
    }
}
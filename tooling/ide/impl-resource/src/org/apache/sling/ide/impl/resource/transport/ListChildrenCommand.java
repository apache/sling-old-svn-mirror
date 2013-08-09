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

import java.util.Iterator;

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
import org.json.JSONObject;

class ListChildrenCommand extends AbstractCommand<ResourceProxy> {

    ListChildrenCommand(RepositoryInfo repositoryInfo, HttpClient httpClient, String relativePath) {
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
    		//return EncodingUtil.getString(rawdata, m.getResponseCharSet());
            if (!isSuccessStatus(responseStatus))
                return failureResultForStatusCode(responseStatus);

            ResourceProxy resource = new ResourceProxy(path);

            JSONObject json = new JSONObject(get.getResponseBodyAsString());
            String primaryType = json.optString(Repository.JCR_PRIMARY_TYPE);
            if (primaryType != null) { // TODO - needed?
                resource.addProperty(Repository.JCR_PRIMARY_TYPE, primaryType);
            }

            // TODO - populate all properties

            for (Iterator<?> keyIterator = json.keys(); keyIterator.hasNext();) {

                String key = (String) keyIterator.next();
                JSONObject value = json.optJSONObject(key);
                if (value != null) {
                    ResourceProxy child = new ResourceProxy(PathUtil.join(path, key));
                    child.addProperty(Repository.JCR_PRIMARY_TYPE, value.optString(Repository.JCR_PRIMARY_TYPE));
                    resource.addChild(child);
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
    	
        return String.format("%8s %s", "LISTCH", path);
    }
}
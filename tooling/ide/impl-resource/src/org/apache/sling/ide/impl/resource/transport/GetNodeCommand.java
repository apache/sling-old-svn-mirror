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
import org.apache.sling.ide.transport.Result;

class GetNodeCommand extends AbstractCommand<byte[]> {
    GetNodeCommand(RepositoryInfo repositoryInfo, HttpClient httpClient, String relativePath) {
        super(repositoryInfo, httpClient, relativePath);
    }

    @Override
    public Result<byte[]> execute() {
    	
        GetMethod get = new GetMethod(getPath());
    	
    	try{
    		httpClient.getParams().setAuthenticationPreemptive(true);
    	    Credentials defaultcreds = new UsernamePasswordCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword());
    	    //TODO
    	    httpClient.getState().setCredentials(new AuthScope(repositoryInfo.getHost(),repositoryInfo.getPort(), AuthScope.ANY_REALM), defaultcreds);
    		int responseStatus=httpClient.executeMethod(get);
    		
    		if ( isSuccessStatus(responseStatus) )
    			return AbstractResult.success(get.getResponseBody());
    		
    		return failureResultForStatusCode(responseStatus);
    	} catch (Exception e) {
    		return AbstractResult.failure(new RepositoryException(e));
    	}finally{
    		get.releaseConnection();
    	}
    }

    @Override
    public String toString() {
    	
    	return String.format("%8s %s", "GETNODE", path);
    }
}
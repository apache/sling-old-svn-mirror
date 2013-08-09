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

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.Result;

public class AddNodeCommand extends AbstractCommand<Void> {
    private final FileInfo fileInfo;

    public AddNodeCommand(FileInfo fileInfo, RepositoryInfo repositoryInfo, HttpClient httpClient) {
        super(repositoryInfo, httpClient, fileInfo.getRelativeLocation());
        this.fileInfo = fileInfo;
    }

    @Override
    public Result<Void> execute() {
        PostMethod post = new PostMethod(getPath());
    	try{
    		File f=new File(fileInfo.getLocation());
            if (f.isFile()) {
                Part[] parts = { new FilePart(fileInfo.getName(), f) };
                post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
            }
    		httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(repositoryInfo.getUsername(),repositoryInfo.getPassword()));
    		httpClient.getParams().setAuthenticationPreemptive(true);
            int responseStatus = httpClient.executeMethod(post);
    		
    		return resultForResponseStatus(responseStatus);
    			
    	} catch(Exception e){
    		return AbstractResult.failure(new RepositoryException(e));
    	}finally{
    		post.releaseConnection();
    	}
    }

    @Override
    public String toString() {
    	
    	return String.format("%8s %s", "ADD", fileInfo.getRelativeLocation() + "/" + fileInfo.getName());
    }
}
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
package org.apache.sling.slingclipse.http.impl;

import java.io.File;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.sling.slingclipse.api.Command;
import org.apache.sling.slingclipse.api.FileInfo;
import org.apache.sling.slingclipse.api.RepositoryException;
import org.apache.sling.slingclipse.api.ResponseType;
import org.apache.sling.slingclipse.api.Result;

public class RepositoryImpl extends AbstractRepository{
	
	private final HttpClient httpClient = new HttpClient();	

	/* (non-Javadoc)
	 * @see org.apache.sling.slingclipse.api.Repository#newAddNodeCommand(org.apache.sling.slingclipse.api.FileInfo)
	 */
	@Override
	public Command<Void> newAddNodeCommand(final FileInfo fileInfo) {
		return new Command<Void>() {
			@Override
			public Result<Void> execute() {
                PostMethod post = new PostMethod(createFullPath(fileInfo.getRelativeLocation()));
				try{
					File f=new File(fileInfo.getLocation());
                    if (f.isFile()) {
                        Part[] parts = { new FilePart(fileInfo.getName(), f) };
                        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
                    }
					httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(repositoryInfo.getUsername(),repositoryInfo.getPassword()));
					httpClient.getParams().setAuthenticationPreemptive(true);
					int responseStatus=httpClient.executeMethod(post);
					
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
		};
	}

	private Result<Void> resultForResponseStatus(int responseStatus) {
		if ( isSuccessStatus(responseStatus) )
			return AbstractResult.success(null);
		
		return failureResultForStatusCode(responseStatus);
	}

	private <T> Result<T> failureResultForStatusCode(int responseStatus) {
		return AbstractResult.failure(new RepositoryException("Repository has returned status code " + responseStatus));
	}

	private boolean isSuccessStatus(int responseStatus) {
		
		// TODO - consider all 2xx and possibly 3xx as success?
		
		return responseStatus == 200 /* OK */ || responseStatus == 201 /* CREATED */;
	}

	@Override
	public Command<Void> newDeleteNodeCommand(final FileInfo fileInfo) {
		return new Command<Void>() {
			@Override
			public Result<Void> execute() {
                PostMethod post = new PostMethod(createFullPath(fileInfo.getRelativeLocation() + "/"
                        + fileInfo.getName()));
				try{
					Part[] parts ={new StringPart(":operation", "delete")};
					post.setRequestEntity(new MultipartRequestEntity(parts,post.getParams()));
					httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(repositoryInfo.getUsername(),repositoryInfo.getPassword()));
					httpClient.getParams().setAuthenticationPreemptive(true);
					int responseStatus=httpClient.executeMethod(post);
					
					return resultForResponseStatus(responseStatus);
				} catch(Exception e){
					return AbstractResult.failure(new RepositoryException(e));
				}finally{
					post.releaseConnection();
				}
			}
			
			@Override
			public String toString() {
				return String.format("%8s %s", "DELETE", fileInfo.getRelativeLocation() + "/" + fileInfo.getName());
			}
		};
	}
	
	@Override
	public Command<String> newListChildrenNodeCommand(final String path,final ResponseType responseType) {
		return new Command<String>() {
			@Override
			public Result<String> execute() {
				//TODO handle the response type
                GetMethod get = new GetMethod(createFullPath(path + ".1.json"));
				try{
					httpClient.getParams().setAuthenticationPreemptive(true);
				    Credentials defaultcreds = new UsernamePasswordCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword());
				    //TODO
				    httpClient.getState().setCredentials(new AuthScope(repositoryInfo.getHost(),repositoryInfo.getPort(), AuthScope.ANY_REALM), defaultcreds);
					int responseStatus=httpClient.executeMethod(get);

					//TODO change responseAsString with something like
					//return EncodingUtil.getString(rawdata, m.getResponseCharSet());
					if ( isSuccessStatus(responseStatus) )
						return AbstractResult.success(get.getResponseBodyAsString());
					
					return failureResultForStatusCode(responseStatus);
				} catch (Exception e) {
					return AbstractResult.failure(new RepositoryException(e));
				}finally{
					get.releaseConnection();
				}
			}
			
			@Override
			public String toString() {
				
				return String.format("%8s %s (%s)", "LISTCH", path, responseType);
			}
		};
	}

	@Override
	public Command<byte[]> newGetNodeCommand(final String path) {
		
		return new Command<byte[]>() {
			@Override
			public Result<byte[]> execute() {
				
                GetMethod get = new GetMethod(createFullPath(path));
				
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
		};
	}
	
    private String createFullPath(String relativePath) {

        boolean repoUrlHasTrailingSlash = repositoryInfo.getUrl().endsWith("/");
        boolean relativePathHasLeadingSlash = !relativePath.isEmpty() && relativePath.charAt(0) == '/';

        if (repoUrlHasTrailingSlash ^ relativePathHasLeadingSlash)
            return repositoryInfo.getUrl() + relativePath;
        if (!repoUrlHasTrailingSlash && !relativePathHasLeadingSlash)
            return repositoryInfo.getUrl() + '/' + relativePath;
        if (repoUrlHasTrailingSlash && relativePathHasLeadingSlash)
            return repositoryInfo.getUrl() + relativePath.substring(1);

        throw new AssertionError("unreachable");
    }

	@Override
	public Command<String> newGetNodeContentCommand(final String path, final ResponseType responseType) {
		return new Command<String>() {
			@Override
			public Result<String> execute() {
				//TODO handle the response type
                GetMethod get = new GetMethod(createFullPath(path + ".json"));
				try{
					httpClient.getParams().setAuthenticationPreemptive(true);
				    Credentials defaultcreds = new UsernamePasswordCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword());
				    httpClient.getState().setCredentials(new AuthScope(repositoryInfo.getHost(),repositoryInfo.getPort(), AuthScope.ANY_REALM), defaultcreds); 
					int responseStatus=httpClient.executeMethod(get);
					//TODO change responseAsString with something like
					// return EncodingUtil.getString(rawdata, m.getResponseCharSet());
					if ( isSuccessStatus(responseStatus) )
						return AbstractResult.success(get.getResponseBodyAsString()); 
					
					return failureResultForStatusCode(responseStatus);
				} catch (Exception e) {
					return AbstractResult.failure(new RepositoryException(e));
				}finally{
					get.releaseConnection();
				}
			}
			
			@Override
			public String toString() {
				
				return String.format("%8s %s (%s)", "GETCONT", path, responseType);
			}
		};
	}
	
	@Override
	public Command<Void> newUpdateContentNodeCommand(final FileInfo fileInfo, final Map<String, String> properties) {
		
		return new Command<Void>() {
			@Override
			public Result<Void> execute() {
                PostMethod post = new PostMethod(createFullPath(fileInfo.getRelativeLocation()));
				try{
					Part[] parts = new Part[properties.size()];
					int counter=0;
					for (Map.Entry <String,String> proerty:properties.entrySet()) {
						parts[counter]=new StringPart(proerty.getKey(), proerty.getValue());
						counter++;
					}
					post.setRequestEntity(new MultipartRequestEntity(parts,post.getParams()));
					httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(repositoryInfo.getUsername(),repositoryInfo.getPassword()));
					httpClient.getParams().setAuthenticationPreemptive(true);
					int responseStatus=httpClient.executeMethod(post);
					
					return resultForResponseStatus(responseStatus);
				} catch(Exception e){
					return AbstractResult.failure(new RepositoryException(e));
				}finally{
					post.releaseConnection();
				}
			}
			
			@Override
			public String toString() {
				
				return String.format("%8s %s", "UPDATE", fileInfo.getRelativeLocation() + "/" + fileInfo.getName());
			}
		};
	}
 
}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.apache.sling.ide.impl.resource.util.Tracer;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ProtectedNodes;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class RepositoryImpl extends AbstractRepository{
	
    private final HttpClient httpClient = new HttpClient();
    private Tracer tracer;

	/* (non-Javadoc)
	 * @see org.apache.sling.slingclipse.api.Repository#newAddNodeCommand(org.apache.sling.slingclipse.api.FileInfo)
	 */
	@Override
	public Command<Void> newAddNodeCommand(final FileInfo fileInfo) {
        return wrap(new Command<Void>() {
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
        });
	}

    private <T> Command<T> wrap(Command<T> command) {

        return new TracingCommand<T>(command, tracer);
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
        return wrap(new Command<Void>() {
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
        });
	}
	
	@Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final String path) {
        return wrap(new Command<ResourceProxy>() {
			@Override
            public Result<ResourceProxy> execute() {
                GetMethod get = new GetMethod(createFullPath(path + ".1.json"));
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
        });
	}

	@Override
	public Command<byte[]> newGetNodeCommand(final String path) {
		
        return wrap(new Command<byte[]>() {
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
        });
	}
	
    private String createFullPath(String relativePath) {

        return PathUtil.join(repositoryInfo.getUrl(), relativePath);
    }

	@Override
    public Command<Map<String, Object>> newGetNodeContentCommand(final String path) {
        return wrap(new Command<Map<String, Object>>() {
			@Override
            public Result<Map<String, Object>> execute() {
				//TODO handle the response type
                GetMethod get = new GetMethod(createFullPath(path + ".json"));
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

                    Map<String, Object> properties = new HashMap<String, Object>();
                    JSONArray names = result.names();
                    for (int i = 0; i < names.length(); i++) {
                        String name = names.getString(i);
                        Object object = result.get(name);
                        if (object instanceof String) {
                            properties.put(name, object);
                        } else {
                            System.out.println("Property '" + name + "' of type '" + object.getClass().getName()
                                    + " is not handled");
                        }
                    }

                    return AbstractResult.success(properties);
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
        });
	}
	
	@Override
    public Command<Void> newUpdateContentNodeCommand(final FileInfo fileInfo, final Map<String, Object> properties) {
		
        return wrap(new Command<Void>() {
			@Override
			public Result<Void> execute() {
                PostMethod post = new PostMethod(createFullPath(fileInfo.getRelativeLocation()));
				try{
                    List<Part> parts = new ArrayList<Part>();
                    for (Map.Entry<String, Object> property : properties.entrySet()) {
                        if (ProtectedNodes.exists(property.getKey())) {
                            continue;
                        }

                        Object propValue = property.getValue();

                        if (propValue instanceof String) {
                            parts.add(new StringPart(property.getKey(), (String) propValue));
                        } else if (property != null) {
                            // TODO handle multi-valued properties
                            System.err.println("Unable to handle property " + property.getKey() + " of type "
                                    + property.getValue().getClass());
                        }
					}
                    post.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), post
                            .getParams()));
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
        });
	}

    public void bindTracer(Tracer tracer) {

        this.tracer = tracer;
    }

    public void unbindTracer(Tracer tracer) {

        this.tracer = null;
    }

    static class TracingCommand<T> implements Command<T> {

        private final Command<T> command;
        private final Tracer tracer;

        public TracingCommand(Command<T> command, Tracer tracer) {
            this.command = command;
            this.tracer = tracer;
        }

        @Override
        public Result<T> execute() {

            Result<T> result = command.execute();

            if (tracer != null)
                tracer.trace("{0} -> {1}", command, result.toString());

            return result;
        }

    }
}

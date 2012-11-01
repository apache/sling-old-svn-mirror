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
package org.apache.sling.slingclipse.api;

public interface Repository {
	
	public static String JCR_PRIMARY_TYPE= "jcr:primaryType";
	public static String NT_FILE= "nt:file";
	
	//TODO change with properties
	public void setRepositoryInfo(RepositoryInfo repositoryInfo);

	public void addNode(FileInfo fileInfo);
	
	public void deleteNode(FileInfo fileInfo);
 
	public String listChildrenNode(String path,ResponseType responseType);
 	
	public String getNodeContent(String path,ResponseType responseType);

	public byte[] getNode(String path);
}

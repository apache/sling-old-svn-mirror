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
package org.apache.sling.samples.webloader;

/** Gets documents from the Web via a Google query, and stores them into the
 *  repository. The service interface is designed to be easy to use from Sling
 *  scripts.
 */  
public interface Webloader {
    /** Create a new job that loads documents in the repository, and start
     *  it immediately 
     *  @return the job ID
     *  @param webQuery used to Google for documents to retrieve
     *  @param storagePath documents are stored under this path in the repository
     *  @param fileExtensions comma-separated list of extensions , each one 
     *      is passed in turn to Google as a "filetype:" search option
     *  @param maxDocsToRetrieve up to this many documents are stored
     *  @param maxDocSizeInKb documents over this size are ignored, to speed up the process
     */
    String createJob(String webQuery, String storagePath, 
            String fileExtensions, int maxDocsToRetrieve, int maxDocSizeInKb);
    
    /** Get the status of a job given its ID
     *  @return null if the job doesn't exist
     */
    WebloaderJobStatus getJobStatus(String jobId);
}

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
package org.apache.sling.samples.webloader.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.samples.webloader.Webloader;
import org.apache.sling.samples.webloader.WebloaderJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Webloader implementation, manages WebloaderJobs
 *
 */
@Component(immediate=true)
@Service
@Property(name="service.description", value="Sling Webloader service")
public class WebLoaderImpl implements Webloader {

    private static final Logger log = LoggerFactory.getLogger(WebLoaderImpl.class);

    private final Map<String, WebloaderJob> jobs = new HashMap<String, WebloaderJob>();

    @Reference
    private SlingRepository repository;

    @Reference
    private MimeTypeService mimeTypeService;

    /** @inheritDoc */
    public String createJob(String webQuery, String storagePath, String
            fileExtensions, int maxDocsToRetrieve, int maxDocSizeInKb) {
        deleteFinishedJobs();
        final WebloaderJob j = new WebloaderJob(repository, mimeTypeService,
                webQuery, storagePath, fileExtensions, maxDocsToRetrieve, maxDocSizeInKb);
        synchronized (jobs) {
            jobs.put(j.getJobId(), j);
        }
        log.info("Created job {}", j);
        return j.getJobId();
    }

    /** @inheritDoc */
    public WebloaderJobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }

    /** Remove finished jobs from our list of jobs */
    protected void deleteFinishedJobs() {
        final List<WebloaderJob> toDelete = new LinkedList<WebloaderJob>();
        for(WebloaderJob j : jobs.values()) {
            if(!j.isRunning()) {
                toDelete.add(j);
            }
        }

        synchronized (jobs) {
            for(WebloaderJob j : toDelete) {
                jobs.remove(j.getJobId());
            }
        }
    }

}

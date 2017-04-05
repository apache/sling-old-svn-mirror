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
package org.apache.sling.servlets.post.impl.helper;

import java.util.Calendar;
import java.util.Iterator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ChunkCleanUpTask</code> implements a job run at regular intervals
 * to find incomplete chunk uploads and remove them from the repository to
 * prevent littering the repository with incomplete chunks.
 * <p>
 * This task is configured with OSGi configuration for the PID
 * <code>org.apache.sling.servlets.post.impl.helper.ChunkCleanUpTask</code> with
 * property <code>scheduler.expression</code> being the schedule to execute the
 * task. The schedule is a cron job expression as described at <a
 * href="http://www.docjar.com/docs/api/org/quartz/CronTrigger.html">Cron
 * Trigger</a> with the default value configured to run twice a day at 0h41m31s
 * and 12h4131s.
 * <p>
 * The property <code>chunk.cleanup.age</code> specifies chunk's age in minutes
 * before it is considered for clean up.
 * <p>
 * Currently the cleanup tasks connects as the administrative user to the
 * default workspace assuming users are stored in that workspace and the
 * administrative user has full access.
 */
@Component(service = Runnable.class,
    property = {
           "service.description=Periodic Chunk Cleanup Job",
           "service.vendor=The Apache Software Foundation"
    })
@Designate(ocd = ChunkCleanUpTask.Config.class)
public class ChunkCleanUpTask implements Runnable {

    @ObjectClassDefinition(name = "Apache Sling Post Chunk Upload : Cleanup Task",
            description = "Task to regularly purge incomplete chunks from the repository")
    public @interface Config {

        @AttributeDefinition(name = "Schedule", description = "Cron expression scheudling this job. Default is hourly 17m23s after the hour. "
                + "See http://www.docjar.com/docs/api/org/quartz/CronTrigger.html for a description "
                + "of the format for this value.")
        String scheduler_expression() default "31 41 0/12 * * ?";

        @AttributeDefinition(name = "scheduler.concurrent",
                description = "Allow Chunk Cleanup Task to run concurrently (default: false).")
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(name = "Cleanup Age",
                description = "The chunk's age in minutes before it is considered for clean up.")
        int chunk_cleanup_age() default 360;
    }

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory rrFactory;

    private SlingFileUploadHandler uploadhandler = new SlingFileUploadHandler();

    /**
     * Clean up age criterion in millisec.
     */
    private long chunkCleanUpAge;

    /**
     * Executes the job. Is called for each triggered schedule point.
     */
    @Override
    public void run() {
        log.debug("ChunkCleanUpTask: Starting cleanup");
        cleanup();
    }

    /**
     * This method deletes chunks which are {@link #isEligibleForCleanUp(Resource)}
     * for cleanup. It queries all
     * {@link SlingPostConstants#NT_SLING_CHUNK_MIXIN} nodes and filter nodes
     * which are {@link #isEligibleForCleanUp(Resource)} for cleanup. It then
     * deletes old chunks upload.
     */
    private void cleanup() {

        long start = System.currentTimeMillis();

        int numCleaned = 0;
        int numLive = 0;

        ResourceResolver admin = null;
        try {
            admin = rrFactory.getAdministrativeResourceResolver(null);

            final Iterator<Resource> rsrcIter = admin.findResources(
                "SELECT * FROM [sling:chunks] ", "sql");
            while (rsrcIter.hasNext()) {
                final Resource rsrc = rsrcIter.next();
                if (isEligibleForCleanUp(rsrc)) {
                    numCleaned++;
                    uploadhandler.deleteChunks(rsrc);
                } else {
                    numLive++;
                }
            }
            if (admin.hasChanges()) {
                try {
                    admin.refresh();
                    admin.commit();
                } catch (PersistenceException re) {
                    log.info("ChunkCleanUpTask: Failed persisting chunk removal. Retrying later");
                }
            }

        } catch (Throwable t) {
            log.error(
                "ChunkCleanUpTask: General failure while trying to cleanup chunks",
                t);
        } finally {
            if (admin != null) {
                admin.close();
            }
        }
        long end = System.currentTimeMillis();
        log.info(
            "ChunkCleanUpTask finished: Removed {} chunk upload(s) in {}ms ({} chunk upload(s) still active)",
            new Object[] { numCleaned, (end - start), numLive });
    }

    /**
     * Check if {@link Resource} is eligible of
     * {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE} cleanup. To be
     * eligible the age of last
     * {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE} uploaded should be
     * greater than @link {@link #chunkCleanUpAge}
     *
     * @param rsrc {@link Resource} containing
     *            {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE}
     *            {@link Resource}s
     * @return true if eligible else false.
     */
    private boolean isEligibleForCleanUp(Resource rsrc) {
        boolean result = false;
        final Resource lastChunkNode = uploadhandler.getLastChunk(rsrc);
        if ( lastChunkNode != null ) {
            final Calendar created = lastChunkNode.getValueMap().get(JcrConstants.JCR_CREATED, Calendar.class);
            if ( created != null && System.currentTimeMillis() - created.getTimeInMillis() > chunkCleanUpAge ) {
                result = true;
            }
        }
        return result;
    }

    @Activate
    protected void activate(final Config configuration) {
        chunkCleanUpAge = configuration.chunk_cleanup_age();
        log.info("scheduler config [{}], chunkGarbageTime  [{}] ms",
            configuration.scheduler_expression(),
            chunkCleanUpAge);

    }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.queue.impl.simple;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a queue provider {@link org.apache.sling.distribution.queue.DistributionQueueProvider} for simple in memory
 * {@link org.apache.sling.distribution.queue.DistributionQueue}s
 */
public class SimpleDistributionQueueProvider implements DistributionQueueProvider {

    public static final String TYPE = "simple";
    public static final String TYPE_CHECKPOINT = "simple-checkpoint";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final Scheduler scheduler;

    private final Map<String, SimpleDistributionQueue> queueMap = new ConcurrentHashMap<String, SimpleDistributionQueue>();
    private final boolean checkpoint;
    private File checkpointDirectory;

    public SimpleDistributionQueueProvider(Scheduler scheduler, String name, boolean checkpoint) {
        this.checkpoint = checkpoint;
        if (name == null || scheduler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }

        if (checkpoint) {
            this.checkpointDirectory = new File(name + "-simple-queues-checkpoints");
            log.info("creating checkpoint directory {}", checkpointDirectory.getAbsoluteFile());
            if (checkpointDirectory.exists() && !checkpointDirectory.isDirectory()) {
                assert checkpointDirectory.delete();
            }
            boolean created = false;
            if (!checkpointDirectory.exists()) {
                created = checkpointDirectory.mkdir();
            }
            log.info("checkpoint directory created: {}, exists {}", created, checkpointDirectory.isDirectory() && checkpointDirectory.exists());
        }

        this.scheduler = scheduler;
        this.name = name;
    }

    @Nonnull
    public DistributionQueue getQueue(@Nonnull String queueName) {
        String key = name + queueName;

        SimpleDistributionQueue queue = queueMap.get(key);
        if (queue == null) {
            log.debug("creating a queue with key {}", key);
            queue = new SimpleDistributionQueue(name, queueName);
            queueMap.put(key, queue);
            log.debug("queue created {}", queue);
        }
        return queue;
    }

    Collection<SimpleDistributionQueue> getQueues() {
        return queueMap.values();
    }

    public void enableQueueProcessing(@Nonnull DistributionQueueProcessor queueProcessor, String... queueNames) {

        if (checkpoint) {
            // recover from checkpoints
            log.debug("recovering from checkpoints if needed");
            for (final String queueName : queueNames) {
                log.debug("recovering for queue {}", queueName);
                DistributionQueue queue = getQueue(queueName);
                FilenameFilter filenameFilter = new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String name) {
                        return name.equals(queueName + "-checkpoint");
                    }
                };
                for (File qf : checkpointDirectory.listFiles(filenameFilter)) {
                    log.info("recovering from checkpoint {}", qf);
                    try {
                        LineIterator lineIterator = IOUtils.lineIterator(new FileReader(qf));
                        while (lineIterator.hasNext()) {
                            String s = lineIterator.nextLine();
                            String[] split = s.split(" ");
                            String id = split[0];
                            String infoString = split[1];
                            Map<String, Object> info = new HashMap<String, Object>();
                            JSONTokener jsonTokener = new JSONTokener(infoString);
                            JSONObject jsonObject = new JSONObject(jsonTokener);
                            Iterator<String> keys = jsonObject.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONArray v = jsonObject.optJSONArray(key);
                                if (v != null) {
                                    String[] a = new String[v.length()];
                                    for (int i = 0; i < a.length; i++) {
                                        a[i] = v.getString(i);
                                    }
                                    info.put(key, a);
                                } else {
                                    info.put(key, jsonObject.getString(key));
                                }
                            }
                            queue.add(new DistributionQueueItem(id, info));
                        }
                        log.info("recovered {} items from queue {}", queue.getStatus().getItemsCount(), queueName);
                    } catch (FileNotFoundException e) {
                        log.warn("could not read checkpoint file {}", qf.getAbsolutePath());
                    } catch (JSONException e) {
                        log.warn("could not parse info from checkpoint file {}", qf.getAbsolutePath());
                    }
                }
            }

            // enable checkpointing
            for (String queueName : queueNames) {
                ScheduleOptions options = scheduler.NOW(-1, 15)
                        .canRunConcurrently(false)
                        .name(getJobName(queueName + "-checkpoint"));
                scheduler.schedule(new SimpleDistributionQueueCheckpoint(getQueue(queueName), checkpointDirectory), options);
            }
        }

        // enable processing
        for (String queueName : queueNames) {
            ScheduleOptions options = scheduler.NOW(-1, 1)
                    .canRunConcurrently(false)
                    .name(getJobName(queueName));
            scheduler.schedule(new SimpleDistributionQueueProcessor(getQueue(queueName), queueProcessor), options);
        }

    }

    public void disableQueueProcessing() {
        for (DistributionQueue queue : getQueues()) {
            String queueName = queue.getName();
            // disable queue processing
            if (scheduler.unschedule(getJobName(queueName))) {
                log.debug("queue processing on {} stopped", queue);
            } else {
                log.warn("could not disable queue processing on {}", queue);
            }
            if (checkpoint) {
                // disable checkpointing
                if (scheduler.unschedule(getJobName(queueName) + "-checkpoint")) {
                    log.debug("checkpoint on {} stopped", queue);
                } else {
                    log.warn("could not disable checkpoint on {}", queue);
                }
            }
        }
    }

    private String getJobName(String queueName) {
        return "simple-queueProcessor-" + name + "-" + queueName;
    }
}

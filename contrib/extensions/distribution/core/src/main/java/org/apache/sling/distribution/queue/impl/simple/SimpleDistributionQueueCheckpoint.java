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

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A light checkpointing of a {@link SimpleDistributionQueue} to {@link File}
 */
class SimpleDistributionQueueCheckpoint implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SimpleDistributionQueueCheckpoint.class);

    private final DistributionQueue queue;
    private final File checkpointDirectory;

    public SimpleDistributionQueueCheckpoint(DistributionQueue queue, File checkpointDirectory) {
        this.queue = queue;
        this.checkpointDirectory = checkpointDirectory;
    }

    @Override
    public void run() {
        String fileName = queue.getName() + "-checkpoint";
        File checkpointFile = new File(checkpointDirectory, fileName + "-new");
        log.debug("started checkpointing");

        try {
            if (checkpointFile.exists()) {
                assert checkpointFile.delete();
            }
            assert checkpointFile.createNewFile();
            Collection<String> lines = new LinkedList<String>();
            FileOutputStream fileOutputStream = new FileOutputStream(checkpointFile);
            for (DistributionQueueEntry queueEntry : queue.getItems(0, -1)) {
                DistributionQueueItem item = queueEntry.getItem();
                String packageId = item.getPackageId();
                StringWriter w = new StringWriter();
                JSONWriter jsonWriter = new JSONWriter(w);
                jsonWriter.object();
                for (Map.Entry entry : item.entrySet()) {
                    jsonWriter.key(String.valueOf(entry.getKey()));
                    Object value = entry.getValue();
                    boolean isArray = value instanceof String[];
                    if (isArray) {
                        jsonWriter.array();
                        for (String s : ((String[]) value)) {
                            jsonWriter.value(s);
                        }
                        jsonWriter.endArray();
                    } else {
                        jsonWriter.value(value);
                    }
                }
                jsonWriter.endObject();
                lines.add(packageId + " " + w.toString());
            }
            log.debug("parsed {} items", lines.size());
            IOUtils.writeLines(lines, Charset.defaultCharset().name(), fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            boolean success = checkpointFile.renameTo(new File(checkpointDirectory, fileName));
            log.debug("checkpoint succeeded: {}", success);
        } catch (Exception e) {
            log.error("failed checkpointing for queue {}", queue.getName());
        }
    }
}

package org.apache.sling.event.impl.job;

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
import java.util.UUID;

import org.apache.sling.event.impl.EventHelper;

public class JobUtil {

    /**
     * Create a unique node path (folder and name) for the job.
     */
    public static String getUniquePath(final String jobTopic, final String jobId) {
        final StringBuilder sb = new StringBuilder(jobTopic.replace('/', '.'));
        sb.append('/');
        if ( jobId != null ) {
            // we create an md from the job id - we use the first 6 bytes to
            // create sub directories
            final String md5 = EventHelper.md5(jobId);
            sb.append(md5.substring(0, 2));
            sb.append('/');
            sb.append(md5.substring(2, 4));
            sb.append('/');
            sb.append(md5.substring(4, 6));
            sb.append('/');
            sb.append(EventHelper.filter(jobId));
        } else {
            // create a path from the uuid - we use the first 6 bytes to
            // create sub directories
            final String uuid = UUID.randomUUID().toString();
            sb.append(uuid.substring(0, 2));
            sb.append('/');
            sb.append(uuid.substring(2, 4));
            sb.append('/');
            sb.append(uuid.substring(5, 7));
            sb.append("/Job_");
            sb.append(uuid.substring(8, 17));
        }
        return sb.toString();
    }

}

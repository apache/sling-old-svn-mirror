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
package org.apache.sling.event.impl.job;

import org.apache.sling.event.EventUtil;
import org.osgi.service.event.Event;

/**
 * Helper class for the job processing.
 * This class is used to get the parallel processing information about
 * a job.
 */
public class ParallelInfo {

    public static final ParallelInfo UNLIMITED = new ParallelInfo(true, -1);
    public static final ParallelInfo SERIAL = new ParallelInfo(false, -1);

    public final boolean processParallel;
    public final int maxParallelJob;

    private ParallelInfo(final boolean processParallel, final int maxParallelJob) {
        this.processParallel = processParallel;
        this.maxParallelJob = maxParallelJob;
    }

    /**
     * Return the parallel processing information
     * For improved processing we first check if a job queue name is set
     * and always return true. This is a pure implementation thing!
     * If the parallel property is set, the following checks are performed:
     * - boolean object: if false, no parallel processing, if true
     *                   full parallel processing
     * - number object: if higher than 1, parallel processing with the
     *                  specified value, else no parallel processing
     * - string value: if "no" or "false", no parallel processing
     *                 if it is a string representation of a number,
     *                 it's treated like a number object (see above)
     *                 in all other cases unlimited parallel processing
     *                 is returnd.
     * If the property is not set we return no parallel processing.
     */
    public static ParallelInfo getParallelInfo(final Event job) {
        if ( job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null ) {
            return ParallelInfo.UNLIMITED;
        }
        final Object value = job.getProperty(EventUtil.PROPERTY_JOB_PARALLEL);
        if ( value != null ) {
            if ( value instanceof Boolean ) {
                final boolean result = ((Boolean)value).booleanValue();
                if ( result ) {
                    return ParallelInfo.UNLIMITED;
                }
                return ParallelInfo.SERIAL;
            } else if ( value instanceof Number ) {
                final int result = ((Number)value).intValue();
                if ( result > 1 ) {
                    return new ParallelInfo(true, result);
                }
                return ParallelInfo.SERIAL;
            }
            final String strValue = value.toString();
            if ( "no".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue) ) {
                return ParallelInfo.SERIAL;
            }
            // check if this is a number
            try {
                final int result = Integer.valueOf(strValue).intValue();
                if ( result > 1 ) {
                    return new ParallelInfo(true, result);
                }
                return ParallelInfo.SERIAL;
            } catch (NumberFormatException ne) {
                // we ignore this
            }
            return ParallelInfo.UNLIMITED;
        }
        return ParallelInfo.SERIAL;
    }
}
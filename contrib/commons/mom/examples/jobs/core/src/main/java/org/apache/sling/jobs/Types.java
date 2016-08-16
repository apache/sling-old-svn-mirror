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

package org.apache.sling.jobs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ieb on 13/04/2016.
 * This has to be part of the API to prevent all sorts of other issues.
 */
public final class Types {



    private Types() {

    }

    public interface JobQueue {
        org.apache.sling.mom.Types.QueueName asQueueName();

        org.apache.sling.mom.Types.TopicName asTopicName();

    }

    public interface JobType {
    }

    public static JobQueue jobQueue(String jobQueue) {
        return new JobQueueImpl(jobQueue);
    }

    public static JobType jobType(String jobType) {
        return new JobTypeImpl(jobType);
    }

    public static JobQueue ANY_JOB_QUEUE = new AnyJobQueue();


    public static Set<JobType> jobType(String[] types) {
        Set<JobType> hs = new HashSet<JobType>();
        for ( String s : types) {
            hs.add(jobType(s));
        }
        return Collections.unmodifiableSet(hs);
    }




    /**
     * Wraps a JobType.
     */
    private static class JobTypeImpl extends StringWrapper implements JobType {

        private JobTypeImpl(String jobType) {
            super(jobType);
        }
    }



    /**
     * Wraps a JobQueue.
     */
    private static class JobQueueImpl extends StringWrapper implements JobQueue {

        private JobQueueImpl(String jobQueue) {
            super(jobQueue);
        }


        @Override
        public boolean equals(Object obj) {
            return obj == ANY_JOB_QUEUE || super.equals(obj);
        }


        @Override
        public org.apache.sling.mom.Types.QueueName asQueueName() {
            return org.apache.sling.mom.Types.queueName(toString());
        }

        @Override
        public org.apache.sling.mom.Types.TopicName asTopicName() {
            return org.apache.sling.mom.Types.topicName(toString());
        }
    }

    /**
     * Special JobQueue to match any.
     */
    private static class AnyJobQueue extends JobQueueImpl {


        private AnyJobQueue() {
            super("*");
        }


        @Override
        public boolean equals(Object obj) {
            return true;
        }

        @Override
        public int compareTo(String o) {
            return 0;
        }
    }

    private static class StringWrapper implements Comparable<String> {


        private String value;

        private StringWrapper(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return value.equals(obj.toString());
        }

        @Override
        public int compareTo(String o) {
            return value.compareTo(o);
        }

        public String toString() {
            return value;
        }
    }

}

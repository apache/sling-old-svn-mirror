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
package org.apache.sling.commons.scheduler.impl;

import java.util.Map;

/**
 * Small helper which is used to calculate metrics suffixes in a performant way
 * (as that gets execute per job execution).
 */
class MetricsHelper {
    
    static final String UNKNOWN_JOBNAME_SUFFIX = "unknown";

    /**
     * Given a particular jobName/job tuple and based on a provided config,
     * derive a filter name from it - if it is configured so.
     * <p>
     * This method is on the critical path wrt job execution so it is important
     * for it to be implemented in a performing way.
     */
    static String deriveFilterName(final ConfigHolder configHolder, final Object job) {
        if (configHolder == null) {
            return null;
        }
        if (job == null) {
            return null;
        }
        final Class<? extends Object> jobClass = job.getClass();
        final String packageName = jobClass.getPackage().getName();
        final Map<String, String> filterDefinitionMap = configHolder.getFilterDefinitions().get(packageName);
        if (filterDefinitionMap == null) {
            // then no match
            return null;
        }
        String jobToStr = jobClass.getName();
        final int dollarPos = jobToStr.indexOf("$");
        if (dollarPos != -1) {
            // cut off inner class name part
            jobToStr = jobToStr.substring(0, dollarPos);
        }
        
        return filterDefinitionMap.get(jobToStr);
    }

    /**
     * Shortens the provided jobName for it to be useful as a metrics suffix.
     * <p>
     * This method will only be used for slow jobs, so it's not the
     * most critical performance wise.
     */
    static String asMetricsSuffix(String jobName) {
        if (jobName == null || jobName.length() == 0) {
            return UNKNOWN_JOBNAME_SUFFIX;
        }
        // translate org.apache.jackrabbit.oak.jcr.observation.ChangeProcessor$4
        // into oajojo.ChangeProcessor

        // this used to do go via the job class, ie
        // the job must be a Runnable or a scheduler Job,
        // so getClass().getName() should return an actual package
        // name.classname[$anonymous/inner]
        // however, the same job class could in theory be used for
        // multiple schedules - so the really unique thing with schedules
        // is its name
        // so we're also using the name here - but we're shortening it.

        final StringBuffer shortified = new StringBuffer();
        // cut off dollar
        final int dollarPos = jobName.indexOf("$");
        if (dollarPos != -1) {
            jobName = jobName.substring(0, dollarPos);
        }
        final String[] split = jobName.split("\\.");
        if (split.length <= 2) {
            // then don't shorten at all
            shortified.append(jobName);
        } else {
            for (int i = 0; i < split.length; i++) {
                final String s = split[i];
                if (i < split.length - 2) {
                    // shorten
                    if (s.length() > 0) {
                        shortified.append(s.substring(0, 1));
                    }
                } else {
                    // except for the last 2
                    shortified.append(".").append(s);
                }
            }
        }
        return shortified.toString();
    }

}

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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A holder of a QuartzSchedulerConfiguration which does some computation at
 * construction time and provides accessors for those.
 * <p>
 * Specifically it creates maps of filter name to filter definition and
 * vice-verca.
 */
public class ConfigHolder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigHolder.class);

    private final QuartzSchedulerConfiguration config;

    private final Map<String, String> filterSuffixes = new HashMap<>();
    private final Map<String, Map<String, String>> filterDefinitions = new HashMap<>();

    ConfigHolder(final QuartzSchedulerConfiguration config) {
        this.config = config;
        if (config == null) {
            return;
        }
        final String[] groupFilter = config.metrics_filters();
        if (groupFilter == null) {
            return;
        }
        for (String aFilterDefinition : groupFilter) {
            final String[] split = aFilterDefinition.split("=");
            if (split == null || split.length != 2) {
                logger.warn(
                        "activate: ignoring wrongly formatted (expects 1 '=') filter definition: " + aFilterDefinition);
                continue;
            }
            final String packageName = packageOf(split[1]);
            if (packageName == null) {
                logger.warn("activate: ignoring wrongly formatted filter definition, "
                        + "expected fully qualified class name (except $anonymous or $inner class part) :" + aFilterDefinition);
                continue;
            }
            if (split[1].contains("$") || split[1].contains("*") || split[1].contains("?")) {
                logger.warn("activate: ignoring wrongly formatted filter definition, "
                        + "disallowed character(s) used ($, *, ?) :" + aFilterDefinition);
                continue;
            }
            filterSuffixes.put(split[0], split[1]);
            Map<String, String> map = filterDefinitions.get(packageName);
            if (map == null) {
                map = new HashMap<>();
                filterDefinitions.put(packageName, map);
            }
            map.put(split[1], split[0]);
        }
    }

    static String packageOf(String jobClass) {
        int lastDot = jobClass.lastIndexOf(".");
        if (lastDot == -1) {
            return null;
        } else {
            return jobClass.substring(0, lastDot);
        }
    }

    String poolName() {
        if (config == null) {
            return ThreadPoolManager.DEFAULT_THREADPOOL_NAME;
        } else {
            return config.poolName();
        }
    }

    String[] allowedPoolNames() {
        if (config == null) {
            return null;
        } else {
            return config.allowedPoolNames();
        }
    }

    long slowThresholdMillis() {
        if (config == null) {
            return QuartzJobExecutor.DEFAULT_SLOW_JOB_THRESHOLD_MILLIS;
        } else {
            return config.slowThresholdMillis();
        }
    }

    Map<String, String> getFilterSuffixes() {
        return filterSuffixes;
    }

    Map<String, Map<String, String>> getFilterDefinitions() {
        return filterDefinitions;
    }

}

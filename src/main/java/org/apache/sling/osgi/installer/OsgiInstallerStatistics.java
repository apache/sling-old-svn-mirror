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
package org.apache.sling.osgi.installer;


/**
 * Statistics about the OSGi installer.
 *
 * TODO Remove this interface - it's just used for testing!!
 */
public interface OsgiInstallerStatistics {

	/** Return counters used for statistics, console display, testing, etc. */
	long [] getCounters();

	/** Counter index: number of OSGi tasks executed */
	int OSGI_TASKS_COUNTER = 0;

    /** Counter index: number of installer cycles */
    int INSTALLER_CYCLES_COUNTER = 1;

    /** Counter index: number of currently registered resources */
    int REGISTERED_RESOURCES_COUNTER = 2;

    /** Counter index: number of currently registered resource groups
     *  of resources having the same OSGi entity ID */
    int REGISTERED_GROUPS_COUNTER = 3;

    /** Counter index: is worker thread idle? (not really a counter: 1 means true) */
    int WORKER_THREAD_IS_IDLE_COUNTER = 4;

    /** Counter index: how many times did worker thread become idle */
    int WORKER_THREAD_BECOMES_IDLE_COUNTER = 5;

	/** Size of the counters array */
	int COUNTERS_SIZE = 6;
}
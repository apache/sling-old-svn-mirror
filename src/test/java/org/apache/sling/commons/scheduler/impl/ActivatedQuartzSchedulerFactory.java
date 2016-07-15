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

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.threads.impl.DefaultThreadPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * This is just a class with helper static method,
 * since we need an activated QuartzScheduler in many tests.
 */
class ActivatedQuartzSchedulerFactory {

    public static QuartzScheduler create(BundleContext context, String poolName) throws Exception {
        QuartzScheduler quartzScheduler = null;
        if (context != null) {
            quartzScheduler = new QuartzScheduler();
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");
            props.put(Constants.SERVICE_PID, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");

            Field f = QuartzScheduler.class.getDeclaredField("threadPoolManager");
            f.setAccessible(true);
            f.set(quartzScheduler, new DefaultThreadPoolManager(context, props));

            Map<String, Object> scheduleActivationProps = new HashMap<String, Object>();
            scheduleActivationProps.put("poolName", poolName == null ? "testName" : poolName);
            if ( poolName != null ) {
                scheduleActivationProps.put("allowedPoolNames", new String[] {"testName", "allowed"});
            }

            quartzScheduler.activate(context, scheduleActivationProps);
            context.registerService("scheduler", quartzScheduler, props);
        }
        return quartzScheduler;
    }
}

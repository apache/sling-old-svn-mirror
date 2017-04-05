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
package org.apache.sling.event.impl.jobs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.event.jobs.Job;
import org.junit.Test;

public class JobsImplTest {

    @Test public void testSorting() {
        final Calendar now = Calendar.getInstance();
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Job.PROPERTY_JOB_CREATED, now);

        final JobImpl job1 = new JobImpl("test", "hello_1", properties);
        final JobImpl job2 = new JobImpl("test", "hello_2", properties);
        final JobImpl job3 = new JobImpl("test", "hello_4", properties);
        final JobImpl job4 = new JobImpl("test", "hello_30", properties);
        final JobImpl job5 = new JobImpl("test", "hello_50", properties);

        final List<JobImpl> list = new ArrayList<JobImpl>();
        list.add(job5);
        list.add(job2);
        list.add(job1);
        list.add(job4);
        list.add(job3);

        Collections.sort(list);

        assertEquals(job1, list.get(0));
        assertEquals(job2, list.get(1));
        assertEquals(job3, list.get(2));
        assertEquals(job4, list.get(3));
        assertEquals(job5, list.get(4));

    }
}

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
package org.apache.sling.event.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class TimedJobsTest extends AbstractJobHandlingTest {

    private static final String TOPIC = "timed/test/topic";

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testTimedJob() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        final ServiceRegistration ehReg = this.registerJobConsumer(TOPIC, new JobConsumer() {

            @Override
            public JobResult process(final Job job) {
                if ( job.getTopic().equals(TOPIC) ) {
                    counter.incrementAndGet();
                }
                return JobResult.OK;
            }

        });
        try {
            final Date d = new Date();
            d.setTime(System.currentTimeMillis() + 3000); // run in 3 seconds

            // create scheduled job
            final ScheduledJobInfo info = this.getJobManager().createJob(TOPIC).schedule().at(d).add();
            assertNotNull(info);

            while ( counter.get() == 0 ) {
                this.sleep(1000);
            }
            assertEquals(1, this.getJobManager().getScheduledJobs().size()); // job is still scheduled
            info.unschedule();
        } finally {
            ehReg.unregister();
        }
    }
}

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
package org.apache.sling.event.impl.jobs.tasks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class HistoryCleanUpTaskTest {

    private static final String JCR_PATH = JobManagerConfiguration.DEFAULT_REPOSITORY_PATH + "/finished";
    private static final String JCR_TOPIC = "test";
    private static final String JCR_JOB_NAME = "test-job";
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd/HH/mm");
    private static final int MAX_AGE_IN_DAYS = 60;

    @Rule
    public final SlingContext ctx = new SlingContext();

    @Mock
    private JobManagerConfiguration configuration;
    private Job job;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private JobExecutionContext jobContext;

    private HistoryCleanUpTask task;

    @Before
    public void setUp() {
        setUpJob();
        setupConfiguration();
        task = ctx.registerInjectActivateService(new HistoryCleanUpTask());
    }

    private void setupConfiguration() {
        Mockito.when(configuration.getStoredSuccessfulJobsPath()).thenReturn(JCR_PATH);
        Mockito.when(configuration.createResourceResolver()).thenReturn(ctx.resourceResolver());
        ctx.registerService(JobManagerConfiguration.class, configuration);
    }

    private void setUpJob() {
        Map<String, Object> parameters = Maps.<String, Object> newHashMap();
        parameters.put("age", MAX_AGE_IN_DAYS * 24 * 60);
        job = new JobImpl("not-relevant", "not-relevant_123", parameters);
        Mockito.when(jobContext.isStopped()).thenReturn(false);
    }

    @Test
    public void shouldNotDeleteResourcesYoungerThanRemoveDate() {
        Resource resource = createResourceWithDaysBeforeDate(MAX_AGE_IN_DAYS / 2);
        task.process(job, jobContext);
        assertNotNull(ctx.resourceResolver().getResource(resource.getPath()));
    }

    @Test
    public void shouldDeleteResourcesOlderThanRemoveDate() {
        Resource resource = createResourceWithDaysBeforeDate(MAX_AGE_IN_DAYS * 2);
        task.process(job, jobContext);
        assertNull(ctx.resourceResolver().getResource(resource.getPath()));
    }

    private Resource createResourceWithDaysBeforeDate(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        String path = JCR_PATH + '/' + JCR_TOPIC + '/' + DATE_FORMATTER.format(cal.getTime()) + '/' + JCR_JOB_NAME;
        return ctx.create().resource(path);
    }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.samples.impl;

import java.util.Date;

import org.apache.sling.hc.annotations.SlingHealthCheck;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;

/** Test creating a Health Check using the SlingHealthCheck
 *  annotation. */

@SlingHealthCheck(
        name="Annotated Health Check Sample", 
        mbeanName="annotatedHC",
        description="Sample Health Check defined by a java annotation",
        tags={"sample","annotation"})

public class AnnotatedHealthCheckSample implements HealthCheck{

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.info("All good at {}", new Date());
        return new Result(resultLog);
    }
}
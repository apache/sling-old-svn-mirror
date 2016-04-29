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
package org.apache.sling.hc.core.impl.servlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serializes health check results into json format. */
@Service(ResultJsonSerializer.class)
@Component(metatype = false)
public class ResultJsonSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ResultJsonSerializer.class);

    static final String OVERALL_RESULT_KEY = "OverallResult";

    public String serialize(final Result overallResult, final List<HealthCheckExecutionResult> executionResults, final String jsonpCallback,
            boolean includeDebug) {

        LOG.debug("Sending json response... ");

        JSONObject result = new JSONObject();
        try {

            result.put("overallResult", overallResult.getStatus());
            JSONArray resultsJsonArr = new JSONArray();
            result.put("results", resultsJsonArr);

            for (HealthCheckExecutionResult healthCheckResult : executionResults) {
                resultsJsonArr.put(getJsonForSimpleResult(healthCheckResult, includeDebug));
            }

        } catch (JSONException e) {
            LOG.info("Could not serialize health check result: " + e, e);
        }

        String resultStr;
        if (StringUtils.isNotBlank(jsonpCallback)) {
            resultStr = jsonpCallback + "(" + result.toString() + ");";
        } else {
            resultStr = result.toString();
        }

        return resultStr;

    }

    private JSONObject getJsonForSimpleResult(final HealthCheckExecutionResult healthCheckResult, boolean includeDebug) throws JSONException {

        JSONObject result = new JSONObject();

        result.put("name", healthCheckResult.getHealthCheckMetadata().getName());
        result.put("status", healthCheckResult.getHealthCheckResult().getStatus());
        result.put("timeInMs", healthCheckResult.getElapsedTimeInMs());
        result.put("finishedAt", healthCheckResult.getFinishedAt());

        JSONArray messagesArr = new JSONArray();
        result.put("messages", messagesArr);
        for (ResultLog.Entry entry : healthCheckResult.getHealthCheckResult()) {
            if (!includeDebug && entry.getStatus() == Result.Status.DEBUG) {
                continue;
            }
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("status", entry.getStatus());
            jsonEntry.put("message", entry.getMessage());
            Exception exception = entry.getException();
            if (exception != null) {
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                jsonEntry.put("exception", stringWriter.toString());
            }
            messagesArr.put(jsonEntry);
        }

        return result;
    }

}

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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
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

        JsonObjectBuilder result = Json.createObjectBuilder();
        try {

            result.add("overallResult", overallResult.getStatus().toString());
            JsonArrayBuilder resultsJsonArr = Json.createArrayBuilder();
            
            for (HealthCheckExecutionResult healthCheckResult : executionResults) {
                resultsJsonArr.add(getJsonForSimpleResult(healthCheckResult, includeDebug));
            }
            
            result.add("results", resultsJsonArr);
        } catch (JsonException e) {
            LOG.info("Could not serialize health check result: " + e, e);
        }

        StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(result.build()).close();
        
        String resultStr = writer.toString();
        
        if (StringUtils.isNotBlank(jsonpCallback)) {
            resultStr = jsonpCallback + "(" + resultStr + ");";
        }

        return resultStr;

    }

    private JsonObject getJsonForSimpleResult(final HealthCheckExecutionResult healthCheckResult, boolean includeDebug) {

        JsonObjectBuilder result = Json.createObjectBuilder();

        result.add("name", healthCheckResult.getHealthCheckMetadata().getName());
        result.add("status", healthCheckResult.getHealthCheckResult().getStatus().toString());
        result.add("timeInMs", healthCheckResult.getElapsedTimeInMs());
        result.add("finishedAt", healthCheckResult.getFinishedAt().toString());
        JsonArrayBuilder tagsArray = Json.createArrayBuilder();
        for (final String tag : healthCheckResult.getHealthCheckMetadata().getTags()) {
            tagsArray.add(tag);
        }
        result.add("tags", tagsArray);

        JsonArrayBuilder messagesArr = Json.createArrayBuilder();
        
        for (ResultLog.Entry entry : healthCheckResult.getHealthCheckResult()) {
            if (!includeDebug && entry.getStatus() == Result.Status.DEBUG) {
                continue;
            }
            JsonObjectBuilder jsonEntry = Json.createObjectBuilder();
            jsonEntry.add("status", entry.getStatus().toString());
            jsonEntry.add("message", entry.getMessage());
            Exception exception = entry.getException();
            if (exception != null) {
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                jsonEntry.add("exception", stringWriter.toString());
            }
            messagesArr.add(jsonEntry);
        }
        
        result.add("messages", messagesArr);
        

        return result.build();
    }

}

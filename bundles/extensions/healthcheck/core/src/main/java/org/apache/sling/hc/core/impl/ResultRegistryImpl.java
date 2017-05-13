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
package org.apache.sling.hc.core.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.ResultLog.Entry;
import org.apache.sling.hc.api.ResultRegistry;
import org.apache.sling.hc.api.Result.Status;

@Component()
@Service(value={ResultRegistry.class, HealthCheck.class})
public class ResultRegistryImpl implements ResultRegistry {
    
    private Map<String, TimedResult> activeResults = new HashMap<String, TimedResult>(500, 0.75f);
    
    @Override
    public void put(@Nonnull String identifier, @Nonnull Result result, @Nullable Calendar expiration) {
        if(expiration != null && expiration.before(Calendar.getInstance())) {
            return;
        }
        synchronized(activeResults) {
            TimedResult previous = activeResults.get(identifier);
            if(previous != null && previous.expiration != null && previous.expiration.before(Calendar.getInstance())) {
                previous = null;
            }
            if(previous != null && previous.result.getStatus().ordinal() > result.getStatus().ordinal()) {
                //the previous entry is more important. 
                //Do we need to update the calendar of the previous entry?
                if(previous.expiration != null && result.getStatus().ordinal() >= Result.Status.WARN.ordinal()) {
                    //potentially.
                    if(expiration == null) {
                        previous.expiration = null;
                    }
                    else if(previous.expiration.before(expiration)) {
                        previous.expiration = expiration;
                    }
                    //else nothing to change for previous entry
                }
                return; //do not store this new entry, because previous is more important.
            }
            activeResults.put(identifier, new TimedResult(result, expiration));
        }
    }

    @Override
    public void remove(@Nonnull String identifier) {
        synchronized (activeResults) {
            activeResults.remove(identifier);
        }
    }
    
    @Override
    public Result execute() {
        Collection<IdentifiedResult> results = getActiveResults();
        if(results.isEmpty()) {
            return new Result(Status.OK, "No results to report");
        }
        
        ResultLog resultLog = new ResultLog();
        for (IdentifiedResult identifiedResult : results) {
            for (Entry entry : identifiedResult.result) {
                resultLog.add(new ResultLog.Entry(entry.getStatus(), identifiedResult.identifier + ": " + entry.getMessage(), entry.getException()));
            }
        }

        return new Result(resultLog);
    }
    
    private void removeExpiredResultIfSame(@Nonnull String identifier, @Nonnull TimedResult result) {
        synchronized(activeResults) {
            if(activeResults.get(identifier) == result) {
                activeResults.remove(identifier);
            }
        }
    }
    
    private Collection<IdentifiedResult> getActiveResults() {
        //TreeMap will keep the ordering consistent.
        final TreeMap<String, IdentifiedResult> results = new TreeMap<String, IdentifiedResult>();
        final Calendar now = Calendar.getInstance();
        for(String identifier : activeResults.keySet().toArray(new String[0])) {
            TimedResult tr = activeResults.get(identifier);
            if(tr == null) {
                continue;
            }
            if(tr.expiration != null && tr.expiration.before(now)) {
                removeExpiredResultIfSame(identifier, tr);
                continue;
            }
            results.put(identifier, new IdentifiedResult(identifier, tr.result));
        }
        return results.values();
    }
    
    private class TimedResult {
        @Nonnull
        final Result result;
        @Nullable
        Calendar expiration;
        public TimedResult(@Nonnull Result result, @Nullable Calendar expiration) {
            this.result = result;
            this.expiration = expiration;
        }
    }
    
    private class IdentifiedResult {
        @Nonnull
        final String identifier;
        @Nonnull
        final Result result;
        public IdentifiedResult(String identifier, Result result) {
            super();
            this.identifier = identifier;
            this.result = result;
        }
    }
}

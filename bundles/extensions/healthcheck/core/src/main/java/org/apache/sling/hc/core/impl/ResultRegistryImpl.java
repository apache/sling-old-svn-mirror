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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.Result.Status;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.ResultLog.Entry;
import org.apache.sling.hc.api.ResultRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service(value={ResultRegistry.class}) //HealthCheck.class is registered dynamically.
public class ResultRegistryImpl implements ResultRegistry {

    private static final String NAME = "Aggregating ResultRegistry Service";
    
    private static final Logger log = LoggerFactory.getLogger(ResultRegistryImpl.class);
    
    @SuppressWarnings("rawtypes")
    private ServiceRegistration healthCheckRegistration;

    private Map<String, TimedResult> activeResults = new HashMap<String, TimedResult>(500, 0.75f);
    private Map<String, Set<String>> tags = new TreeMap<String, Set<String>>();
    
    private final Object semaphore = new Object();
    
    @Override
    public void put(@Nonnull String identifier, @Nonnull Result result, @Nullable Calendar expiration, @Nullable String... tags) {
        if(expiration != null && expiration.before(Calendar.getInstance())) {
            return;
        }
        if(tags == null) {
            tags = new String[0];
        }
        
        synchronized(semaphore) {
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
            final TimedResult old = activeResults.get(identifier);
            if(old != null) {
                removeNoBuild(identifier, old); //remove fixes tags
            }
            activeResults.put(identifier, new TimedResult(result, expiration, tags));
            for(String tag : tags) {
                if(!this.tags.containsKey(tag)) {
                    this.tags.put(tag, new TreeSet<String>());
                }
                Set<String> names = this.tags.get(tag);
                names.add(identifier);
            }
            
            buildProperties();
        }
    }
    

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void buildProperties() {
        if(healthCheckRegistration == null) {
            log.warn("Unable to build properties, lacking service registration");
            return;
        }
        Dictionary d = new java.util.Properties();
        d.put(HealthCheck.NAME, NAME);
        d.put(HealthCheck.MBEAN_NAME, NAME);
        d.put(HealthCheck.TAGS, tags.keySet().toArray(new String[0]));
        healthCheckRegistration.setProperties(d);
    }
    
    
    @Activate
    public void activate(BundleContext context) {
        healthCheckRegistration = context.registerService(HealthCheck.class, (HealthCheck)this, null);
        buildProperties();
    }
    
    @Deactivate
    public void deactivate(BundleContext context) {
        if(healthCheckRegistration != null) {
            healthCheckRegistration.unregister();
        }
        healthCheckRegistration = null;
    }

    @Override
    public void remove(@Nonnull String identifier) {
        TimedResult ir = activeResults.get(identifier);
        if(ir != null) {
            removeNoBuild(identifier, ir);
            buildProperties();
        }
    }

    private void removeNoBuild(@Nonnull String identifier, @Nonnull TimedResult result) {
        synchronized (semaphore) {
            if(activeResults.remove(identifier) != null) {
                for(String tag : result.tags) {
                    Set<String> tagList = tags.get(tag);
                    if(tagList == null) {
                        continue; //shouldn't happen.
                    }
                    if(tagList.contains(identifier)) {
                        tagList.remove(identifier); //should always happen
                    }
                    if(tagList.isEmpty()) {
                        tags.remove(tag);
                    }
                }
            }
        }
    }
    
    @Override
    public Result execute() {
        Collection<IdentifiedResult> results = getActiveResults();
        if(results.isEmpty()) {
            ResultLog rv = new ResultLog();
            
            rv.add(new Entry(Status.OK, "Nothing to report"));
            return new Result(rv);
        }
        
        ResultLog resultLog = new ResultLog();
        for (IdentifiedResult identifiedResult : results) {
            for (Entry entry : identifiedResult.result) {
                resultLog.add(new ResultLog.Entry(entry.getStatus(), identifiedResult.identifier + ": " + entry.getMessage(), entry.getException()));
            }
        }

        return new Result(resultLog);
    }
    

    /**
     * returns true if actually removed.
     */
    private boolean removeExpiredResultIfSame(@Nonnull String identifier, @Nonnull TimedResult result) {
        synchronized(semaphore) {
            if(activeResults.get(identifier) == result) {
                remove(identifier);
                return true;
            }
        }
        return false;
    }
    
    private Collection<IdentifiedResult> getActiveResults() {
        //TreeMap will keep the ordering consistent.
        final TreeMap<String, IdentifiedResult> results = new TreeMap<String, IdentifiedResult>();
        final Calendar now = Calendar.getInstance();
        boolean recalculateTags = false;
        for(String identifier : activeResults.keySet().toArray(new String[0])) {
            TimedResult tr = activeResults.get(identifier);
            if(tr == null) {
                continue;
            }
            if(tr.expiration != null && tr.expiration.before(now)) {
                if(removeExpiredResultIfSame(identifier, tr)) {
                    recalculateTags = true;
                }
                continue;
            }
            results.put(identifier, new IdentifiedResult(identifier, tr.result, tr.tags));
        }
        if(recalculateTags) {
            buildProperties();
        }
        return results.values();
    }
    
    private class TimedResult {
        @Nonnull
        final Result result;
        @Nullable
        Calendar expiration;
        @Nonnull 
        final String[] tags;
        public TimedResult(@Nonnull Result result, @Nullable Calendar expiration, @Nonnull String[] tags) {
            this.result = result;
            this.expiration = expiration;
            this.tags = tags;
        }
    }
    
    private class IdentifiedResult {
        @Nonnull
        final String identifier;
        @Nonnull
        final Result result;
        @Nonnull 
        final String[] tags;
        public IdentifiedResult(@Nonnull String identifier, @Nonnull Result result, @Nonnull String[] tags) {
            super();
            this.identifier = identifier;
            this.result = result;
            this.tags = tags;
        }
    }
}

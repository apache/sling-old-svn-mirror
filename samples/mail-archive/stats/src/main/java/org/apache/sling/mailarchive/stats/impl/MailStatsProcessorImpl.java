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
package org.apache.sling.mailarchive.stats.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.james.mime4j.dom.Message;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.mailarchive.stats.MailStatsProcessor;
import org.apache.sling.mailarchive.stats.OrgMapper;
import org.apache.sling.mailarchiveserver.api.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
/** Computes stats of how often a given organization writes to
 *  another one in a given time period which is defined by
 *  a Date formatter. Using a formatter that uses only year and
 *  month, for example, yields per-month data.
 */
public class MailStatsProcessorImpl implements MailStatsProcessor, MessageProcessor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private OrgMapper orgMapper;
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    // TODO configurable format
    final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM");
    
    // TODO configurable root path
    private static final String ROOT_PATH = "/content/mailarchiveserver/stats"; 
    
    public static final String DEFAULT_RESOURCE_TYPE = "mailserver/stats";
    public static final String DESTINATION_RESOURCE_TYPE = "mailserver/stats/destination";
    public static final String DATA_RESOURCE_TYPE = "mailserver/stats/data";
    public static final String PERIOD_PROP = "period";
    private static final String [] EMPTY_STRING_ARRAY = new String[]{};
    static final String SOURCE_PROP_PREFIX = "FROM_";
    
    // We need to count the number of messages to a destination, 
    // per formatted timestamp and source
    private final Map<String, DataRecord> data = new HashMap<String, DataRecord>();
    
    class DataRecord {
        final String destination;
        final Map<String, Integer> sourceCounts = new HashMap<String, Integer>();
        final String timestampPath;
        
        DataRecord(Date d, String destination) {
            this.destination = orgMapper.mapToOrg(destination);
            synchronized (dateFormat) {
                this.timestampPath = dateFormat.format(d); 
            }
        }
        
        Map<String, Integer> getSourceCounts() {
            return sourceCounts;
        }
        
        void increment(String source) {
            source = SOURCE_PROP_PREFIX + orgMapper.mapToOrg(source);
            Integer count = sourceCounts.get(source);
            if(count == null) {
                count = 1;
            } else {
                count = count.intValue() + 1;
            }
            sourceCounts.put(source, count);
        }
        
        public String getOrgPath() {
            return ROOT_PATH + "/" + destination;
        }
        
        public String getPath() {
            return getOrgPath() + "/" + timestampPath;
        }
        
        @Override
        public String toString() {
            return new StringBuilder()
            .append(getClass().getSimpleName())
            .append(' ')
            .append(timestampPath)
            .append(' ')
            .append(destination)
            .append(' ')
            .append(sourceCounts)
            .toString();
        }
        
        String getKey() {
            return new StringBuilder()
            .append(timestampPath)
            .append('#')
            .append(destination)
            .toString();
        }
    }
    
    public void computeStats(Date d, String from, String [] to, String [] cc) {
        if(to != null) {
            for(String dest : to) {
                addRecord(d, from, dest);
            }
        }
        if(cc != null) {
            for(String dest : cc) {
                addRecord(d, from, dest);
            }
        }
    }
    
    private void addRecord(Date d, String from, String to) {
        final DataRecord r = new DataRecord(d, to);
        synchronized (data) {
            final DataRecord existing = data.get(r.getKey());
            if(existing == null) {
                r.increment(from);
                data.put(r.getKey(), r);
            } else {
                existing.increment(from);
            }
        }
    }

    /** Called by the mail archive server store before storing messages - 
     *  we hook into this to compute our stats.
     */
    @Override
    public void processMessage(Message m) {
        log.debug("Processing {}", m);
        final String [] to = toArray(m.getTo());
        final String [] cc = toArray(m.getCc());
        for(String from : MailStatsProcessorImpl.toArray(m.getFrom())) {
            computeStats(m.getDate(), from.toString(), to, cc);
        }
        
        // TODO call this async and less often?
        flush();
    }

    /** Flush in-memory data to permanent storage */
    public void flush() {
        try {
            ResourceResolver resolver = null;
            try {
                resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                for(DataRecord r : data.values()) {
                    // Each org gets its own resource under our root
                    log.info("Storing {} at path {}", r, r.getPath());
                    ResourceUtil.getOrCreateResource(resolver, ROOT_PATH, DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, false);
                    ResourceUtil.getOrCreateResource(resolver, r.getOrgPath(), DESTINATION_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, false);
                    
                    // Properties are the message counts per source for this destination
                    final Map<String, Object> data = new HashMap<String, Object>();
                    for(Map.Entry<String, Integer> e : r.getSourceCounts().entrySet()) {
                        data.put(e.getKey(), e.getValue());
                    }
                    data.put(PERIOD_PROP, r.timestampPath);
                    data.put("sling:resourceType", DATA_RESOURCE_TYPE);
                    
                    // TODO for now this overwrites existing values,
                    // need to combine existing and new
                    ResourceUtil.getOrCreateResource(resolver, r.getPath(), data, DEFAULT_RESOURCE_TYPE, false);
                    
                }
                data.clear();
            } finally {
                if(resolver != null) {
                    resolver.commit();
                    resolver.close();
                }
            }
        } catch(Exception e) {
            log.warn("Exception in flush()", e);
        }
    }
    
    // TODO don't we have a utility for that?
    static String makeJcrFriendly(String s) {
        return s.replaceAll("[\\s\\.-]", "_").replaceAll("\\W", "").replaceAll("\\_", " ").trim().replaceAll(" ", "_");
    }

    static String [] toArray(AbstractList<?> list) {
        if(list == null) {
            return null;
        }
        final List<String> result = new ArrayList<String>();
        for(Object o : list) {
            result.add(o.toString());
        }
        return result.toArray(EMPTY_STRING_ARRAY);
    }

}
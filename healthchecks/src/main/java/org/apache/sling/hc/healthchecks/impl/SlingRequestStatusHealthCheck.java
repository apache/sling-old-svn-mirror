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
package org.apache.sling.hc.healthchecks.impl;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.Constants;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.healthchecks.util.FormattingResultLog;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks the HTTP status of Sling requests */
@Component(
        name="org.apache.sling.hc.SlingRequestStatusHealthCheck",
        configurationFactory=true, 
        policy=ConfigurationPolicy.REQUIRE, 
        metatype=true)
@Service
public class SlingRequestStatusHealthCheck implements HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(SlingRequestStatusHealthCheck.class);
    private Map<String, String> info;
    private String [] paths;
    
    static class PathSpec {
        int status;
        String path;
        
        PathSpec(String configuredPath) {
            path = configuredPath;
            status = 200;
            
            final String [] parts  = configuredPath.split(":");
            if(parts.length == 2) {
                try {
                    status = Integer.valueOf(parts[1].trim());
                    path = parts[0].trim();
                } catch(NumberFormatException nfe) {
                    log.warn("NumberFormatException while parsing [{}], invalid status value?", configuredPath);
                }
            } 
        }
    }
    
    @Property(cardinality=Integer.MAX_VALUE)
    public static final String PROP_PATH = "path";
    
    @Property(cardinality=50)
    public static final String PROP_TAGS = Constants.HC_TAGS;
    
    @Property
    public static final String PROP_NAME = Constants.HC_NAME;
    
    @Property
    public static final String PROP_MBEAN_NAME = Constants.HC_MBEAN_NAME;
    
    @Reference
    private SlingRequestProcessor requestProcessor;
    
    @Reference
    private ResourceResolverFactory resolverFactory;  
    
    @Activate
    public void activate(ComponentContext ctx) {
        info = new HealthCheckInfo(ctx.getProperties());
        paths = PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_PATH), new String [] {});
        log.info("Activated, paths={}", Arrays.asList(paths));
    }
    
    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        
        ResourceResolver resolver = null;
        int checked = 0;
        int failed = 0;
        String lastPath = null;
        
        try {
            resolver = resolverFactory.getAdministrativeResourceResolver(null);
            for(String p : paths) {
                lastPath = p;
                final PathSpec ps = new PathSpec(p);
                final HttpServletRequest request = new InternalRequest(ps.path);
                final InternalResponse response = new InternalResponse();
                requestProcessor.processRequest(request, response, resolver);
                final int status = response.getStatus();
                if(status != ps.status) {
                    failed++;
                    resultLog.warn("[{}] returns status {}, expected {}", new Object[] { ps.path, status, ps.status });
                } else {
                    resultLog.debug("[{}] returns status {} as expected", ps.path, status);
                }
                checked++;
            }
        } catch(Exception e) {
            resultLog.warn("Exception while executing request [{}]: {}", lastPath, e);
        } finally {
            if(resolver != null) {
                resolver.close();
            }
        }
        
        if(checked == 0) {
            resultLog.warn("No paths checked, empty paths list?");
        } else {
            resultLog.debug("{} paths checked, {} failures", checked, failed);
        }
        
        return new Result(resultLog);
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}
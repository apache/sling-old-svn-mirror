/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.crankstart.testservices;

import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/** Servlet that requires a configuration, used to test the
 *  Crankstart initial config feature with factory configs
 */
@Component(immediate=true,configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true)
@Service(value=Servlet.class)
@Reference(name="httpService",referenceInterface=HttpService.class)
public class ConfigFactoryServlet extends TestServlet {
    private static final long serialVersionUID = -6918378772515948579L;
    
    @Property(value="default message")
    protected static final String PROP_MESSAGE = "message";
    
    @Property(value="/default_path")
    protected static final String PROP_PATH = "path";
    
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException, NamespaceException {
        message = PropertiesUtil.toString(config.get(PROP_MESSAGE), "no message");
        path = PropertiesUtil.toString(config.get(PROP_PATH), "no path");
        register();
    }
    
    @Deactivate
    protected void deactivate(Map<String, Object> config) throws ServletException, NamespaceException {
        unregister();
    }
}

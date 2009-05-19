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
package org.apache.sling.osgi.installer.impl;

import java.util.LinkedList;

import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.apache.sling.osgi.installer.OsgiResourceProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** List of OsgiResourceProcessor, initialized with our
 *  set of processors.
 */
@SuppressWarnings("serial")
class OsgiResourceProcessorList extends LinkedList<OsgiResourceProcessor> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
	OsgiResourceProcessorList(BundleContext ctx, PackageAdmin pa, StartLevel sa, OsgiControllerServices sp) {
        add(new BundleResourceProcessor(ctx, pa, sa));
        add(new ConfigResourceProcessor(sp));
	}
	
	OsgiResourceProcessor getProcessor(String uri, InstallableData data) {
        OsgiResourceProcessor result = null;

        for(OsgiResourceProcessor p : this) {
            if(p.canProcess(uri, data)) {
                result = p;
                break;
            }
        }

        if(result == null) {
            log.debug("No processor found for resource {}", uri);
        }

        return result;
	}
}
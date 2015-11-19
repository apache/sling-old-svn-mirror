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
package org.apache.sling.discovery.base.its.setup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.base.its.setup.mock.MockFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGiMock {

    private static final Logger logger = LoggerFactory.getLogger(OSGiMock.class);

    private final List<Object> services = new LinkedList<Object>();

    public void addService(Object service) {
        if (service==null) {
            throw new IllegalArgumentException("service must not be null");
        }
        services.add(service);
    }
    
    public void activateAll() throws Exception {
        for (@SuppressWarnings("rawtypes")
        Iterator it = services.iterator(); it.hasNext();) {
            Object aService = it.next();

            activate(aService);
        }
    }

	public static void activate(Object aService) throws IllegalAccessException,
			InvocationTargetException {
	    Class<?> clazz = aService.getClass();
	    while (clazz != null) {
	        Method[] methods = clazz.getDeclaredMethods();
	        for (int i = 0; i < methods.length; i++) {
	            Method method = methods[i];
	            if (method.getName().equals("activate")) {
	                method.setAccessible(true);
	                if ( method.getParameterTypes().length == 0 ) {
	                    logger.info("activate: activating "+aService+"...");
	                    method.invoke(aService, null);
	                    logger.info("activate: activating "+aService+" done.");
	                } else if (method.getParameterTypes().length==1 && (method.getParameterTypes()[0]==ComponentContext.class)){
	                    logger.info("activate: activating "+aService+"...");
	                    method.invoke(aService, MockFactory.mockComponentContext());
	                    logger.info("activate: activating "+aService+" done.");
	                } else if (method.getParameterTypes().length==1 && (method.getParameterTypes()[0]==BundleContext.class)){
	                    logger.info("activate: activating "+aService+"...");
	                    method.invoke(aService, MockFactory.mockBundleContext());
	                    logger.info("activate: activating "+aService+" done.");
	                } else {
	                    throw new IllegalStateException("unsupported activate variant: "+method);
	                }
	                return;
	            }
	        }
	        clazz = clazz.getSuperclass();
	    }
	}

	public void deactivateAll() throws Exception {
        for (@SuppressWarnings("rawtypes")
        Iterator it = services.iterator(); it.hasNext();) {
            Object aService = it.next();

            deactivate(aService);
        }
	}

	public static void deactivate(Object aService) throws IllegalAccessException,
			InvocationTargetException {
        Class<?> clazz = aService.getClass();
        while (clazz != null) {
    		Method[] methods = clazz.getDeclaredMethods();
    		for (int i = 0; i < methods.length; i++) {
    		    Method method = methods[i];
    		    if (method.getName().equals("deactivate")) {
    		        method.setAccessible(true);
    		        if ( method.getParameterTypes().length == 0 ) {
    		            method.invoke(aService, null);
    		        } else {
    		            method.invoke(aService, MockFactory.mockComponentContext());
    		        }
    		        return;
    		    }
    		}
            clazz = clazz.getSuperclass();
        }
	}

    public void addServices(Object[] additionalServices) {
        if (additionalServices==null) {
            return;
        }
        for (Object additionalService : additionalServices) {
            addService(additionalService);
        }
    }
}

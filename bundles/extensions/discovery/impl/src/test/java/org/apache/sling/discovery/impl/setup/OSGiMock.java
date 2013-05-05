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
package org.apache.sling.discovery.impl.setup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;

public class OSGiMock {

    private final List<Object> services = new LinkedList<Object>();

    public void addService(Object service) {
        services.add(service);
    }

    public void activateAll(boolean resetRepo) throws Exception {
        if (resetRepo) {
            Session l = RepositoryProvider.instance().getRepository()
                    .loginAdministrative(null);
            try {
                l.removeItem("/var");
                l.save();
                l.logout();
            } catch (Exception e) {
                l.refresh(false);
                l.logout();
            }
        }

        for (@SuppressWarnings("rawtypes")
        Iterator it = services.iterator(); it.hasNext();) {
            Object aService = it.next();

            activate(aService);
        }
    }

	public static void activate(Object aService) throws IllegalAccessException,
			InvocationTargetException {
		Method[] methods = aService.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
		    Method method = methods[i];
		    if (method.getName().equals("activate")) {
		        method.setAccessible(true);
		        if ( method.getParameterTypes().length == 0 ) {
		            method.invoke(aService, null);
		        } else {
		            method.invoke(aService, MockFactory.mockComponentContext());
		        }
		    }
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
		Method[] methods = aService.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
		    Method method = methods[i];
		    if (method.getName().equals("deactivate")) {
		        method.setAccessible(true);
		        if ( method.getParameterTypes().length == 0 ) {
		            method.invoke(aService, null);
		        } else {
		            method.invoke(aService, MockFactory.mockComponentContext());
		        }
		    }
		}
	}
}

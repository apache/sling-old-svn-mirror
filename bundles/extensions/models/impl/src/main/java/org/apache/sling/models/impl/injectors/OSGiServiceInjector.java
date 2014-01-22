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
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Filter;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class OSGiServiceInjector implements Injector {

    private static final Logger log = LoggerFactory.getLogger(OSGiServiceInjector.class);

    private BundleContext bundleContext;

    @Override
    public String getName() {
        return "osgi-services";
    }

    @Activate
    public void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        Filter filter = element.getAnnotation(Filter.class);
        String filterString = null;
        if (filter != null) {
            filterString = filter.value();
        }

        return getValue(adaptable, type, filterString, callbackRegistry);
    }

    private <T> Object getService(Object adaptable, Class<T> type, String filter, DisposalCallbackRegistry callbackRegistry) {
        SlingScriptHelper helper = getScriptHelper(adaptable);

        if (helper != null) {
            T[] services = helper.getServices(type, filter);
            if (services == null || services.length == 0) {
                return null;
            } else {
                return services[0];
            }
        } else {
            try {
                ServiceReference[] refs = bundleContext.getServiceReferences(type.getName(), filter);
                if (refs == null || refs.length == 0) {
                    return null;
                } else {
                    callbackRegistry.addDisposalCallback(new Callback(refs, bundleContext));
                    return bundleContext.getService(refs[0]);
                }
            } catch (InvalidSyntaxException e) {
                log.error("invalid filter expression", e);
                return null;
            }
        }
    }

    private <T> Object[] getServices(Object adaptable, Class<T> type, String filter, DisposalCallbackRegistry callbackRegistry) {
        SlingScriptHelper helper = getScriptHelper(adaptable);

        if (helper != null) {
            T[] services = helper.getServices(type, filter);
            return services;
        } else {
            try {
                ServiceReference[] refs = bundleContext.getServiceReferences(type.getName(), filter);
                if (refs == null || refs.length == 0) {
                    return null;
                } else {
                    callbackRegistry.addDisposalCallback(new Callback(refs, bundleContext));
                    List<Object> services = new ArrayList<Object>();
                    for (ServiceReference ref : refs) {
                        Object service = bundleContext.getService(ref);
                        if (service != null) {
                            services.add(service);
                        }
                    }
                    return services.toArray();
                }
            } catch (InvalidSyntaxException e) {
                log.error("invalid filter expression", e);
                return null;
            }
        }
    }

    private SlingScriptHelper getScriptHelper(Object adaptable) {
        if (adaptable instanceof ServletRequest) {
            ServletRequest request = (ServletRequest) adaptable;
            SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
            if (bindings != null) {
                return bindings.getSling();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Object getValue(Object adaptable, Type type, String filterString, DisposalCallbackRegistry callbackRegistry) {
        if (type instanceof Class) {
            Class<?> injectedClass = (Class<?>) type;
            if (injectedClass.isArray()) {
                Object[] services = getServices(adaptable, injectedClass.getComponentType(), filterString, callbackRegistry);
                Object arr = Array.newInstance(injectedClass.getComponentType(), services.length);
                for (int i = 0; i < services.length; i++) {
                    Array.set(arr, i, services[i]);
                }
                return arr;
            } else {
                return getService(adaptable, injectedClass, filterString, callbackRegistry);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            if (ptype.getActualTypeArguments().length != 1) {
                return null;
            }
            Class<?> collectionType = (Class<?>) ptype.getRawType();
            if (!(collectionType.equals(Collection.class) || collectionType.equals(List.class))) {
                return null;
            }

            Class<?> serviceType = (Class<?>) ptype.getActualTypeArguments()[0];
            Object[] services = getServices(adaptable, serviceType, filterString, callbackRegistry);
            return Arrays.asList(services);
        } else {
            log.warn("Cannot handle type {}", type);
            return null;
        }
    }
    
    private static class Callback implements DisposalCallback {
        private final ServiceReference[] refs;
        private final BundleContext context;
        
        public Callback(ServiceReference[] refs, BundleContext context) {
            this.refs = refs;
            this.context = context;
        }
        
        @Override
        public void onDisposed() {
            if (refs != null) {
                for (ServiceReference ref : refs) {
                    context.ungetService(ref);
                }
            }
        }
    }

}

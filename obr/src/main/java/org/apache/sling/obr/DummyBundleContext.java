/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.obr;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>DummyBundleContext</code> is used for the resolution of bundles
 * from the local repository to create a subset repository for download.
 * 
 * @author fmeschbe
 */
class DummyBundleContext implements BundleContext {

    private BundleContext delegatee;
    
    private Map properties;

    DummyBundleContext(BundleContext delegatee) {
        this.delegatee = delegatee;
        this.properties = new HashMap();
    }

    public Filter createFilter(String arg0) throws InvalidSyntaxException {
        return delegatee.createFilter(arg0);
    }

    public ServiceReference[] getAllServiceReferences(String arg0, String arg1)
            throws InvalidSyntaxException {
        return delegatee.getAllServiceReferences(arg0, arg1);
    }

    public Bundle getBundle() {
        return delegatee.getBundle();
    }

    public Bundle getBundle(long id) {
        return (id == 0) ? delegatee.getBundle(id) : null;
    }

    public Bundle[] getBundles() {
        return new Bundle[]{ delegatee.getBundle(0) };
    }

    public File getDataFile(String arg0) {
        return delegatee.getDataFile(arg0);
    }

    void setProperty(String name, String value) {
        properties.put(name, value);
    }
    
    public String getProperty(String name) {
        if (properties.containsKey(name)) {
            return String.valueOf(properties.get(name));
        }
        
        return delegatee.getProperty(name);
    }

    public Object getService(ServiceReference arg0) {
        return delegatee.getService(arg0);
    }

    public ServiceReference getServiceReference(String arg0) {
        return delegatee.getServiceReference(arg0);
    }

    public ServiceReference[] getServiceReferences(String arg0, String arg1)
            throws InvalidSyntaxException {
        return delegatee.getServiceReferences(arg0, arg1);
    }

    public Bundle installBundle(String arg0, InputStream arg1)
            throws BundleException {
        return delegatee.installBundle(arg0, arg1);
    }

    public Bundle installBundle(String arg0) throws BundleException {
        return delegatee.installBundle(arg0);
    }

    public ServiceRegistration registerService(String arg0, Object arg1,
            Dictionary arg2) {
        return delegatee.registerService(arg0, arg1, arg2);
    }

    public ServiceRegistration registerService(String[] arg0, Object arg1,
            Dictionary arg2) {
        return delegatee.registerService(arg0, arg1, arg2);
    }

    public boolean ungetService(ServiceReference arg0) {
        return delegatee.ungetService(arg0);
    }


    // listener support, not really
    public void addBundleListener(BundleListener arg0) {
        // delegatee.addBundleListener(arg0);
    }

    public void addFrameworkListener(FrameworkListener arg0) {
//        delegatee.addFrameworkListener(arg0);
    }

    public void addServiceListener(ServiceListener arg0, String arg1) {
//        delegatee.addServiceListener(arg0, arg1);
    }

    public void addServiceListener(ServiceListener arg0) {
//        delegatee.addServiceListener(arg0);
    }

    public void removeBundleListener(BundleListener arg0) {
//        delegatee.removeBundleListener(arg0);
    }

    public void removeFrameworkListener(FrameworkListener arg0) {
//        delegatee.removeFrameworkListener(arg0);
    }

    public void removeServiceListener(ServiceListener arg0) {
//        delegatee.removeServiceListener(arg0);
    }

}

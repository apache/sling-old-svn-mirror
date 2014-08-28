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
package org.apache.sling.commons.mime.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private ServiceRegistration reg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        try {
            final Object provider = new TikaMimeTypeProvider();
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Tika MIME Type Provider");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

            reg = context.registerService(MimeTypeProvider.class.getName(), provider, props);

            logger.debug("Registered Apache Tika mime type provider");
        } catch (final Throwable t) {
            logger.debug("Unable to register Apache Tika mime type provider", t);
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        if ( reg != null ) {
            reg.unregister();
            reg = null;
        }
    }

}

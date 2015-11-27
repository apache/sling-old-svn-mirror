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
package org.apache.sling.startup.configurable;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.launchpad.api.StartupService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    property = Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
)
@Designate(ocd = ConfigurableStartupHandlerConfiguration.class)
public class ConfigurableStartupHandler implements StartupHandler {

    private ConfigurationAdmin configurationAdmin;

    private BundleContext bundleContext;

    private ServiceListener serviceListener = serviceEvent -> evaluate();

    private ServiceTracker<StartupListener, StartupListener> serviceTracker;

    private ServiceRegistration startupServiceRegistration;

    private StartupMode startupMode;

    private String[] requiredServices;

    private boolean reconfigureStartupModeToRestartOnFinish;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    private final Object lock = new Object();

    private static final String SERVICES_FILTERS_TEMPLATE = "(|%s)";

    private final Logger logger = LoggerFactory.getLogger(ConfigurableStartupHandler.class);

    public ConfigurableStartupHandler() {
    }

    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Activate
    private void activate(final BundleContext bundleContext, final ConfigurableStartupHandlerConfiguration configuration) {
        logger.info("activating with Startup Mode '{}' and required services '{}'", configuration.startupMode(), configuration.requiredServices());
        this.bundleContext = bundleContext;
        configure(configuration);
        serviceTracker = new ServiceTracker<>(bundleContext, StartupListener.class, new StartupListenerServiceTrackerCustomizer(bundleContext));
        serviceTracker.open(true);
        evaluate();
    }

    @Modified
    private void modified(final ConfigurableStartupHandlerConfiguration configuration) {
        logger.info("modifying with Startup Mode '{}' and required services '{}'", configuration.startupMode(), configuration.requiredServices());
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.info("deactivating");
        serviceTracker.close();
        synchronized (lock) {
            bundleContext.removeServiceListener(serviceListener);
            startupServiceRegistration.unregister();
            startupServiceRegistration = null;
            bundleContext = null;
        }
    }

    @Override
    public StartupMode getMode() {
        return startupMode;
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public void waitWithStartup(boolean flag) {
        logger.warn("Waiting with startup is not supported by this startup handler. Ignoring flag '{}'.", flag);
    }

    private void configure(final ConfigurableStartupHandlerConfiguration configuration) {
        synchronized (lock) {
            startupMode = configuration.startupMode();
            requiredServices = configuration.requiredServices();
            reconfigureStartupModeToRestartOnFinish = configuration.reconfigureStartupModeToRestartOnFinish();
            bundleContext.removeServiceListener(serviceListener);
            final StringBuilder filters = new StringBuilder();
            for (final String clazz : requiredServices) {
                filters.append(String.format("(%s=%s)", Constants.OBJECTCLASS, clazz));
            }
            final String filter = String.format(SERVICES_FILTERS_TEMPLATE, filters);
            try {
                bundleContext.addServiceListener(serviceListener, filter);
            } catch (InvalidSyntaxException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void evaluate() {
        logger.info("evaluating");
        synchronized (lock) {
            if (!finished.get()) {
                for (final String clazz : requiredServices) {
                    if (bundleContext.getServiceReference(clazz) == null) {
                        logger.info("Required service of type '{}' is missing.", clazz);
                        return;
                    } else {
                        logger.info("Required service of type '{}' is found.", clazz);
                    }
                }
                finished.set(true);
                for (final StartupListener startupListener : serviceTracker.getServices(new StartupListener[0])) {
                    try {
                        logger.info("Notifying {} about finished startup ('{}').", startupListener, startupMode);
                        startupListener.startupFinished(startupMode);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                registerStartupService();
                if (reconfigureStartupModeToRestartOnFinish) {
                    reconfigureStartupMode();
                }
            }
        }
    }

    private void registerStartupService() {
        logger.info("registering Startup Service with Startup Mode '{}'", startupMode);
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(StartupMode.class.getName(), startupMode.name());
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Startup Service");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        final StartupService startupService = new ConfigurableStartupService(startupMode);
        startupServiceRegistration = bundleContext.registerService(StartupService.class, startupService, properties);
    }

    private void reconfigureStartupMode() {
        try {
            final String pid = getClass().getName();
            final Configuration configuration = configurationAdmin.getConfiguration(pid);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                properties = new Hashtable<>();
            }
            if (!StartupMode.RESTART.name().equals(properties.get("startupMode"))) {
                properties.put("startupMode", StartupMode.RESTART.name());
                configuration.update(properties);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private class StartupListenerServiceTrackerCustomizer implements ServiceTrackerCustomizer<StartupListener, StartupListener> {

        private final BundleContext bundleContext;

        public StartupListenerServiceTrackerCustomizer(final BundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }

        @Override
        public StartupListener addingService(final ServiceReference<StartupListener> serviceReference) {
            final StartupListener startupListener = bundleContext.getService(serviceReference);
            if (startupListener != null) {
                try {
                    logger.info("Informing {} about startup ('{}', '{}').", new Object[]{startupListener, startupMode, finished});
                    startupListener.inform(startupMode, finished.get());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return startupListener;
        }

        @Override
        public void modifiedService(final ServiceReference<StartupListener> serviceReference, final StartupListener startupListener) {
        }

        @Override
        public void removedService(final ServiceReference<StartupListener> serviceReference, final StartupListener startupListener) {
            bundleContext.ungetService(serviceReference);
        }

    }

}

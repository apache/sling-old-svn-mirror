package org.apache.sling.commons.log.logback.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.gaffer.GafferUtil;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextAwareBase;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.EnvUtil;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.GenericConfigurator;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusListenerAsList;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

public class LogbackManager extends LoggerContextAwareBase {
    private static final String PREFIX = "org.apache.sling.commons.log";

    private static final String DEBUG = PREFIX + "." + "debug";

    private static final String PLUGIN_URL = "slinglog";

    private static final String PRINTER_URL = "slinglogs";

    private static final String RESET_EVENT_TOPIC = "org/apache/sling/commons/log/RESET";

    private final String rootDir;

    private final String contextName = "sling";

    private final LogConfigManager logConfigManager;

    private final List<LogbackResetListener> resetListeners = new ArrayList<LogbackResetListener>();

    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Acts as a bridge between Logback and OSGi
     */
    private final LoggerContextListener osgiIntegrationListener = new OsgiIntegrationListener();

    private final boolean debug;

    private final boolean started;

    private final Semaphore resetLock = new Semaphore(1);

    private final AtomicBoolean configChanged = new AtomicBoolean();

    private final AppenderTracker appenderTracker;

    private final ConfigSourceTracker configSourceTracker;

    private final FilterTracker filterTracker;

    private final TurboFilterTracker turboFilterTracker;

    private final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    private final List<ServiceTracker> serviceTrackers = new ArrayList<ServiceTracker>();

    /**
     * Time at which reset started. Used as the threshold for logging error
     * messages from status printer
     */
    private volatile long resetStartTime;

    public LogbackManager(BundleContext bundleContext) throws InvalidSyntaxException {
        final long startTime = System.currentTimeMillis();
        setLoggerContext((LoggerContext) LoggerFactory.getILoggerFactory());

        this.rootDir = getRootDir(bundleContext);

        this.debug = Boolean.parseBoolean(bundleContext.getProperty(DEBUG));

        this.appenderTracker = new AppenderTracker(bundleContext, getLoggerContext());
        this.configSourceTracker = new ConfigSourceTracker(bundleContext, this);
        this.filterTracker = new FilterTracker(bundleContext,this);
        this.turboFilterTracker = new TurboFilterTracker(bundleContext,getLoggerContext());
        // TODO Make it configurable
        // TODO: what should it be ?
        getLoggerContext().setName(contextName);
        this.logConfigManager = new LogConfigManager(getLoggerContext(), bundleContext, rootDir, this);

        resetListeners.add(logConfigManager);
        resetListeners.add(appenderTracker);
        resetListeners.add(configSourceTracker);
        resetListeners.add(filterTracker);
        resetListeners.add(turboFilterTracker);

        //Record trackers for shutdown later
        serviceTrackers.add(appenderTracker);
        serviceTrackers.add(configSourceTracker);
        serviceTrackers.add(filterTracker);
        serviceTrackers.add(turboFilterTracker);

        getLoggerContext().addListener(osgiIntegrationListener);

        configure();
        registerWebConsoleSupport(bundleContext);
        registerEventHandler(bundleContext);
        StatusPrinter.printInCaseOfErrorsOrWarnings(getLoggerContext(), startTime);
        started = true;
    }

    public void shutdown() {
        logConfigManager.close();

        for(ServiceTracker tracker : serviceTrackers){
            tracker.close();
        }

        for (ServiceRegistration reg : registrations) {
            reg.unregister();
        }

        getLoggerContext().removeListener(osgiIntegrationListener);

        getLoggerContext().stop();
    }

    public void configChanged() {
        if (!started) {
            return;
        }

        if (resetLock.tryAcquire()) {
            scheduleConfigReload();
        } else {
            configChanged.set(true);
            addInfo("LoggerContext reset in progress. Marking config changed to true");
        }
    }

    public void fireResetCompleteListeners(){
        for(LogbackResetListener listener : resetListeners){
            listener.onResetComplete(getLoggerContext());
        }
    }

    public LogConfigManager getLogConfigManager() {
        return logConfigManager;
    }

    public AppenderTracker getAppenderTracker() {
        return appenderTracker;
    }

    public ConfigSourceTracker getConfigSourceTracker() {
        return configSourceTracker;
    }

    public void addSubsitutionProperties(InterpretationContext ic) {
        ic.addSubstitutionProperty("sling.home", rootDir);
    }

    public URL getDefaultConfig() {
        return getClass().getClassLoader().getResource("logback-empty.xml");
    }

    private void configure() {
        ConfiguratorCallback cb = new DefaultCallback();

        // Check first for an explicit configuration file
        File configFile = logConfigManager.getLogbackConfigFile();
        if (configFile != null) {
            cb = new FilenameConfiguratorCallback(configFile);
        }

        configure(cb);
    }

    private void configure(ConfiguratorCallback cb) {
        StatusListener statusListener = new StatusListenerAsList();
        if (debug) {
            statusListener = new OnConsoleStatusListener();
        }

        getStatusManager().add(statusListener);
        addInfo("Resetting context: " + getLoggerContext().getName());
        resetContext(statusListener);

        StatusUtil statusUtil = new StatusUtil(getLoggerContext());
        JoranConfigurator configurator = createConfigurator();
        final List<SaxEvent> eventList = configurator.recallSafeConfiguration();
        final long threshold = System.currentTimeMillis();
        boolean success = false;
        try {
            cb.perform(configurator);
            if (statusUtil.hasXMLParsingErrors(threshold)) {
                cb.fallbackConfiguration(eventList, createConfigurator(), statusListener);
            }
            addInfo("Context: " + getLoggerContext().getName() + " reloaded.");
            success = true;
        } catch (Throwable t) {
            //Need to catch any error as Logback must work in all scenarios
            addError("Error configuring Logback",t);
        } finally {
            if(!success){
                cb.fallbackConfiguration(eventList, createConfigurator(), statusListener);
            }
            getStatusManager().remove(statusListener);
            StatusPrinter.printInCaseOfErrorsOrWarnings(getLoggerContext(), resetStartTime);
        }
    }

    private JoranConfigurator createConfigurator() {
        SlingConfigurator configurator = new SlingConfigurator();
        configurator.setContext(getLoggerContext());
        return configurator;
    }

    private void resetContext(StatusListener statusListener) {
        getLoggerContext().reset();

        // after a reset the statusListenerAsList gets removed as a listener
        if (statusListener != null && !getStatusManager().getCopyOfStatusListenerList().contains(statusListener)) {
            getStatusManager().add(statusListener);
        }
    }

    private void scheduleConfigReload() {
        getLoggerContext().getExecutorService().submit(new LoggerReconfigurer());
    }

    private String getRootDir(BundleContext bundleContext) {
        String rootDir = bundleContext.getProperty("sling.home");
        if (rootDir == null) {
            rootDir = new File(".").getAbsolutePath();
        }
        addInfo("Using rootDir as " + rootDir);
        return rootDir;
    }

    private class LoggerReconfigurer implements Runnable {

        public void run() {
            // TODO Might be better to run a job to monitor refreshRequirement
            boolean configChanged = false;
            try {
                addInfo("Performing configuration");
                configure();
                configChanged = LogbackManager.this.configChanged.getAndSet(false);
                if (configChanged) {
                    scheduleConfigReload();
                }
            } catch (Exception e) {
                log.warn("Error occurred while re-configuring logger", e);
                addError("Error occurred while re-configuring logger", e);
            } finally {
                if (!configChanged) {
                    resetLock.release();
                    addInfo("Re configuration done");
                }
            }
        }
    }

    // ~-------------------------------LogggerContextListener

    private class OsgiIntegrationListener implements LoggerContextListener {

        public boolean isResetResistant() {
            // The integration listener has to survive resets from other causes
            // like reset when Logback detects change in config file and reloads
            // on
            // on its own in ReconfigureOnChangeFilter
            return true;
        }

        public void onStart(LoggerContext context) {
        }

        public void onReset(LoggerContext context) {
            addInfo("OsgiIntegrationListener : context reset detected. Adding LogManager to context map and firing"
                + " listeners");

            // Attach a console appender to handle logging untill we configure
            // one. This would be removed in LogConfigManager.reset
            final Logger rootLogger = getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.INFO);
            rootLogger.addAppender(logConfigManager.getDefaultAppender());


            // Now record the time of reset with a default appender attached to
            // root logger. We also add a milli second extra to account for logs which would have
            // got fired in same duration
            resetStartTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.toMillis(1);

            context.putObject(LogbackManager.class.getName(), LogbackManager.this);
            for (LogbackResetListener l : resetListeners) {
                l.onResetStart(context);
            }
        }

        public void onStop(LoggerContext context) {
        }

        public void onLevelChange(Logger logger, Level level) {
        }

    }

    // ~--------------------------------Configurator Base

    private class SlingConfigurator extends JoranConfigurator {

        @Override
        protected void buildInterpreter() {
            super.buildInterpreter();
            addSubsitutionProperties(interpreter.getInterpretationContext());
        }
    }

    // ~--------------------------------Configuration Support

    private abstract class ConfiguratorCallback {
        abstract void perform(JoranConfigurator configurator) throws JoranException;

        /**
         * Logic based on
         * ch.qos.logback.classic.turbo.ReconfigureOnChangeFilter.
         * ReconfiguringThread
         */
        public void fallbackConfiguration(List<SaxEvent> eventList, JoranConfigurator configurator,
                StatusListener statusListener) {
            URL mainURL = getMainUrl();
            if (mainURL != null) {
                if (eventList != null) {
                    addWarn("Falling back to previously registered safe configuration.");
                    try {
                        resetContext(statusListener);
                        GenericConfigurator.informContextOfURLUsedForConfiguration(context, mainURL);
                        configurator.doConfigure(eventList);
                        addInfo("Re-registering previous fallback configuration once more as a fallback configuration point");
                        configurator.registerSafeConfiguration();
                    } catch (JoranException e) {
                        addError("Unexpected exception thrown by a configuration considered safe.", e);
                    }
                } else {
                    addWarn("No previous configuration to fall back on.");
                }
            }
        }

        protected URL getMainUrl() {
            return null;
        }
    }

    private class FilenameConfiguratorCallback extends ConfiguratorCallback {
        private final File configFile;

        public FilenameConfiguratorCallback(File configFile) {
            this.configFile = configFile;
        }

        public void perform(JoranConfigurator configurator) throws JoranException {
            final String path = configFile.getAbsolutePath();
            addInfo("Configuring from " + path);
            if (configFile.getName().endsWith("xml")) {
                configurator.doConfigure(configFile);
            } else if (configFile.getName().endsWith("groovy")) {
                if (EnvUtil.isGroovyAvailable()) {
                    // avoid directly referring to GafferConfigurator so as to
                    // avoid
                    // loading groovy.lang.GroovyObject . See also
                    // http://jira.qos.ch/browse/LBCLASSIC-214
                    GafferUtil.runGafferConfiguratorOn(getLoggerContext(), this, configFile);
                } else {
                    addError("Groovy classes are not available on the class path. ABORTING INITIALIZATION.");
                }
            }
        }

        @Override
        protected URL getMainUrl() {
            try {
                return configFile.toURI().toURL();
            } catch (MalformedURLException e) {
                addWarn("Cannot convert file to url " + configFile.getAbsolutePath(), e);
                return null;
            }
        }
    }

    private class DefaultCallback extends ConfiguratorCallback {
        public void perform(JoranConfigurator configurator) throws JoranException {
            configurator.doConfigure(getMainUrl());
        }

        @Override
        protected URL getMainUrl() {
            return getDefaultConfig();
        }
    }

    // ~ ----------------------------------------------WebConsole Support

    public LoggerStateContext determineLoggerState() {
        final List<Logger> loggers = getLoggerContext().getLoggerList();
        final LoggerStateContext ctx = new LoggerStateContext(loggers);
        for (Logger logger : loggers) {
            if (logger.iteratorForAppenders().hasNext() || logger.getLevel() != null) {
                ctx.loggerInfos.add(logger);
            }

            Iterator<Appender<ILoggingEvent>> itr = logger.iteratorForAppenders();
            while (itr.hasNext()) {
                Appender<ILoggingEvent> a = itr.next();
                if (a.getName() != null && !ctx.appenders.containsKey(a.getName())) {
                    ctx.appenders.put(a.getName(), a);
                }
            }
        }
        return ctx;
    }

    public class LoggerStateContext {
        final LoggerContext loggerContext = getLoggerContext();

        final List<Logger> allLoggers;

        /**
         * List of logger which have explicitly defined level or appenders set
         */
        final List<Logger> loggerInfos = new ArrayList<Logger>();

        final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<String, Appender<ILoggingEvent>>();

        final Map<Appender<ILoggingEvent>, AppenderTracker.AppenderInfo> dynamicAppenders = new HashMap<Appender<ILoggingEvent>, AppenderTracker.AppenderInfo>();

        final Map<ServiceReference,TurboFilter> turboFilters;

        LoggerStateContext(List<Logger> allLoggers) {
            this.allLoggers = allLoggers;
            for (AppenderTracker.AppenderInfo ai : getAppenderTracker().getAppenderInfos()) {
                dynamicAppenders.put(ai.appender, ai);
            }
            this.turboFilters = turboFilterTracker.getFilters();
        }

        int getNumberOfLoggers() {
            return allLoggers.size();
        }

        int getNumOfDynamicAppenders() {
            return getAppenderTracker().getAppenderInfos().size();
        }

        int getNumOfAppenders() {
            return appenders.size();
        }

        boolean isDynamicAppender(Appender<ILoggingEvent> a) {
            return dynamicAppenders.containsKey(a);
        }

        ServiceReference getTurboFilterRef(TurboFilter tf){
            for(Map.Entry<ServiceReference,TurboFilter> e : turboFilters.entrySet()){
                if(e.getValue().equals(tf)){
                    return e.getKey();
                }
            }
            return null;
        }

        Collection<Appender<ILoggingEvent>> getAllAppenders() {
            return appenders.values();
        }

        Map<String,Appender<ILoggingEvent>> getAppenderMap(){
            return Collections.unmodifiableMap(appenders);
        }
    }

    private void registerWebConsoleSupport(BundleContext context) {
        final ServiceFactory serviceFactory = new PluginServiceFactory(PLUGIN_URL);

        Properties pluginProps = new Properties();
        pluginProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        pluginProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Support");
        pluginProps.put("felix.webconsole.label", PLUGIN_URL);
        pluginProps.put("felix.webconsole.title", "Log Support");
        pluginProps.put("felix.webconsole.category", "Sling");
        pluginProps.put("felix.webconsole.css", new String[] {
            "/" + PLUGIN_URL + "/res/ui/prettify.css", "/" + PLUGIN_URL + "/res/ui/log.css"
        });

        registrations.add(context.registerService("javax.servlet.Servlet", serviceFactory, pluginProps));

        Properties printerProps = new Properties();
        printerProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Configuration Printer");
        printerProps.put("felix.webconsole.label", PRINTER_URL);
        printerProps.put("felix.webconsole.title", "Log Files");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        // TODO need to see to add support for Inventory Feature
        registrations.add(context.registerService(SlingConfigurationPrinter.class.getName(),
            new SlingConfigurationPrinter(this), printerProps));
    }

    private class PluginServiceFactory implements ServiceFactory {
        private Object instance;

        private final String label;

        private PluginServiceFactory(String label) {
            this.label = label;
        }

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            synchronized (this) {
                if (this.instance == null) {
                    this.instance = new SlingLogPanel(LogbackManager.this, label);
                }
                return instance;
            }
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        }
    }

    private void registerEventHandler(BundleContext bundleContext) {
        Properties props = new Properties();
        props.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "Sling Log Reset Event Handler");
        props.put("event.topics", new String[] {
            RESET_EVENT_TOPIC
        });

        registrations.add(bundleContext.registerService("org.osgi.service.event.EventHandler", new ServiceFactory() {
            private Object instance;

            @Override
            public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
                synchronized (this) {
                    if (this.instance == null) {
                        this.instance = new ConfigResetRequestHandler(LogbackManager.this);
                    }
                    return instance;
                }
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o) {
            }
        }, props));
    }

}

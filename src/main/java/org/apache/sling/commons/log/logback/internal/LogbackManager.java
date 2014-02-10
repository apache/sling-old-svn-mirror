package org.apache.sling.commons.log.logback.internal;

import java.io.File;
import java.io.PrintStream;
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.gaffer.GafferUtil;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextAwareBase;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.EnvUtil;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.GenericConfigurator;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusListenerAsList;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.sling.commons.log.logback.internal.AppenderTracker.AppenderInfo;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class LogbackManager extends LoggerContextAwareBase {
    private static final String JUL_SUPPORT = "org.apache.sling.commons.log.julenabled";

    //These properties should have been defined in SlingLogPanel
    //But we need them while registering ServiceFactory and hence
    //would not want to load SlingLogPanel class for registration
    //purpose as we need to run in cases where Servlet classes
    //are not available
    static final String APP_ROOT = "slinglog";

    static final String RES_LOC = APP_ROOT + "/res/ui";

    public static final String[] CSS_REFS = {
            RES_LOC + "/jquery.autocomplete.css",
            RES_LOC + "/prettify.css",
            RES_LOC + "/log.css",
    };

    private static final String PREFIX = "org.apache.sling.commons.log";

    private static final String DEBUG = PREFIX + "." + "debug";

    private static final String PRINTER_URL = "slinglogs";

    private static final String RESET_EVENT_TOPIC = "org/apache/sling/commons/log/RESET";

    private final BundleContext bundleContext;

    private final String rootDir;

    private final String contextName = "sling";

    private final LogConfigManager logConfigManager;

    private final List<LogbackResetListener> resetListeners = new ArrayList<LogbackResetListener>();

    private final org.slf4j.Logger log;

    /**
     * Acts as a bridge between Logback and OSGi
     */
    private final LoggerContextListener osgiIntegrationListener = new OsgiIntegrationListener();

    private final boolean debug;

    private final boolean started;

    private final Semaphore resetLock = new Semaphore(1);

    private final Object configChangedFlagLock = new Object();

    private boolean configChanged = false;

    private final AppenderTracker appenderTracker;

    private final ConfigSourceTracker configSourceTracker;

    private final FilterTracker filterTracker;

    private final TurboFilterTracker turboFilterTracker;

    private final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    private final List<ServiceTracker> serviceTrackers = new ArrayList<ServiceTracker>();

    private final boolean bridgeHandlerInstalled;

    /**
     * Time at which reset started. Used as the threshold for logging error
     * messages from status printer
     */
    private volatile long resetStartTime;

    public LogbackManager(BundleContext bundleContext) throws InvalidSyntaxException {
        this.bundleContext = bundleContext;

        setLoggerContext((LoggerContext) LoggerFactory.getILoggerFactory());

        this.log = LoggerFactory.getLogger(getClass());
        this.rootDir = getRootDir(bundleContext);
        this.debug = Boolean.parseBoolean(bundleContext.getProperty(DEBUG));
        this.bridgeHandlerInstalled = installSlf4jBridgeHandler(bundleContext);

        this.appenderTracker = new AppenderTracker(bundleContext, getLoggerContext());
        this.configSourceTracker = new ConfigSourceTracker(bundleContext, this);
        this.filterTracker = new FilterTracker(bundleContext,this);
        this.turboFilterTracker = new TurboFilterTracker(bundleContext,getLoggerContext());

        getLoggerContext().setName(contextName);
        this.logConfigManager = new LogConfigManager(getLoggerContext(), bundleContext, rootDir, this);

        resetListeners.add(new LevelChangePropagatorChecker());
        resetListeners.add(logConfigManager);
        resetListeners.add(appenderTracker);
        resetListeners.add(configSourceTracker);
        resetListeners.add(filterTracker);
        resetListeners.add(turboFilterTracker);
        resetListeners.add(new RootLoggerListener()); //Should be invoked at last

        //Record trackers for shutdown later
        serviceTrackers.add(appenderTracker);
        serviceTrackers.add(configSourceTracker);
        serviceTrackers.add(filterTracker);
        serviceTrackers.add(turboFilterTracker);

        getLoggerContext().addListener(osgiIntegrationListener);
        registerWebConsoleSupport();
        registerEventHandler();

        started = true;
        configChanged();
    }

    public void shutdown() {
        if(bridgeHandlerInstalled){
            SLF4JBridgeHandler.uninstall();
        }

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

    //-------------------------------------- Config reset handling ----------

    public void configChanged() {
        if (!started) {
            return;
        }

        /*
        Logback reset cannot be done concurrently. So when Logback is being reset
        we note down any new request for reset. Later when the thread which performs
        reset finishes, then it checks if any request for reset pending. if yes
        then it again tries to reschedules a job to perform reset in rescheduleIfConfigChanged

        Logback reset is done under a lock 'resetLock' so that Logback
        is not reconfigured concurrently. Only the thread which acquires the
        'resetLock' can submit the task for reload (actual reload done async)

        Once the reload is done the lock is released in LoggerReconfigurer#run

        The way locking works is any thread which changes config
        invokes configChanged. Here two things are possible

        1. Log reset in progress i.e. resetLock already acquired
           In this case the thread would just set the 'configChanged' flag to true

        2. No reset in progress. Thread would acquire the  resetLock and submit the
          job to reset Logback


        Any such change is synchronized with configChangedFlagLock such that a request
         for config changed is not missed
        */

        synchronized (configChangedFlagLock){
            if (resetLock.tryAcquire()) {
                configChanged = false;
                scheduleConfigReload();
            } else {
                configChanged = true;
                addInfo("LoggerContext reset in progress. Marking config changed to true");
            }
        }
    }

    private void rescheduleIfConfigChanged(){
        synchronized (configChangedFlagLock){
            //If config changed then only acquire a lock
            //and proceed to reload
            if(configChanged){
                if(resetLock.tryAcquire()){
                    configChanged = false;
                    scheduleConfigReload();
                }
                //else some other thread acquired the resetlock
                //and reset is in progress. That job would
                //eventually call rescheduleIfConfigChanged again
                //and configChanged request would be taken care of
            }
        }
    }

    private void scheduleConfigReload() {
        getLoggerContext().getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                // TODO Might be better to run a job to monitor refreshRequirement
                try {
                    addInfo("Performing configuration");
                    configure();
                } catch (Exception e) {
                    log.warn("Error occurred while re-configuring logger", e);
                    addError("Error occurred while re-configuring logger", e);
                } finally {
                    resetLock.release();
                    addInfo("Re configuration done");
                    rescheduleIfConfigChanged();
                }
            }
        });
    }

    public void fireResetCompleteListeners(){
        for(LogbackResetListener listener : resetListeners){
            addInfo("Firing reset listener - onResetComplete "+listener.getClass());
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

    public String getRootDir() {
        return rootDir;
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
        long startTime = System.currentTimeMillis();
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
            //The error would be dumped to sysout in later call to Status printer
            addError("Error occurred while configuring Logback", t);
        } finally {
            if(!success){
                cb.fallbackConfiguration(eventList, createConfigurator(), statusListener);
            }
            getStatusManager().remove(statusListener);
            printInCaseOfErrorsOrWarnings(getLoggerContext(), resetStartTime, startTime);
        }
    }


    /**
     * Based on StatusPrinter. printInCaseOfErrorsOrWarnings. This has been adapted
     * to print more context i.e. some message from before the error message to better understand
     * the failure scenario
     *
     * @param threshold time since which the message have to be checked for errors/warnings
     * @param msgSince time form which we are interested in the message logs
     */
    private static void printInCaseOfErrorsOrWarnings(Context context, long threshold, long msgSince) {
        if (context == null) {
            throw new IllegalArgumentException("Context argument cannot be null");
        }
        PrintStream ps = System.out;
        StatusManager sm = context.getStatusManager();
        if (sm == null) {
            ps.println("WARN: Context named \"" + context.getName()
                    + "\" has no status manager");
        } else {
            StatusUtil statusUtil = new StatusUtil(context);
            if (statusUtil.getHighestLevel(threshold) >= ErrorStatus.WARN) {
                StatusPrinter.print(sm, msgSince);
            }
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

    private String getRootDir(BundleContext bundleContext) {
        String rootDir = bundleContext.getProperty("sling.home");
        if (rootDir == null) {
            rootDir = new File(".").getAbsolutePath();
        }
        addInfo("Using rootDir as " + rootDir);
        return rootDir;
    }

    //~-------------------------------------------------- Slf4j Bridge Handler Support

    /**
     * Installs the Slf4j BridgeHandler to route the JUL logs through Slf4j
     *
     * @return true only if the BridgeHandler is installed.
     */
    private static boolean installSlf4jBridgeHandler(BundleContext bundleContext){
        // SLING-2373
        if (Boolean.parseBoolean(bundleContext.getProperty(JUL_SUPPORT))) {
            // In config one must enable the LevelChangePropagator
            // http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
            // make sure configuration is empty unless explicitly set
            if (System.getProperty("java.util.logging.config.file") == null
                    && System.getProperty("java.util.logging.config.class") == null) {
                final Thread ct = Thread.currentThread();
                final ClassLoader old = ct.getContextClassLoader();
                try {
                    ct.setContextClassLoader(LogbackManager.class.getClassLoader());
                    System.setProperty("java.util.logging.config.class",
                            DummyLogManagerConfiguration.class.getName());
                    java.util.logging.LogManager.getLogManager().reset();
                } finally {
                    ct.setContextClassLoader(old);
                    System.clearProperty("java.util.logging.config.class");
                }
            }

            SLF4JBridgeHandler.install();
            return true;
        }
        return false;
    }

    /**
     * It checks if LevelChangePropagator is installed or not. If not then
     * it installs the propagator when Slf4j Bridge Handler is installed
     */
    private class LevelChangePropagatorChecker implements LogbackResetListener {

        @Override
        public void onResetStart(LoggerContext context) {

        }

        @Override
        public void onResetComplete(LoggerContext context) {
            List<LoggerContextListener> listenerList = context.getCopyOfListenerList();
            boolean levelChangePropagatorInstalled = false;
            for (LoggerContextListener listener : listenerList) {
                if (listener instanceof LevelChangePropagator) {
                    levelChangePropagatorInstalled = true;
                    break;
                }
            }

            //http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
            if (!levelChangePropagatorInstalled
                    && bridgeHandlerInstalled) {
                LevelChangePropagator propagator = new LevelChangePropagator();
                propagator.setContext(context);
                propagator.start();
                context.addListener(propagator);
                addInfo("Slf4j bridge handler found to be enabled. Installing the LevelChangePropagator");
            }
        }
    }

    /**
     * The <code>DummyLogManagerConfiguration</code> class is used as JUL
     * LogginManager configurator to preven reading platform default
     * configuration which just duplicate log output to be redirected to SLF4J.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class DummyLogManagerConfiguration {
    }

    // ~-------------------------------LoggerContextListener

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

            context.setPackagingDataEnabled(logConfigManager.isPackagingDataEnabled());

            // Attach a console appender to handle logging untill we configure
            // one. This would be removed in RootLoggerListener.reset
            final Logger rootLogger = getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.INFO);
            rootLogger.addAppender(logConfigManager.getDefaultAppender());

            // Now record the time of reset with a default appender attached to
            // root logger. We also add a milli second extra to account for logs which would have
            // got fired in same duration
            resetStartTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.toMillis(1);
            addInfo("Registered a default console based logger");

            context.putObject(LogbackManager.class.getName(), LogbackManager.this);
            for (LogbackResetListener l : resetListeners) {
                addInfo("Firing reset listener - onResetStart "+l.getClass());
                l.onResetStart(context);
            }
        }

        public void onStop(LoggerContext context) {
        }

        public void onLevelChange(Logger logger, Level level) {
        }

    }

    private class RootLoggerListener implements LogbackResetListener {

        @Override
        public void onResetStart(LoggerContext context) {

        }

        @Override
        public void onResetComplete(LoggerContext context) {
            // Remove the default console appender that we attached at start of
            // reset
            ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Iterator<Appender<ILoggingEvent>> appenderItr = root.iteratorForAppenders();

            //Root logger has at least 1 appender associated with it. Remove the one added by us
            if (appenderItr.hasNext()) {
                root.detachAppender(LogConfigManager.DEFAULT_CONSOLE_APPENDER_NAME);
                addInfo("Found appender attached with root logger. Detaching the default console based logger");
            } else {
                addInfo("No appender was found to be associated with root logger. Registering " +
                        "a Console based logger");
            }
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

        //Distinguish between Logger configured via
        //1. OSGi Config - The ones configured via ConfigAdmin
        //2. Other means - Configured via Logback config or any other means
        for (LogConfig lc : logConfigManager.getLogConfigs()) {
            for (String category : lc.getCategories()) {
                ctx.osgiConfiguredLoggers.put(category, lc);
            }
        }

        for (Logger logger : loggers) {
            boolean hasOnlySlingRollingAppenders = true;
            Iterator<Appender<ILoggingEvent>> itr = logger.iteratorForAppenders();
            while (itr.hasNext()) {
                Appender<ILoggingEvent> a = itr.next();
                if (a.getName() != null && !ctx.appenders.containsKey(a.getName())) {
                    ctx.appenders.put(a.getName(), a);
                }

                if(!(a instanceof SlingRollingFileAppender)){
                    hasOnlySlingRollingAppenders = false;
                }
            }

            if(logger.getLevel() == null){
                continue;
            }

            boolean configuredViaOSGiConfig =
                    ctx.osgiConfiguredLoggers.containsKey(logger.getName());
            if (!configuredViaOSGiConfig
                    || (configuredViaOSGiConfig && !hasOnlySlingRollingAppenders))
                    {
                ctx.nonOSgiConfiguredLoggers.add(logger);
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
        final List<Logger> nonOSgiConfiguredLoggers = new ArrayList<Logger>();

        final Map<String,LogConfig> osgiConfiguredLoggers = new HashMap<String, LogConfig>();

        final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<String, Appender<ILoggingEvent>>();

        final Map<Appender<ILoggingEvent>, AppenderInfo> dynamicAppenders =
                new HashMap<Appender<ILoggingEvent>, AppenderInfo>();

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

    private void registerWebConsoleSupport() {
        final ServiceFactory serviceFactory = new PluginServiceFactory();

        Properties pluginProps = new Properties();
        pluginProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        pluginProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Support");
        pluginProps.put("felix.webconsole.label", APP_ROOT);
        pluginProps.put("felix.webconsole.title", "Log Support");
        pluginProps.put("felix.webconsole.category", "Sling");
        pluginProps.put("felix.webconsole.css", CSS_REFS);

        registrations.add(bundleContext.registerService("javax.servlet.Servlet", serviceFactory, pluginProps));

        Properties printerProps = new Properties();
        printerProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Configuration Printer");
        printerProps.put("felix.webconsole.label", PRINTER_URL);
        printerProps.put("felix.webconsole.title", "Log Files");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        // TODO need to see to add support for Inventory Feature
        registrations.add(bundleContext.registerService(SlingConfigurationPrinter.class.getName(),
            new SlingConfigurationPrinter(this), printerProps));
    }

    private class PluginServiceFactory implements ServiceFactory {
        private Object instance;

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            synchronized (this) {
                if (this.instance == null) {
                    this.instance = new SlingLogPanel(LogbackManager.this,bundleContext);
                }
                return instance;
            }
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        }
    }

    private void registerEventHandler() {
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

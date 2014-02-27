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
package org.apache.sling.commons.log.logback.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.helpers.Transform;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.CachingDateFormatter;
import org.apache.sling.commons.log.logback.internal.AppenderTracker.AppenderInfo;
import org.apache.sling.commons.log.logback.internal.LogbackManager.LoggerStateContext;
import org.apache.sling.commons.log.logback.internal.config.ConfigurationException;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.apache.sling.commons.log.logback.internal.util.Util;
import org.apache.sling.commons.log.logback.internal.util.XmlUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import static org.apache.sling.commons.log.logback.internal.ConfigSourceTracker.ConfigSourceInfo;
import static org.apache.sling.commons.log.logback.internal.LogbackManager.APP_ROOT;
import static org.apache.sling.commons.log.logback.internal.LogbackManager.RES_LOC;

/**
 * The <code>SlingLogPanel</code> is a Felix Web Console plugin to display the
 * current active log bundle configuration.
 * <p/>
 * In future revisions of this plugin, the configuration may probably even be
 * modified through this panel.
 */
public class SlingLogPanel extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final CachingDateFormatter SDF = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss");

    private static final String[] LEVEL_NAMES = {
            Level.ERROR.levelStr,
            Level.WARN.levelStr,
            Level.INFO.levelStr,
            Level.DEBUG.levelStr,
            Level.TRACE.levelStr,
    };

    private static final String PACKAGE_SEPARATOR = ".";

    private final LogbackManager logbackManager;
    private final BundleContext bundleContext;

    private final String labelRes;

    private final int labelResLen;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SlingLogPanel.class);

    public SlingLogPanel(final LogbackManager logbackManager, BundleContext bundleContext) {
        this.logbackManager = logbackManager;
        this.bundleContext = bundleContext;
        this.labelRes = '/' + APP_ROOT + '/';
        this.labelResLen = labelRes.length() - 1;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        final PrintWriter pw = resp.getWriter();

        final String consoleAppRoot = (String) req.getAttribute("felix.webconsole.appRoot");
        final String pluginRoot = (String) req.getAttribute("felix.webconsole.pluginRoot");

        final LoggerStateContext ctx = logbackManager.determineLoggerState();
        appendLoggerStatus(pw, ctx);
        appendOsgiConfiguredLoggerData(pw, consoleAppRoot);
        appendOtherLoggerData(pw, ctx);
        addAppenderData(pw, consoleAppRoot, ctx);
        appendTurboFilterData(pw, consoleAppRoot, ctx);
        appendLogbackMainConfig(pw);
        appendLogbackFragments(pw, consoleAppRoot);
        appendLogbackStatus(pw, ctx);
        addScriptBlock(pw, ctx);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // check if a configuration should be deleted
        boolean isDelete = req.getParameter("delete") != null;
        // get the configuration pid
        String pid = req.getParameter("pid");
        try {
            if (isDelete) {
                // in delete mode remove the logger with the given pid
                removeLogger(pid);
            } else {
                // get the logger parameters and configure the logger
                // if the given pid is empty a new logger with be created
                String logger = req.getParameter("logger");
                String logLevel = req.getParameter("loglevel");
                String logFile = req.getParameter("logfile");
                String[] loggers = req.getParameterValues("logger");
                if (null != logger) {
                    configureLogger(pid, logLevel, loggers, logFile);
                }
            }
        } catch (ConfigurationException e) {
            internalFailure("", e);
        }
        // send the redirect back to the logpanel
        final String consoleAppRoot = (String) req
                .getAttribute("felix.webconsole.appRoot");
        resp.sendRedirect(consoleAppRoot + "/" + APP_ROOT);
    }

    private void addScriptBlock(PrintWriter pw, LoggerStateContext ctx) {
        pw.println("<script type=\"text/javascript\" src=\"" + RES_LOC + "/slinglog.js\"></script>");
        pw.println("<script type=\"text/javascript\" src=\"" + RES_LOC + "/jquery.autocomplete.min.js\"></script>");
        pw.println("<script type=\"text/javascript\" src=\"" + RES_LOC + "/prettify.js\"></script>");

        pw.println("<script type=\"text/javascript\">$(document).ready(function() { initializeSlingLogPanel(); });</script>");
        pw.println("<script>");
        // write all present loggers as script variable so the autocomplete script can search over them
        pw.println("var loggers=[");
        Set<String> loggers = new TreeSet<String>();

        for (Logger logger : ctx.loggerContext.getLoggerList()) {
            loggers.add(logger.getName());
        }

        Set<String> packageList = new TreeSet<String>();
        for (String logger : loggers) {
            int pos = logger.lastIndexOf(PACKAGE_SEPARATOR);
            if (pos != -1) {
                String pack = logger.substring(0, pos);
                packageList.add(pack);
            }
        }
        loggers.addAll(packageList);
        for (Iterator<String> loggerIt = loggers.iterator(); loggerIt.hasNext(); ) {
            String logger = loggerIt.next();
            pw.print("'" + logger + "'");
            if (loggerIt.hasNext()) {
                pw.print(",");
            }
        }
        pw.println("];");
        pw.println("</script>");
        pw.println("<script>$(document).ready(prettyPrint);</script>");
    }

    private void appendLoggerStatus(PrintWriter pw, LoggerStateContext ctx) {
        pw.printf(
                "<p class='statline'>Log Service Stats: %d categories, %d appender, %d Dynamic appenders</p>%n",
                ctx.getNumberOfLoggers(), ctx.getNumOfAppenders(), ctx.getNumOfDynamicAppenders());
    }

    private void appendOsgiConfiguredLoggerData(PrintWriter pw, String consoleAppRoot) {
        pw.println("<div class='table'>");

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Logger (Configured via OSGi Config)</div>");

        pw.println("<form method='POST'><table id=\"loggerConfig\" class='tablesorter nicetable ui-widget'>");

        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.println("<th>Log Level</th>");
        pw.println("<th>Log File</th>");
        pw.println("<th>Logger</th>");
        pw.println("<th width=\"20%\">" + getConfigColTitle(consoleAppRoot) + "</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");

        final LogConfigManager configManager = logbackManager.getLogConfigManager();
        String rootPath = logbackManager.getRootDir();
        boolean shortenPaths = areAllLogfilesInSameFolder(configManager.getLogWriters(), rootPath);
        for (LogConfig logConfig : configManager.getLogConfigs()) {
            pw.println("<tr id=\"" + logConfig.getConfigPid() + "\">");
            pw.println("<td><span class=\"logLevels\" data-currentloglevel=\""
                    + logConfig.getLogLevel().levelStr + "\">" + logConfig.getLogLevel().levelStr
                    + "</span></td>");
            pw.println("<td><span class=\"logFile\">" + getPath(logConfig.getLogWriterName(), rootPath, shortenPaths) + "</span></td>");

            pw.println("<td><span class=\"loggers\">");
            String sep = "";
            for (String cat : logConfig.getCategories()) {
                pw.println(sep + "<span class=\"logger\">" + cat + "</span>");
                sep = "<br />";
            }
            pw.println("</td>");

            String pid = logConfig.getConfigPid();
            String url = createUrl(consoleAppRoot, "configMgr", pid, true);
            if (logConfig.getCategories().contains(Logger.ROOT_LOGGER_NAME)) {
                url = createUrl(consoleAppRoot, "configMgr", pid, false);
            }
            pw.println("<td>" + url + "</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody><tfoot>");
        pw.println("<tr id=\"newlogger\">");
        pw.println("<td><span id=\"allLogLevels\" class=\"logLevels\" data-loglevels=\"");
        String sep = "";
        for (String levelName : LEVEL_NAMES) {
            pw.print(sep + levelName);
            sep = ",";
        }

        pw.println("\"></span></td>");
        pw.println("<td><span id=\"defaultLogfile\" data-defaultlogfile=\""
                + getPath(configManager.getDefaultWriter().getFileName(), rootPath, shortenPaths)
                + "\" class=\"logFile\"></span></td>");
        pw.println("<td><span class=\"loggers\"></span></td>");
        pw.println("<td><input type='submit' class=\"configureLink\" value='Add new Logger' /></td></tr></tfoot>");

        pw.println("</table></form>");
        pw.println("</div>");
    }

    private void appendOtherLoggerData(PrintWriter pw, LoggerStateContext ctx) {
        if (ctx.nonOSgiConfiguredLoggers.isEmpty()) {
            return;
        }

        pw.println("<div class='table'>");

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Logger (Configured via other means)</div>");

        pw.println("<table class='nicetable ui-widget'>");

        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.println("<th>Log Level</th>");
        pw.println("<th>Additivity</th>");
        pw.println("<th>Name</th>");
        pw.println("<th>Appender</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");

        for (Logger logger : ctx.nonOSgiConfiguredLoggers) {
            pw.println("<tr>");
            pw.println("<td>" + logger.getLevel() + "</td>");
            pw.println("<td>" + Boolean.toString(logger.isAdditive()) + "</td>");
            pw.println("<td>" + logger.getName() + "</td>");

            pw.println("<td>");
            pw.println("<ul>");
            Iterator<Appender<ILoggingEvent>> itr = logger.iteratorForAppenders();
            while (itr.hasNext()) {
                Appender<ILoggingEvent> a = itr.next();
                pw.print("<li>");
                pw.print(getName(a));
                pw.print("</li>");
            }
            pw.println("</ul>");
            pw.println("</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void addAppenderData(PrintWriter pw, String consoleAppRoot, LoggerStateContext ctx) {
        pw.println("<div class='table'>");

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Appender</div>");

        pw.println("<table class='nicetable ui-widget'>");

        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.println("<th>Appender</th>");
        pw.println("<th>" + getConfigColTitle(consoleAppRoot) + "</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");

        for (Appender<ILoggingEvent> appender : ctx.appenders.values()) {
            pw.println("<tr>");
            pw.println("<td>" + getName(appender) + "</td>");
            pw.println("<td>" + formatPid(consoleAppRoot, appender, ctx) + "</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void appendTurboFilterData(PrintWriter pw, String consoleAppRoot, LoggerStateContext ctx) {
        if (ctx.loggerContext.getTurboFilterList().isEmpty()) {
            return;
        }

        pw.println("<div class='table'>");

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Turbo Filters</div>");

        pw.println("<table class='nicetable ui-widget'>");

        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.println("<th>Turbo Filter</th>");
        pw.println("<th>" + getConfigColTitle(consoleAppRoot) + "</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");


        for (TurboFilter tf : ctx.loggerContext.getTurboFilterList()) {
            pw.println("<tr>");
            pw.println("<td>" + getName(tf) + "</td>");
            pw.println("<td>" + formatPid(consoleAppRoot, tf, ctx) + "</td>");
            pw.println("</tr>");

        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }


    private void appendLogbackStatus(PrintWriter pw, LoggerStateContext ctx) {
        pw.println("<div class='table'>");

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Logback Status</div>");
        pw.println("<div style='overflow-y:scroll; height:400px'>");
        pw.println("<table class='nicetable ui-widget'>");

        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.println("<th>Date</th>");
        pw.println("<th>Level</th>");
        pw.println("<th>Origin</th>");
        pw.println("<th>Message</th>");
        pw.println("</tr>");
        pw.println("</thead>");

        pw.println("<tbody class='ui-widget-content'  >");

        List<Status> statusList = ctx.loggerContext.getStatusManager().getCopyOfStatusList();
        for (Status s : statusList) {
            pw.println("<tr>");
            pw.println("<td class=\"date\">" + SDF.format(s.getDate()) + "</td>");
            pw.println("<td class=\"level\">" + statusLevelAsString(s) + "</td>");
            pw.println("<td>" + abbreviatedOrigin(s) + "</td>");
            pw.println("<td>" + s.getMessage() + "</td>");
            pw.println("</tr>");

            // noinspection ThrowableResultOfMethodCallIgnored
            if (s.getThrowable() != null) {
                printThrowable(pw, s.getThrowable());
            }
        }

        pw.println("</tbody>");

        pw.println("</table>");
        pw.print("</div>");
        pw.println("</div>");
    }

    private void appendLogbackMainConfig(PrintWriter pw) {
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Logback Config</div>");
        pw.println("<table class='nicetable ui-widget'>");
        pw.println("<tbody class='ui-widget-content'>");

        File configFile = null;
        URL url = null;
        InputSource source = null;
        try {
            String msg;
            configFile = logbackManager.getLogConfigManager().getLogbackConfigFile();
            if (configFile != null) {
                source = new InputSource(new BufferedInputStream(new FileInputStream(configFile)));
                msg = "Source " + configFile.getAbsolutePath();
            } else {
                url = logbackManager.getDefaultConfig();
                URLConnection uc = url.openConnection();
                uc.setDefaultUseCaches(false);
                source = new InputSource(new BufferedInputStream(uc.getInputStream()));
                msg = "Source : Default";
            }

            pw.println("<tr>");
            pw.println("<td>" + msg + "</td>");
            pw.println("</tr>");

            pw.println("<tr><td>");
            String textContent = XmlUtil.escapeXml(XmlUtil.prettyPrint(source));
            pw.print("<pre class=\"prettyprint lang-xml\" style=\"border: 0px\">");
            pw.print(textContent);
            pw.print("</pre>");
            pw.println("</td></tr>");
        } catch (IOException e) {
            String msg = "Error occurred while opening file [" + configFile + "]";
            if (url != null) {
                msg = "Error occurred while opening url [" + url + "]";
            }
            log.warn(msg, e);
        } finally {
            Util.close(source);
        }
        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void appendLogbackFragments(PrintWriter pw, String consoleAppRoot) {
        final Collection<ConfigSourceInfo> configSources = logbackManager.getConfigSourceTracker().getSources();

        if (configSources.isEmpty()) {
            return;
        }

        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Logback Config Fragments</div>");
        pw.println("<table class='nicetable ui-widget'>");
        pw.println("<tbody class='ui-widget-content'>");

        for (ConfigSourceInfo ci : configSources) {
            final String pid = ci.getReference().getProperty(Constants.SERVICE_ID).toString();
            String url = createUrl(consoleAppRoot, "services", pid);
            pw.println("<tr>");
            pw.println("<td>" + url + "</td>");
            pw.println("</tr>");

            pw.println("<tr>");
            pw.println("<td>");
            // prettify.js adds a border. We eed to remove that
            pw.print("<pre class=\"prettyprint lang-xml\" style=\"border: 0px\">");
            pw.print(ci.getSourceAsEscapedString());
            pw.print("</pre>");

            pw.println("</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    /**
     * Called internally by {@link AbstractWebConsolePlugin} to load resources.
     * This particular implementation depends on the label. As example, if the
     * plugin is accessed as <code>/system/console/abc</code>, and the plugin
     * resources are accessed like <code>/system/console/abc/res/logo.gif</code>
     * , the code here will try load resource <code>/res/logo.gif</code> from
     * the bundle, providing the plugin.
     *
     * @param path the path to read.
     * @return the URL of the resource or <code>null</code> if not found.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected URL getResource(String path) {
        return (path != null && path.startsWith(labelRes)) ? //
                getClass().getResource(path.substring(labelResLen))
                : null;
    }

    /**
     * Checks if all log files are in the same folder, then the path can displayed shortened in the panel.
     *
     * @param logWriters list of log writers
     * @param rootPath   root path
     * @return true if all logfiles are in the same folder
     */
    private boolean areAllLogfilesInSameFolder(final Iterable<LogWriter> logWriters, final String rootPath) {
        String lastPath = null;
        for (final LogWriter writer : logWriters) {
            String path = getPath(writer.getFileName(), null, false);
            if (!path.startsWith(rootPath)) {
                return false;
            }
            path = path.substring(0, rootPath.length());
            if (lastPath == null) {
                lastPath = path;
            } else if (!path.equals(lastPath)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Configures the logger with the given pid. If the pid is empty a new logger configuration is created.
     *
     * @param pid      configuration pid of the logger
     * @param logLevel the log level to set
     * @param loggers  list of logger categories to set
     * @param logFile  log file (relative path is ok)
     * @throws IOException            when an existing configuration couldn't be updated or a configuration couldn't be created.
     * @throws ConfigurationException when mandatory parameters where not specified
     */
    private void configureLogger(final String pid, final String logLevel, final String[] loggers, final String logFile)
            throws IOException, ConfigurationException {
        // try to get the configadmin service reference
        ServiceReference sr = this.bundleContext
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (sr != null) {
            try {
                if (logLevel == null) {
                    throw new ConfigurationException(LogConfigManager.LOG_LEVEL,
                            "Log level has to be specified.");
                }
                if (loggers == null) {
                    throw new ConfigurationException(LogConfigManager.LOG_LOGGERS,
                            "Logger categories have to be specified.");
                }
                if (logFile == null) {
                    throw new ConfigurationException(LogConfigManager.LOG_FILE,
                            "LogFile name has to be specified.");
                }
                // try to get the configadmin
                final ConfigurationAdmin configAdmin = (ConfigurationAdmin) this.bundleContext
                        .getService(sr);
                if (configAdmin != null) {
                    Configuration config;
                    if (pid == null || pid.length() == 0) {
                        config = configAdmin.createFactoryConfiguration(LogConfigManager.FACTORY_PID_CONFIGS);
                    } else {
                        config = configAdmin.getConfiguration(pid);
                    }
                    if (config != null) {
                        Dictionary<String, Object> dict = new Hashtable<String, Object>();
                        dict.put(LogConfigManager.LOG_LEVEL, logLevel.toLowerCase());
                        dict.put(LogConfigManager.LOG_LOGGERS, loggers);
                        dict.put(LogConfigManager.LOG_FILE, logFile);
                        config.update(dict);
                    }
                }
            } finally {
                // release the configadmin reference
                this.bundleContext.ungetService(sr);
            }
        }
    }


    /**
     * Removes the logger configuration with the given pid in the configadmin.
     *
     * @param pid pid of the configuration to delete
     * @throws ConfigurationException when there is no configuration for this pid
     */
    private void removeLogger(final String pid)
            throws ConfigurationException {
        // try to get the configadmin service reference
        ServiceReference sr = this.bundleContext
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (sr != null) {
            try {
                if (pid == null) {
                    throw new ConfigurationException(LogConfigManager.PID,
                            "PID has to be specified.");
                }
                // try to get the configadmin
                final ConfigurationAdmin configAdmin = (ConfigurationAdmin) this.bundleContext
                        .getService(sr);
                if (configAdmin != null) {
                    try {
                        Configuration config = configAdmin.getConfiguration(pid);
                        if (config != null) {
                            config.delete();
                        } else {
                            throw new ConfigurationException(LogConfigManager.PID,
                                    "No configuration for this PID:" + pid);
                        }
                    } catch (IOException ioe) {
                        internalFailure(
                                "Cannot delete configuration for pid " + pid,
                                ioe);
                    }
                }
            } finally {
                // release the configadmin reference
                this.bundleContext.ungetService(sr);
            }
        }
    }

    private void internalFailure(String msg, Exception e) {
        logbackManager.getLogConfigManager().internalFailure(msg, e);
    }

    private static String getName(TurboFilter tf) {
        if (tf.getName() != null) {
            return String.format("%s (%s)", tf.getName(), tf.getClass().getName());
        } else {
            return tf.getClass().getName();
        }
    }

    private static String formatPid(final String consoleAppRoot, final TurboFilter tf,
                                    final LoggerStateContext ctx) {
        ServiceReference sr = ctx.getTurboFilterRef(tf);
        if (sr != null) {
            final String pid = sr.getProperty(Constants.SERVICE_ID).toString();
            return createUrl(consoleAppRoot, "services", pid);
        } else {
            return "[config]";
        }
    }

    private static String getName(Appender<ILoggingEvent> appender) {
        // For normal file appender we also display the name of appender
        if (appender instanceof FileAppender) {
            return String.format("File : [%s] %s", appender.getName(), ((FileAppender) appender).getFile());
        }

        final String appenderName = appender.getName();
        if(appenderName == null){
            return appender.getClass().getName();
        } else {
            return String.format("%s (%s)", appender.getName(), appender.getClass().getName());
        }
    }

    private static String formatPid(final String consoleAppRoot, final Appender<ILoggingEvent> appender,
                                    final LoggerStateContext ctx) {
        if (appender instanceof SlingRollingFileAppender) {
            final LogWriter lw = ((SlingRollingFileAppender) appender).getLogWriter();
            String pid = lw.getConfigurationPID();
            if (lw.isImplicit()) {
                pid = lw.getImplicitConfigPID();
            }
            return createUrl(consoleAppRoot, "configMgr", pid);
        } else if (ctx.isDynamicAppender(appender)) {
            final AppenderInfo ai = ctx.dynamicAppenders.get(appender);

            final String pid = ai.pid;
            return createUrl(consoleAppRoot, "services", pid);
        } else {
            return "[others]";
        }
    }

    private static String getConfigColTitle(String consoleAppRoot) {
        return (consoleAppRoot == null) ? "PID" : "Configuration";
    }

    private static String createUrl(String consoleAppRoot, String subContext, String pid) {
        return createUrl(consoleAppRoot, subContext, pid, false);
    }

    private static String createUrl(String consoleAppRoot, String subContext, String pid, boolean inlineEditable) {
        // no recent web console, so just render the pid as the link
        if (consoleAppRoot == null) {
            return "<a href=\"" + subContext + "/" + pid + "\">" + pid + "</a>";
        }

        // recent web console has app root and hence we can use an image
        String classAttr = "class=\"configureLink\"";
        if (!inlineEditable) {
            classAttr = "";
        }

        return "<a " + classAttr + " href=\"" + subContext + "/" + pid + "\"><img src=\"" + consoleAppRoot
                + "/res/imgs/component_configure.png\" border=\"0\" /></a>";
    }

    private static String getPath(String path, final String rootPath, final boolean shortenPaths) {
        if (shortenPaths && path != null) {
            // if the shortenPath parameter is set (all log files are in the same folder)
            // remove the root path (root log file folder) from the paths
            path = path.substring(rootPath.length() + 1);
        }
        return (path != null) ? path : "[stdout]";
    }


    // ~------------------------------------------------Status Manager
    // Based on ch.qos.logback.core.status.ViewStatusMessagesServletBase

    private static String statusLevelAsString(Status s) {
        switch (s.getEffectiveLevel()) {
            case Status.INFO:
                return "INFO";
            case Status.WARN:
                return "<span class=\"warn\">WARN</span>";
            case Status.ERROR:
                return "<span class=\"error\">ERROR</span>";
        }
        return null;
    }

    static String abbreviatedOrigin(Status s) {
        Object o = s.getOrigin();
        if (o == null) {
            return null;
        }
        String fqClassName = o.getClass().getName();
        int lastIndex = fqClassName.lastIndexOf(CoreConstants.DOT);
        if (lastIndex != -1) {
            return fqClassName.substring(lastIndex + 1, fqClassName.length());
        } else {
            return fqClassName;
        }
    }

    private static void printThrowable(PrintWriter pw, Throwable t) {
        pw.println("  <tr>");
        pw.println("    <td colspan=\"4\" class=\"exception\"><pre>");
        StringWriter sw = new StringWriter();
        PrintWriter expPw = new PrintWriter(sw);
        t.printStackTrace(expPw);
        pw.println(Transform.escapeTags(sw.getBuffer()));
        pw.println("    </pre></td>");
        pw.println("  </tr>");
    }
}

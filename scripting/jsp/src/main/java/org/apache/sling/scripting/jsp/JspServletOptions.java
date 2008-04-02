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
package org.apache.sling.scripting.jsp;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.sling.scripting.jsp.jasper.IOProvider;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspConfig;
import org.apache.sling.scripting.jsp.jasper.compiler.Localizer;
import org.apache.sling.scripting.jsp.jasper.compiler.TagPluginManager;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to hold all init parameters specific to the JSP engine.
 */
public class JspServletOptions implements Options {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspServletOptions.class);

    /** Default source and target VM version (value is "1.5"). */
    private static final String DEFAULT_VM_VERSION = "1.5";

    private Properties settings = new Properties();

    /**
     * Is Jasper being used in development mode?
     */
    private boolean development = true;

    /**
     * Should Ant fork its java compiles of JSP pages.
     */
    public boolean fork = true;

    /**
     * Do you want to keep the generated Java files around?
     */
    private boolean keepGenerated = true;

    /**
     * Should white spaces between directives or actions be trimmed?
     */
    private boolean trimSpaces = false;

    /**
     * Determines whether tag handler pooling is enabled.
     */
    private boolean isPoolingEnabled = true;

    /**
     * Do you want support for "mapped" files? This will generate servlet that
     * has a print statement per line of the JSP file. This seems like a really
     * nice feature to have for debugging.
     */
    private boolean mappedFile = true;

    /**
     * Do you want stack traces and such displayed in the client's browser? If
     * this is false, such messages go to the standard error or a log file if
     * the standard error is redirected.
     */
    private boolean sendErrorToClient = false;

    /**
     * Do we want to include debugging information in the class file?
     */
    private boolean classDebugInfo = true;

    /**
     * Background compile thread check interval in seconds.
     */
    private int checkInterval = 0;

    /**
     * Is the generation of SMAP info for JSR45 debuggin suppressed?
     */
    private boolean isSmapSuppressed = false;

    /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    private boolean isSmapDumped = false;

    /**
     * Are Text strings to be generated as char arrays?
     */
    private boolean genStringAsCharArray = false;

    private boolean errorOnUseBeanInvalidClassAttribute = true;

    /**
     * I want to see my generated servlets. Which directory are they in?
     */
    private String scratchDir;

    /**
     * Need to have this as is for versions 4 and 5 of IE. Can be set from the
     * initParams so if it changes in the future all that is needed is to have a
     * jsp initParam of type ieClassId="<value>"
     */
    private String ieClassId = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

    /**
     * What classpath should I use while compiling generated servlets?
     */
    private String classpath = null;

    /**
     * Compiler target VM.
     */
    private String compilerTargetVM = DEFAULT_VM_VERSION;

    /**
     * The compiler source VM.
     */
    private String compilerSourceVM = DEFAULT_VM_VERSION;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache = null;

    /**
     * Jsp config information
     */
    private JspConfig jspConfig = null;

    /**
     * TagPluginManager
     */
    private TagPluginManager tagPluginManager = null;

    /**
     * Java platform encoding to generate the JSP page servlet.
     */
    private String javaEncoding = "UTF8";

    /**
     * Modification test interval.
     */
    private int modificationTestInterval = -1;

    /**
     * The class loader to use to compile and load JSP files
     */
    private ClassLoader jspClassLoader;

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    private boolean xpoweredBy;

    /**
     * Should we include a source fragment in exception messages, which could be
     * displayed to the developer ?
     */
    private boolean displaySourceFragments = true;

    public String getProperty(String name) {
        return this.settings.getProperty(name);
    }

    public void setProperty(String name, String value) {
        if (name != null && value != null) {
            this.settings.setProperty(name, value);
        }
    }

    /**
     * Are we keeping generated code around?
     */
    public boolean getKeepGenerated() {
        return this.keepGenerated;
    }

    /**
     * Should white spaces between directives or actions be trimmed?
     */
    public boolean getTrimSpaces() {
        return this.trimSpaces;
    }

    public boolean isPoolingEnabled() {
        return this.isPoolingEnabled;
    }

    /**
     * Are we supporting HTML mapped servlets?
     */
    public boolean getMappedFile() {
        return this.mappedFile;
    }

    /**
     * Should errors be sent to client or thrown into stderr?
     */
    public boolean getSendErrorToClient() {
        return this.sendErrorToClient;
    }

    /**
     * Should class files be compiled with debug information?
     */
    public boolean getClassDebugInfo() {
        return this.classDebugInfo;
    }

    /**
     * Background JSP compile thread check intervall
     */
    public int getCheckInterval() {
        return this.checkInterval;
    }

    /**
     * Modification test interval.
     */
    public int getModificationTestInterval() {
        return this.modificationTestInterval;
    }

    /**
     * Is Jasper being used in development mode?
     */
    public boolean getDevelopment() {
        return this.development;
    }

    /**
     * Is the generation of SMAP info for JSR45 debuggin suppressed?
     */
    public boolean isSmapSuppressed() {
        return this.isSmapSuppressed;
    }

    /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    public boolean isSmapDumped() {
        return this.isSmapDumped;
    }

    /**
     * Are Text strings to be generated as char arrays?
     */
    public boolean genStringAsCharArray() {
        return this.genStringAsCharArray;
    }

    /**
     * Class ID for use in the plugin tag when the browser is IE.
     */
    public String getIeClassId() {
        return this.ieClassId;
    }

    /**
     * What is my scratch dir?
     */
    public String getScratchDir() {
        return this.scratchDir;
    }

    /**
     * What classpath should I use while compiling the servlets generated from
     * JSP files?
     */
    public String getClassPath() {
        return this.classpath;
    }

    /**
     * Return <code>null</code> to force use of the <code>JasperLoader</code>.
     */
    public ClassLoader getJspClassLoader() {
        return this.jspClassLoader;
    }

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    public boolean isXpoweredBy() {
        return this.xpoweredBy;
    }

    /**
     * Allways return null for the compiler to use, assuming JDT is the default
     * which we will never overwrite.
     */
    public String getCompiler() {
        return null;
    }

    /**
     * @see Options#getCompilerTargetVM
     */
    public String getCompilerTargetVM() {
        return this.compilerTargetVM;
    }

    /**
     * @see Options#getCompilerSourceVM
     */
    public String getCompilerSourceVM() {
        return this.compilerSourceVM;
    }

    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        return this.errorOnUseBeanInvalidClassAttribute;
    }

    public void setErrorOnUseBeanInvalidClassAttribute(boolean b) {
        this.errorOnUseBeanInvalidClassAttribute = b;
    }

    public TldLocationsCache getTldLocationsCache() {
        return this.tldLocationsCache;
    }

    public void setTldLocationsCache(TldLocationsCache tldC) {
        this.tldLocationsCache = tldC;
    }

    public String getJavaEncoding() {
        return this.javaEncoding;
    }

    public boolean getFork() {
        return this.fork;
    }

    public JspConfig getJspConfig() {
        return this.jspConfig;
    }

    public TagPluginManager getTagPluginManager() {
        return this.tagPluginManager;
    }

    public boolean isCaching() {
        return false;
    }

    public Map getCache() {
        return null;
    }

    public boolean getDisplaySourceFragment() {
        return displaySourceFragments;
    }

    /**
     * Allways return null for the compiler to use, assuming JDT is the default
     * which we will never overwrite.
     */
    public String getCompilerClassName() {
        return null;
    }

    /**
     * Create an JspServletOptions object using data available from
     * ServletConfig and ServletContext.
     */
    public JspServletOptions(ServletContext servletContext,
            IOProvider ioProvider, ComponentContext componentContext,
            ClassLoader jspClassLoader, TldLocationsCache tldLocationsCache) {

        this.jspClassLoader = jspClassLoader;

        // JVM version numbers default to 1.4
        this.compilerSourceVM = DEFAULT_VM_VERSION;
        this.compilerTargetVM = DEFAULT_VM_VERSION;

        Dictionary<?, ?> config = componentContext.getProperties();
        Enumeration<?> enumeration = config.keys();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (key.startsWith("jasper.")) {
                Object value = config.get(key);
                if (value != null) {
                    setProperty(key.substring("jasper.".length()),
                        value.toString());
                }
            }

        }

        // quick hack
        // String validating=config.getInitParameter( "validating");
        // if( "false".equals( validating )) ParserUtils.validating=false;

        String keepgen = getProperty("keepgenerated");
        if (keepgen != null) {
            if (keepgen.equalsIgnoreCase("true")) {
                this.keepGenerated = true;
            } else if (keepgen.equalsIgnoreCase("false")) {
                this.keepGenerated = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.keepgen"));
                }
            }
        }

        String trimsp = getProperty("trimSpaces");
        if (trimsp != null) {
            if (trimsp.equalsIgnoreCase("true")) {
                this.trimSpaces = true;
            } else if (trimsp.equalsIgnoreCase("false")) {
                this.trimSpaces = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.trimspaces"));
                }
            }
        }

        String dpsFrags = getProperty("displaySourceFragments");
        if (dpsFrags != null) {
            if (dpsFrags.equalsIgnoreCase("true")) {
                this.displaySourceFragments = true;
            } else if (dpsFrags.equalsIgnoreCase("false")) {
                this.displaySourceFragments = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.displaySourceFragment"));
                }
            }
        }

        this.isPoolingEnabled = true;
        String poolingEnabledParam = getProperty("enablePooling");
        if (poolingEnabledParam != null
            && !poolingEnabledParam.equalsIgnoreCase("true")) {
            if (poolingEnabledParam.equalsIgnoreCase("false")) {
                this.isPoolingEnabled = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.enablePooling"));
                }
            }
        }

        String mapFile = getProperty("mappedfile");
        if (mapFile != null) {
            if (mapFile.equalsIgnoreCase("true")) {
                this.mappedFile = true;
            } else if (mapFile.equalsIgnoreCase("false")) {
                this.mappedFile = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.mappedFile"));
                }
            }
        }

        String senderr = getProperty("sendErrToClient");
        if (senderr != null) {
            if (senderr.equalsIgnoreCase("true")) {
                this.sendErrorToClient = true;
            } else if (senderr.equalsIgnoreCase("false")) {
                this.sendErrorToClient = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.sendErrToClient"));
                }
            }
        }

        String debugInfo = getProperty("classdebuginfo");
        if (debugInfo != null) {
            if (debugInfo.equalsIgnoreCase("true")) {
                this.classDebugInfo = true;
            } else if (debugInfo.equalsIgnoreCase("false")) {
                this.classDebugInfo = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.classDebugInfo"));
                }
            }
        }

        String checkInterval = getProperty("checkInterval");
        if (checkInterval != null) {
            try {
                this.checkInterval = Integer.parseInt(checkInterval);
                if (this.checkInterval == 0) {
                    this.checkInterval = 300;
                    if (log.isWarnEnabled()) {
                        log.warn(Localizer.getMessage("jsp.warning.checkInterval"));
                    }
                }
            } catch (NumberFormatException ex) {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.checkInterval"));
                }
            }
        }

        String modificationTestInterval = getProperty("modificationTestInterval");
        if (modificationTestInterval != null) {
            try {
                this.modificationTestInterval = Integer.parseInt(modificationTestInterval);
            } catch (NumberFormatException ex) {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.modificationTestInterval"));
                }
            }
        }

        String development = getProperty("development");
        if (development != null) {
            if (development.equalsIgnoreCase("true")) {
                this.development = true;
            } else if (development.equalsIgnoreCase("false")) {
                this.development = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.development"));
                }
            }
        }

        String suppressSmap = getProperty("suppressSmap");
        if (suppressSmap != null) {
            if (suppressSmap.equalsIgnoreCase("true")) {
                this.isSmapSuppressed = true;
            } else if (suppressSmap.equalsIgnoreCase("false")) {
                this.isSmapSuppressed = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.suppressSmap"));
                }
            }
        }

        String dumpSmap = getProperty("dumpSmap");
        if (dumpSmap != null) {
            if (dumpSmap.equalsIgnoreCase("true")) {
                this.isSmapDumped = true;
            } else if (dumpSmap.equalsIgnoreCase("false")) {
                this.isSmapDumped = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.dumpSmap"));
                }
            }
        }

        String genCharArray = getProperty("genStrAsCharArray");
        if (genCharArray != null) {
            if (genCharArray.equalsIgnoreCase("true")) {
                this.genStringAsCharArray = true;
            } else if (genCharArray.equalsIgnoreCase("false")) {
                this.genStringAsCharArray = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.genchararray"));
                }
            }
        }

        String errBeanClass = getProperty("errorOnUseBeanInvalidClassAttribute");
        if (errBeanClass != null) {
            if (errBeanClass.equalsIgnoreCase("true")) {
                this.errorOnUseBeanInvalidClassAttribute = true;
            } else if (errBeanClass.equalsIgnoreCase("false")) {
                this.errorOnUseBeanInvalidClassAttribute = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.errBean"));
                }
            }
        }

        String ieClassId = getProperty("ieClassId");
        if (ieClassId != null) {
            this.ieClassId = ieClassId;
        }

        String classpath = getProperty("classpath");
        if (classpath != null) {
            this.classpath = classpath;
        }

        String dir = getProperty("scratchdir");
        this.scratchDir = (dir != null) ? dir : "/var/classes";
        ioProvider.mkdirs(this.scratchDir);

        String compilerTargetVM = getProperty("compilerTargetVM");
        if (compilerTargetVM != null) {
            this.compilerTargetVM = compilerTargetVM;
        }

        String compilerSourceVM = getProperty("compilerSourceVM");
        if (compilerSourceVM != null) {
            this.compilerSourceVM = compilerSourceVM;
        }

        String javaEncoding = getProperty("javaEncoding");
        if (javaEncoding != null) {
            this.javaEncoding = javaEncoding;
        }

        String fork = getProperty("fork");
        if (fork != null) {
            if (fork.equalsIgnoreCase("true")) {
                this.fork = true;
            } else if (fork.equalsIgnoreCase("false")) {
                this.fork = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.fork"));
                }
            }
        }

        String xpoweredBy = getProperty("xpoweredBy");
        if (xpoweredBy != null) {
            if (xpoweredBy.equalsIgnoreCase("true")) {
                this.xpoweredBy = true;
            } else if (xpoweredBy.equalsIgnoreCase("false")) {
                this.xpoweredBy = false;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(Localizer.getMessage("jsp.warning.xpoweredBy"));
                }
            }
        }

        // Setup the global Tag Libraries location cache for this
        // web-application.
        this.tldLocationsCache = tldLocationsCache;

        // Setup the jsp config info for this web app.
        this.jspConfig = new JspConfig(servletContext);

        // Create a Tag plugin instance
        this.tagPluginManager = new TagPluginManager(servletContext);
    }

}

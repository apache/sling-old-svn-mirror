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

import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.sling.scripting.jsp.jasper.IOProvider;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspConfig;
import org.apache.sling.scripting.jsp.jasper.compiler.Localizer;
import org.apache.sling.scripting.jsp.jasper.compiler.TagPluginManager;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to hold all init parameters specific to the JSP engine.
 */
public class JspServletOptions implements Options {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspServletOptions.class);

    /** Value for automatic source/target version setting. */
    public static final String AUTOMATIC_VERSION = "auto";

    private final Map<String, String> settings = new TreeMap<String, String>();

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
     * Need to have this as is for versions 4 and 5 of IE. Can be set from the
     * initParams so if it changes in the future all that is needed is to have a
     * jsp initParam of type ieClassId="<value>"
     */
    private String ieClassId = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

    /**
     * Compiler target VM.
     */
    private String compilerTargetVM;

    /**
     * The compiler source VM.
     */
    private String compilerSourceVM;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache;

    /**
     * Jsp config information
     */
    private JspConfig jspConfig;

    /**
     * TagPluginManager
     */
    private TagPluginManager tagPluginManager;

    /**
     * Java platform encoding to generate the JSP page servlet.
     */
    private String javaEncoding = "UTF8";

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    private boolean xpoweredBy;

    /**
     * Should we include a source fragment in exception messages, which could be
     * displayed to the developer ?
     */
    private boolean displaySourceFragments = false;

    private String getProperty(final String name) {
        return this.settings.get(name);
    }

    private void setProperty(final String name, final String value) {
        this.settings.put(name, value);
    }

    public Map<String, String> getProperties() {
        return this.settings;
    }

    /**
     * Are we keeping generated code around?
     */
    @Override
    public boolean getKeepGenerated() {
        return this.keepGenerated;
    }

    /**
     * Should white spaces between directives or actions be trimmed?
     */
    @Override
    public boolean getTrimSpaces() {
        return this.trimSpaces;
    }

    @Override
    public boolean isPoolingEnabled() {
        return this.isPoolingEnabled;
    }

    /**
     * Are we supporting HTML mapped servlets?
     */
    @Override
    public boolean getMappedFile() {
        return this.mappedFile;
    }

    /**
     * Should errors be sent to client or thrown into stderr?
     */
    @Override
    public boolean getSendErrorToClient() {
        return this.sendErrorToClient;
    }

    /**
     * Should class files be compiled with debug information?
     */
    @Override
    public boolean getClassDebugInfo() {
        return this.classDebugInfo;
    }

    /**
     * Is the generation of SMAP info for JSR45 debugging suppressed?
     */
    @Override
    public boolean isSmapSuppressed() {
        return this.isSmapSuppressed;
    }

    /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    @Override
    public boolean isSmapDumped() {
        return this.isSmapDumped;
    }

    /**
     * Are Text strings to be generated as char arrays?
     */
    @Override
    public boolean genStringAsCharArray() {
        return this.genStringAsCharArray;
    }

    /**
     * Class ID for use in the plugin tag when the browser is IE.
     */
    @Override
    public String getIeClassId() {
        return this.ieClassId;
    }

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    @Override
    public boolean isXpoweredBy() {
        return this.xpoweredBy;
    }

    /**
     * Allways return null for the compiler to use, assuming JDT is the default
     * which we will never overwrite.
     */
    @Override
    public String getCompiler() {
        return null;
    }

    /**
     * @see Options#getCompilerTargetVM
     */
    @Override
    public String getCompilerTargetVM() {
        return this.compilerTargetVM;
    }

    /**
     * @see Options#getCompilerSourceVM
     */
    @Override
    public String getCompilerSourceVM() {
        return this.compilerSourceVM;
    }

    @Override
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        return this.errorOnUseBeanInvalidClassAttribute;
    }

    public void setErrorOnUseBeanInvalidClassAttribute(boolean b) {
        this.errorOnUseBeanInvalidClassAttribute = b;
    }

    @Override
    public TldLocationsCache getTldLocationsCache() {
        return this.tldLocationsCache;
    }

    public void setTldLocationsCache(TldLocationsCache tldC) {
        this.tldLocationsCache = tldC;
    }

    @Override
    public String getJavaEncoding() {
        return this.javaEncoding;
    }

    @Override
    public boolean getFork() {
        return this.fork;
    }

    @Override
    public JspConfig getJspConfig() {
        return this.jspConfig;
    }

    @Override
    public TagPluginManager getTagPluginManager() {
        return this.tagPluginManager;
    }

    @Override
    public boolean getDisplaySourceFragment() {
        return displaySourceFragments;
    }

    /**
     * Always return null for the compiler to use, assuming JDT is the default
     * which we will never overwrite.
     */
    @Override
    public String getCompilerClassName() {
        return null;
    }

    /**
     * Create an JspServletOptions object using data available from
     * ServletConfig and ServletContext.
     */
    public JspServletOptions(ServletContext servletContext,
            IOProvider ioProvider, Map<String, Object> config,
            TldLocationsCache tldLocationsCache) {

        // JVM version numbers default to current vm version
        this.compilerSourceVM = System.getProperty("java.specification.version");
        this.compilerTargetVM = this.compilerSourceVM;

        for(final Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("jasper.")) {
                final Object value = entry.getValue();
                if (value != null) {
                    final String strValue = String.valueOf(value).trim();
                    if ( strValue.length() > 0 ) {
                        setProperty(key.substring("jasper.".length()), strValue);
                    }
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

        final String targetVM = getProperty("compilerTargetVM");
        if (targetVM != null && !AUTOMATIC_VERSION.equalsIgnoreCase(targetVM) ) {
            this.compilerTargetVM = targetVM;
        }
        this.setProperty("compilerTargetVM", this.compilerTargetVM);

        final String sourceVM = getProperty("compilerSourceVM");
        if (sourceVM != null && !AUTOMATIC_VERSION.equalsIgnoreCase(sourceVM) ) {
            this.compilerSourceVM = sourceVM;
        }
        this.setProperty("compilerSourceVM", this.compilerSourceVM);

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

    @Override
    public String getScratchDir() {
        return ":";
    }
}

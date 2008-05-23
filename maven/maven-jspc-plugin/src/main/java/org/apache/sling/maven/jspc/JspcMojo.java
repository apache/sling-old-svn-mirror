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
package org.apache.sling.maven.jspc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.JspCompilationContext;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.Compiler;
import org.apache.sling.scripting.jsp.jasper.compiler.JspConfig;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.compiler.TagPluginManager;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.apache.sling.scripting.jsp.jasper.xmlparser.TreeNode;

/**
 * The <code>JspcMojo</code> is implements the Sling Maven JspC goal
 * <code>jspc</code> compiling JSP into the target and creating a component
 * descriptor for Declarative Services to use the JSP with the help of the
 * appropriate adapter as component.
 *
 * @goal jspc
 * @phase compile
 * @description Compile JSP Files into Servlet Classes using the same JSP
 *              Compiler as is used at runtime to compile Repository based JSP
 *              into classes.
 * @requiresDependencyResolution compile
 */
public class JspcMojo extends AbstractMojo implements Options {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of JSP source files.
     *
     * @parameter expression="${jspc.sourceDirectory}"
     *            default-value="${project.build.scriptSourceDirectory}"
     */
    private File sourceDirectory;

    /**
     * Target folder for the compiled classes.
     *
     * @parameter expression="${jspc.outputDirectory}"
     *            default-value="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${jspc.jasper.classdebuginfo}"
     *            default-value="true"
     */
    private boolean jasperClassDebugInfo;

    /**
     * @parameter expression="${jspc.jasper.enablePooling}" default-value="true"
     */
    private boolean jasperEnablePooling;

    /**
     * @parameter expression="${jspc.jasper.ieClassId}"
     *            default-value="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
     */
    private String jasperIeClassId;

    /**
     * @parameter expression="${jspc.jasper.genStringAsCharArray}"
     *            default-value="false"
     */
    private boolean jasperGenStringAsCharArray;

    /**
     * @parameter expression="${jspc.jasper.keepgenerated}" default-value="true"
     */
    private boolean jasperKeepGenerated;

    /**
     * @parameter expression="${jspc.jasper.mappedfile}" default-value="true"
     */
    private boolean jasperMappedFile;

    /**
     * @parameter expression="${jspc.jasper.trimSpaces}" default-value="false"
     */
    private boolean jasperTrimSpaces;

    /**
     * @parameter expression="${jspc.failOnError}" default-value="true"
     */
    private boolean failOnError;

    /**
     * @parameter expression="${jspc.showSuccess}" default-value="false"
     */
    private boolean showSuccess;

    /**
     * @parameter expression="${jspc.compilerTargetVM}" default-value="1.5"
     */
    private String compilerTargetVM;

    /**
     * @parameter expression="${jspc.compilerSourceVM}" default-value="1.5"
     */
    private String compilerSourceVM;

    /**
     * Comma separated list of extensions of files to be compiled by the plugin.
     *
     * @parameter expression="${jspc.jspFileExtensions}"
     *            default-value="jsp,jspx"
     */
    private String jspFileExtensions;

    /**
     * @parameter expression="${jspc.servletPackage}"
     *            default-value="org.apache.jsp"
     */
    private String servletPackage;

    private Set<String> jspFileExtensionSet;

    private boolean compile = true;

    private String uriSourceRoot;

    private List<String> pages = new ArrayList<String>();

    private ServletContext context;

    private JspRuntimeContext rctxt;

    private URLClassLoader loader = null;

    private Map<String, TreeNode> tldCache;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache = null;

    private JspConfig jspConfig = null;

    private TagPluginManager tagPluginManager = null;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException {

        try {
            uriSourceRoot = sourceDirectory.getCanonicalPath();
        } catch (Exception e) {
            uriSourceRoot = sourceDirectory.getAbsolutePath();
        }

        // ensure output directory
        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.isDirectory()) {
            if (outputDirectoryFile.exists()) {
                throw new MojoExecutionException(outputDirectory
                    + " exists but is not a directory");
            }

            if (!outputDirectoryFile.mkdirs()) {
                throw new MojoExecutionException(
                    "Cannot create output directory " + outputDirectory);
            }
        }

        // have the files compiled
        String oldValue = System.getProperty(LogFactoryImpl.LOG_PROPERTY);
        try {
            // ensure the JSP Compiler does not try to use Log4J
            System.setProperty(LogFactoryImpl.LOG_PROPERTY,
                SimpleLog.class.getName());

            executeInternal();
        } catch (JasperException je) {
            getLog().error("Compilation Failure", je);
            throw new MojoExecutionException(je.getMessage(), je);
        } finally {
            if (oldValue == null) {
                System.clearProperty(LogFactoryImpl.LOG_PROPERTY);
            } else {
                System.setProperty(LogFactoryImpl.LOG_PROPERTY, oldValue);
            }
        }

        project.addCompileSourceRoot(outputDirectory);
    }

    /**
     * Locate all jsp files in the webapp. Used if no explicit jsps are
     * specified.
     */
    public void scanFiles(File base) {
        Stack<File> dirs = new Stack<File>();
        dirs.push(base);

        while (!dirs.isEmpty()) {
            File f = dirs.pop();
            if (f.exists() && f.isDirectory()) {
                String[] files = f.list();
                String ext;
                for (int i = 0; (files != null) && i < files.length; i++) {
                    File f2 = new File(f, files[i]);
                    if (f2.isDirectory()) {
                        dirs.push(f2);
                    } else {
                        ext = files[i].substring(files[i].lastIndexOf('.') + 1);
                        if (getExtensions().contains(ext)) {
                            pages.add(f2.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the compilation.
     *
     * @throws JasperException If an error occurs
     */
    private void executeInternal() throws JasperException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("execute() starting for " + pages.size() + " pages.");
        }

        try {
            if (context == null) {
                initServletContext();
            }

            // No explicit pages, we'll process all .jsp in the webapp
            if (pages.size() == 0) {
                scanFiles(sourceDirectory);
            }

            File uriRootF = new File(uriSourceRoot);
            if (!uriRootF.exists() || !uriRootF.isDirectory()) {
                throw new JasperException("The source location '"
                    + uriSourceRoot + "' must be an existing directory");
            }

            for (String nextjsp : pages) {
                File fjsp = new File(nextjsp);
                if (!fjsp.isAbsolute()) {
                    fjsp = new File(uriRootF, nextjsp);
                }
                if (!fjsp.exists()) {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn("JSP file " + fjsp + " does not exist");
                    }
                    continue;
                }
                String s = fjsp.getAbsolutePath();
                if (s.startsWith(uriSourceRoot)) {
                    nextjsp = s.substring(uriSourceRoot.length());
                }
                if (nextjsp.startsWith("." + File.separatorChar)) {
                    nextjsp = nextjsp.substring(2);
                }

                processFile(nextjsp);
            }

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                rootCause.printStackTrace();
            }
            throw je;

        } catch (/* IO */Exception ioe) {
            throw new JasperException(ioe);
        }
    }

    private void processFile(String file) throws JasperException {
        ClassLoader originalClassLoader = null;

        try {
            String jspUri = file.replace('\\', '/');
            JspCompilationContext clctxt = new JspCompilationContext(jspUri,
                false, this, context, null, rctxt);

            // write to a specific servlet package
            clctxt.setServletPackageName(servletPackage);

            originalClassLoader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                initClassLoader();
            }
            Thread.currentThread().setContextClassLoader(loader);

            // we only use the class loader and do not need the class path
            clctxt.setClassLoader(loader);
            clctxt.setClassPath(null);

            Compiler clc = clctxt.createCompiler();

            // If compile is set, generate both .java and .class, if
            // .jsp file is newer than .class file;
            // Otherwise only generate .java, if .jsp file is newer than
            // the .java file
            if (clc.isOutDated(compile)) {
                clc.compile(compile, true);

                if (showSuccess) {
                    getLog().info("Built File: " + file);
                }
            } else if (showSuccess) {
                getLog().info("File up to date: " + file);
            }

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                getLog().error("General problem compiling " + file, rootCause);
            }

            // Bugzilla 35114.
            if (failOnError) {
                throw je;
            }

            // just log otherwise
            getLog().error(je.getMessage());

        } catch (Exception e) {
            if ((e instanceof FileNotFoundException)
                && getLog().isWarnEnabled()) {
                getLog().warn("Missing file: " + e.getMessage());
            }
            throw new JasperException(e);
        } finally {
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    originalClassLoader);
            }
        }
    }

    // ---------- Additional Settings ------------------------------------------

    private Set<String> getExtensions() {
        if (jspFileExtensionSet == null) {
            jspFileExtensionSet = new HashSet<String>();

            // fallback default value, should actually be set by Maven
            if (jspFileExtensions == null) {
                jspFileExtensions = "jsp,jspx";
            }

            StringTokenizer st = new StringTokenizer(jspFileExtensions, ",");
            while (st.hasMoreTokens()) {
                String ext = st.nextToken().trim();
                if (ext.length() > 0) {
                    jspFileExtensionSet.add(ext);
                }
            }
        }

        return jspFileExtensionSet;
    }

    private void initServletContext() {
        try {
            context = new JspCServletContext(getLog(), new URL("file:"
                + uriSourceRoot.replace('\\', '/') + '/'));
            tldLocationsCache = new TldLocationsCache(context, true);
        } catch (MalformedURLException me) {
            getLog().error("Cannot setup ServletContext", me);
        }

        rctxt = new JspRuntimeContext(context, this);
        jspConfig = new JspConfig(context);
        tagPluginManager = new TagPluginManager(context);
    }

    /**
     * Initializes the classloader as/if needed for the given compilation
     * context.
     *
     * @param clctxt The compilation context
     * @throws IOException If an error occurs
     */
    private void initClassLoader() throws IOException,
            DependencyResolutionRequiredException {
        List artifacts = project.getCompileArtifacts();
        URL[] path = new URL[artifacts.size() + 1];
        int i = 0;
        for (Iterator ai=artifacts.iterator(); ai.hasNext(); ) {
            Artifact a = (Artifact) ai.next();
            path[i++] = a.getFile().toURI().toURL();
        }
        final String targetDirectory = project.getBuild().getOutputDirectory();
        path[path.length - 1] = new File(targetDirectory).toURI().toURL();
        loader = new URLClassLoader(path, this.getClass().getClassLoader());
    }

    // ---------- Options interface --------------------------------------------

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#genStringAsCharArray()
     */
    public boolean genStringAsCharArray() {
        return jasperGenStringAsCharArray;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isCaching()
     */
    public boolean isCaching() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCache()
     */
    public Map<String, TreeNode> getCache() {
        if (tldCache == null) {
            tldCache = new HashMap<String, TreeNode>();
        }

        return tldCache;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCheckInterval()
     */
    public int getCheckInterval() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getClassDebugInfo()
     */
    public boolean getClassDebugInfo() {
        return jasperClassDebugInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getClassPath()
     */
    public String getClassPath() {
        // no extra classpath
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompiler()
     */
    public String getCompiler() {
        // use JDTCompiler, which is the default
        return null;
    }

    public String getCompilerClassName() {
        // use JDTCompiler, which is the default
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerSourceVM()
     */
    public String getCompilerSourceVM() {
        return compilerSourceVM;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerTargetVM()
     */
    public String getCompilerTargetVM() {
        return compilerTargetVM;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getDevelopment()
     */
    public boolean getDevelopment() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getErrorOnUseBeanInvalidClassAttribute()
     */
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        // not configurable
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getFork()
     */
    public boolean getFork() {
        // certainly don't fork (not required anyway)
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getIeClassId()
     */
    public String getIeClassId() {
        return jasperIeClassId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJavaEncoding()
     */
    public String getJavaEncoding() {
        return "UTF-8";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJspClassLoader()
     */
    public ClassLoader getJspClassLoader() {
        // no JSP ClassLoader, use default
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJspConfig()
     */
    public JspConfig getJspConfig() {
        return jspConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getKeepGenerated()
     */
    public boolean getKeepGenerated() {
        return jasperKeepGenerated;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getMappedFile()
     */
    public boolean getMappedFile() {
        return jasperMappedFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getModificationTestInterval()
     */
    public int getModificationTestInterval() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getScratchDir()
     */
    public String getScratchDir() {
        return outputDirectory;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getSendErrorToClient()
     */
    public boolean getSendErrorToClient() {
        // certainly output any problems
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTagPluginManager()
     */
    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTldLocationsCache()
     */
    public TldLocationsCache getTldLocationsCache() {
        return tldLocationsCache;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTrimSpaces()
     */
    public boolean getTrimSpaces() {
        return jasperTrimSpaces;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isPoolingEnabled()
     */
    public boolean isPoolingEnabled() {
        return jasperEnablePooling;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isSmapDumped()
     */
    public boolean isSmapDumped() {
        // always include the SMAP (optionally, limit to if debugging)
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isSmapSuppressed()
     */
    public boolean isSmapSuppressed() {
        // require SMAP
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isXpoweredBy()
     */
    public boolean isXpoweredBy() {
        // no XpoweredBy setting please
        return false;
    }

    public boolean getDisplaySourceFragment() {
        // Display the source fragment on errors for maven compilation
        return true;
    }
}

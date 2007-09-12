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

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * The <code>JspcMojo</code> TODO
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
    private String sourceDirectory;

    /**
     * Target folder for the compiled classes.
     *
     * @parameter expression="${jspc.outputDirectory}"
     *            default-value="${project.build.outputDirectory}"
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
     * @parameter expression="${jspc.failOnError}" default-value="false"
     */
    private boolean failOnError;

    /**
     * @parameter expression="${jspc.showSuccess}" default-value="false"
     */
    private boolean showSuccess;

    /**
     * @parameter expression="${jspc.compilerTargetVM}" default-value="1.4"
     */
    private String compilerTargetVM;

    /**
     * @parameter expression="${jspc.compilerSourceVM}" default-value="1.4"
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
     * @parameter expression="${jspc.servletPackage}" default=""
     */
    private String servletPackage;

    private Set jspFileExtensionSet;

    private boolean compile = true;

    private String uriSourceRoot;

    private List pages = new ArrayList();

    private ServletContext context;

    private JspRuntimeContext rctxt;

    private URLClassLoader loader = null;

    private Map tldCache;

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
            this.uriSourceRoot = new File(this.sourceDirectory).getCanonicalPath();
        } catch (Exception e) {
            this.uriSourceRoot = new File(this.sourceDirectory).getAbsolutePath();
        }

        // scan all JSP file
        // scanFiles(new File(sourceDirectory));

        // have the files compiled
        try {
            this.executeInternal();
        } catch (JasperException je) {
            this.getLog().error("Compilation Failure", je);
            throw new MojoExecutionException(je.getMessage(), je);
        }
    }

    /**
     * Locate all jsp files in the webapp. Used if no explicit jsps are
     * specified.
     */
    public void scanFiles(File base) {
        Stack dirs = new Stack();
        dirs.push(base);

        while (!dirs.isEmpty()) {
            String s = dirs.pop().toString();
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                String[] files = f.list();
                String ext;
                for (int i = 0; (files != null) && i < files.length; i++) {
                    File f2 = new File(s, files[i]);
                    if (f2.isDirectory()) {
                        dirs.push(f2.getPath());
                    } else {
                        ext = files[i].substring(files[i].lastIndexOf('.') + 1);
                        if (this.getExtensions().contains(ext)) {
                            this.pages.add(f2.getAbsolutePath());
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
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("execute() starting for " + this.pages.size() + " pages.");
        }

        try {
            if (this.context == null) {
                this.initServletContext();
            }

            // No explicit pages, we'll process all .jsp in the webapp
            if (this.pages.size() == 0) {
                this.scanFiles(new File(this.sourceDirectory));
            }

            File uriRootF = new File(this.uriSourceRoot);
            if (!uriRootF.exists() || !uriRootF.isDirectory()) {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.jspc.uriroot_not_dir"));
            }

            Iterator iter = this.pages.iterator();
            while (iter.hasNext()) {
                String nextjsp = iter.next().toString();
                File fjsp = new File(nextjsp);
                if (!fjsp.isAbsolute()) {
                    fjsp = new File(uriRootF, nextjsp);
                }
                if (!fjsp.exists()) {
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn(
                            Localizer.getMessage("jspc.error.fileDoesNotExist",
                                fjsp.toString()));
                    }
                    continue;
                }
                String s = fjsp.getAbsolutePath();
                if (s.startsWith(this.uriSourceRoot)) {
                    nextjsp = s.substring(this.uriSourceRoot.length());
                }
                if (nextjsp.startsWith("." + File.separatorChar)) {
                    nextjsp = nextjsp.substring(2);
                }
                this.processFile(nextjsp);
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
                false, this, this.context, null, this.rctxt);

            // write to a specific servlet package
            clctxt.setServletPackageName(this.servletPackage);

            originalClassLoader = Thread.currentThread().getContextClassLoader();
            if (this.loader == null) {
                this.initClassLoader(clctxt);
            }
            Thread.currentThread().setContextClassLoader(this.loader);

            // we only use the class loader and do not need the class path
            clctxt.setClassLoader(this.loader);
            clctxt.setClassPath(null);

            Compiler clc = clctxt.createCompiler();

            // If compile is set, generate both .java and .class, if
            // .jsp file is newer than .class file;
            // Otherwise only generate .java, if .jsp file is newer than
            // the .java file
            if (clc.isOutDated(this.compile)) {
                clc.compile(this.compile, true);

                if (this.showSuccess) {
                    this.getLog().info("Built File: " + file);
                }
            } else if (this.showSuccess) {
                this.getLog().info("File up to date: " + file);
            }

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                this.getLog().error(
                    Localizer.getMessage("jspc.error.generalException", file),
                    rootCause);
            }

            // Bugzilla 35114.
            if (this.failOnError) {
                throw je;
            } else {
                this.getLog().error(je.getMessage());
            }

        } catch (Exception e) {
            if ((e instanceof FileNotFoundException)
                && this.getLog().isWarnEnabled()) {
                this.getLog().warn(
                    Localizer.getMessage("jspc.error.fileDoesNotExist",
                        e.getMessage()));
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

    private Set getExtensions() {
        if (this.jspFileExtensionSet == null) {
            this.jspFileExtensionSet = new HashSet();

            // fallback default value, should actually be set by Maven
            if (this.jspFileExtensions == null) {
                this.jspFileExtensions = "jsp,jspx";
            }

            StringTokenizer st = new StringTokenizer(this.jspFileExtensions, ",");
            while (st.hasMoreTokens()) {
                String ext = st.nextToken().trim();
                if (ext.length() > 0) {
                    this.jspFileExtensionSet.add(ext);
                }
            }
        }

        return this.jspFileExtensionSet;
    }

    private void initServletContext() {
        try {
            this.context = new JspCServletContext(this.getLog(), new URL("file:"
                + this.uriSourceRoot.replace('\\', '/') + '/'));
            this.tldLocationsCache = new TldLocationsCache(this.context, true);
        } catch (MalformedURLException me) {
            this.getLog().error("Cannot setup ServletContext", me);
        }

        this.rctxt = new JspRuntimeContext(this.context, this);
        this.jspConfig = new JspConfig(this.context);
        this.tagPluginManager = new TagPluginManager(this.context);
    }

    /**
     * Initializes the classloader as/if needed for the given compilation
     * context.
     *
     * @param clctxt The compilation context
     * @throws IOException If an error occurs
     */
    private void initClassLoader(JspCompilationContext clctxt)
            throws IOException, DependencyResolutionRequiredException {

        // Turn the classPath into URLs
        List classPath = this.project.getCompileClasspathElements();
        ArrayList urls = new ArrayList();
        for (Iterator cpi = classPath.iterator(); cpi.hasNext();) {
            String path = (String) cpi.next();
            urls.add(new File(path).toURI().toURL());
        }

        URL urlsA[] = new URL[urls.size()];
        urls.toArray(urlsA);
        this.loader = new URLClassLoader(urlsA, this.getClass().getClassLoader());
    }

    // ---------- Options interface --------------------------------------------

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#genStringAsCharArray()
     */
    public boolean genStringAsCharArray() {
        return this.jasperGenStringAsCharArray;
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
    public Map getCache() {
        if (this.tldCache == null) {
            this.tldCache = new HashMap();
        }

        return this.tldCache;
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
        return this.jasperClassDebugInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getClassPath()
     */
    public String getClassPath() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompiler()
     */
    public String getCompiler() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerSourceVM()
     */
    public String getCompilerSourceVM() {
        return this.compilerSourceVM;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerTargetVM()
     */
    public String getCompilerTargetVM() {
        return this.compilerTargetVM;
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
        return this.jasperIeClassId;
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
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJspConfig()
     */
    public JspConfig getJspConfig() {
        return this.jspConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getKeepGenerated()
     */
    public boolean getKeepGenerated() {
        return this.jasperKeepGenerated;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getMappedFile()
     */
    public boolean getMappedFile() {
        return this.jasperMappedFile;
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
        return this.outputDirectory;
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
        return this.tagPluginManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTldLocationsCache()
     */
    public TldLocationsCache getTldLocationsCache() {
        return this.tldLocationsCache;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTrimSpaces()
     */
    public boolean getTrimSpaces() {
        return this.jasperTrimSpaces;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isPoolingEnabled()
     */
    public boolean isPoolingEnabled() {
        return this.jasperEnablePooling;
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
}

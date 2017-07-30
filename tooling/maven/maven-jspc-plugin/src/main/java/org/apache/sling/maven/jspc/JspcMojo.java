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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.impl.EclipseJavaCompiler;
import org.apache.sling.scripting.jsp.jasper.IOProvider;
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.JspCompilationContext;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspConfig;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.compiler.TagPluginManager;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * The <code>JspcMojo</code> is implements the Sling Maven JspC goal
 * <code>jspc</code> compiling JSP into the target and creating a component
 * descriptor for Declarative Services to use the JSP with the help of the
 * appropriate adapter as component.
 */
@Mojo(name = "jspc", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class JspcMojo extends AbstractMojo implements Options {

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Location of the JSP source file. 
     */
    @Parameter( property = "jspc.sourceDirectory", defaultValue = "${project.build.scriptSourceDirectory}")
    private File sourceDirectory;

    /**
     * List of alternative resource directories used during compiling.
     */
    @Parameter
    private File[] resourceDirectories = new File[0];

    /**
     * Target directory for the compiled JSP classes.
     */
    @Parameter ( property = "jspc.outputDirectory", defaultValue = "${project.build.outputDirectory}")
    private String outputDirectory;

    @Parameter ( property = "jspc.jasper.classdebuginfo", defaultValue = "true")
    private boolean jasperClassDebugInfo;

    @Parameter ( property = "jspc.jasper.enablePooling", defaultValue = "true")
    private boolean jasperEnablePooling;

    @Parameter ( property = "jspc.jasper.ieClassId", defaultValue = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93")
    private String jasperIeClassId;

    @Parameter ( property = "jspc.jasper.genStringAsCharArray", defaultValue = "false")
    private boolean jasperGenStringAsCharArray;

    @Parameter ( property = "jspc.jasper.keepgenerated", defaultValue = "true")
    private boolean jasperKeepGenerated;

    @Parameter ( property = "jspc.jasper.mappedfile", defaultValue = "true")
    private boolean jasperMappedFile;

    @Parameter ( property = "jspc.jasper.trimSpaces", defaultValue = "false")
    private boolean jasperTrimSpaces;

    @Parameter ( property = "jspc.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Parameter ( property = "jspc.showSuccess", defaultValue = "false")
    private boolean showSuccess;

    /**
     * The Target Virtual Machine Version to generate class files for.
     */
    @Parameter ( property = "jspc.compilerTargetVM", defaultValue = "1.5")
    private String compilerTargetVM;

    /**
     * The Compiler Source Version of the Java source generated from the JSP files before compiling into classes.
     */
    @Parameter ( property = "jspc.compilerSourceVM", defaultValue = "1.5")
    private String compilerSourceVM;

    /**
     * Prints a compilation report by listing all the packages and dependencies that were used during processing the JSPs.
     */
    @Parameter ( property = "jspc.printCompilationReport", defaultValue = "false")
    private boolean printCompilationReport;

    /**
     * Comma separated list of extensions of files to be compiled by the plugin.
     * @deprecated Use the {@link #includes} filter instead.
     */
    @Deprecated
    @Parameter ( property = "jspc.jspFileExtensions", defaultValue = "jsp,jspx")
    private String jspFileExtensions;

    /**
     * @deprecated Due to internal refactoring, this is not longer supported.
     */
    @Deprecated
    @Parameter ( property = "jspc.servletPackage", defaultValue = "org.apache.jsp")
    private String servletPackage;

    /**
     * Included JSPs, defaults to <code>"**&#47;*.jsp"</code>
     */
    @Parameter
    private String[] includes;

    /**
     * Excluded JSPs, empty by default
     */
    @Parameter
    private String[] excludes;

    private String uriSourceRoot;

    private List<String> pages = new ArrayList<String>();

    private JspCServletContext context;

    private JspRuntimeContext rctxt;

    private TrackingClassLoader loader;

    private List<Artifact> jspcCompileArtifacts;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache;

    private JspConfig jspConfig;

    private TagPluginManager tagPluginManager;

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
            System.setProperty(LogFactoryImpl.LOG_PROPERTY, SimpleLog.class.getName());
            executeInternal();
            if (printCompilationReport) {
                printCompilationReport();
            }
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
    private void scanFiles(File base) {

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(base);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();

        Collections.addAll(pages, scanner.getIncludedFiles());
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

            if (includes == null) {
                includes = new String[]{ "**/*.jsp" };
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

    private void processFile(final String file) throws JasperException {
        try {
            final String jspUri = file.replace('\\', '/');
            final JspCompilationContext clctxt = new JspCompilationContext(jspUri, false, this, context, rctxt, false);

            JasperException error = clctxt.compile();
            if (error != null) {
                throw error;
            }
            if (showSuccess) {
                getLog().info("Built File: " + file);
            }

        } catch (final JasperException je) {
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
            throw new JasperException(e);
        }
    }

    // ---------- Additional Settings ------------------------------------------

    private void initServletContext() throws IOException, DependencyResolutionRequiredException {
        if (loader == null) {
            initClassLoader();
        }

        context = new JspCServletContext(getLog(), new URL("file:" + uriSourceRoot.replace('\\', '/') + '/'));
        for (File resourceDir: resourceDirectories) {
            String root = resourceDir.getCanonicalPath().replace('\\', '/');
            URL altUrl = new URL("file:" + root + "/");
            context.addAlternativeBaseURL(altUrl);
        }

        tldLocationsCache = new JspCTldLocationsCache(context, true, loader);

        JavaCompiler compiler = new EclipseJavaCompiler();
        ClassLoaderWriter writer = new JspCClassLoaderWriter(loader, new File(outputDirectory));
        IOProvider ioProvider = new JspCIOProvider(loader, compiler, writer);
        rctxt = new JspRuntimeContext(context, this, ioProvider);
        jspConfig = new JspConfig(context);
        tagPluginManager = new TagPluginManager(context);
    }


    /**
     * Initializes the classloader as/if needed for the given compilation
     * context.
     *
     * @throws IOException If an error occurs
     */
    private void initClassLoader() throws IOException,
            DependencyResolutionRequiredException {
        List<URL> classPath = new ArrayList<URL>();
        // add output directory to classpath
        final String targetDirectory = project.getBuild().getOutputDirectory();
        classPath.add(new File(targetDirectory).toURI().toURL());

        // add artifacts from project
        Set<Artifact> artifacts = project.getDependencyArtifacts();
        jspcCompileArtifacts = new ArrayList<Artifact>(artifacts.size());
        for (Artifact a: artifacts) {
            final String scope = a.getScope();
            if ("provided".equals(scope) || "runtime".equals(scope) || "compile".equals(scope)) {
                // we need to exclude the javax.servlet.jsp API, otherwise the taglib parser causes problems (see note below)
                if (containsProblematicPackage(a.getFile())) {
                    continue;
                }
                classPath.add(a.getFile().toURI().toURL());
                jspcCompileArtifacts.add(a);
            }
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Compiler classpath:");
            for (URL u: classPath) {
                getLog().debug("  " + u);
            }
        }

        // this is dangerous to use this classloader as parent as the compilation will depend on the classes provided
        // in the plugin dependencies. but if we omit this, we get errors by not being able to load the TagExtraInfo classes.
        // this is because this plugin uses classes from the javax.servlet.jsp that are also loaded via the TLDs.
        loader = new TrackingClassLoader(classPath.toArray(new URL[classPath.size()]), this.getClass().getClassLoader());
    }

    /**
     * Checks if the given jar file contains a problematic java API that should be excluded from the classloader.
     * @param file the file to check
     * @return {@code true} if it contains a problematic package
     * @throws IOException if an error occurrs.
     */
    private boolean containsProblematicPackage(File file) throws IOException {
        JarFile jar = new JarFile(file);
        boolean isJSPApi = jar.getEntry("/javax/servlet/jsp/JspPage.class") != null;
        jar.close();
        return isJSPApi;
    }

    /**
     * Prints the dependency report.
     */
    private void printCompilationReport() {
        if (loader == null) {
            return;
        }

        // first scan all the dependencies for their classes
        Map<String, Set<String>> artifactsByPackage = new HashMap<String, Set<String>>();
        Set<String> usedDependencies = new HashSet<String>();
        for (Artifact a: jspcCompileArtifacts) {
            scanArtifactPackages(artifactsByPackage, a);
            usedDependencies.add(a.getId());
        }

        // create the report
        StringBuilder report = new StringBuilder("JSP compilation report:\n\n");
        List<String> packages = new ArrayList<String>(loader.getPackageNames());
        int pad = 10;
        for (String packageName: artifactsByPackage.keySet()) {
            pad = Math.max(pad, packageName.length());
        }
        pad += 2;
        report.append(StringUtils.rightPad("Package", pad)).append("Dependency");
        report.append("\n---------------------------------------------------------------\n");
        Collections.sort(packages);
        for (String packageName: packages) {
            report.append(StringUtils.rightPad(packageName, pad));
            Set<String> a = artifactsByPackage.get(packageName);
            if (a == null || a.isEmpty()) {
                report.append("n/a");
            } else {
                StringBuilder ids = new StringBuilder();
                for (String id: a) {
                    usedDependencies.remove(id);
                    if (ids.length() > 0) {
                        ids.append(", ");
                    }
                    ids.append(id);
                }
                report.append(ids);
            }
            report.append("\n");
        }

        // print the unused dependencies
        report.append("\n");
        report.append(usedDependencies.size()).append(" dependencies not used by JSPs:\n");
        if (!usedDependencies.isEmpty()) {
            report.append("---------------------------------------------------------------\n");
            for (String id: usedDependencies) {
                report.append(id).append("\n");
            }
        }

        // create the package list that are double defined
        int doubleDefined = 0;
        StringBuilder msg = new StringBuilder();
        packages = new ArrayList<String>(artifactsByPackage.keySet());
        Collections.sort(packages);
        for (String packageName: packages) {
            Set<String> a = artifactsByPackage.get(packageName);
            if (a != null && a.size() > 1) {
                doubleDefined++;
                msg.append(StringUtils.rightPad(packageName, pad));
                msg.append(StringUtils.join(a.iterator(), ", ")).append("\n");
            }
        }
        report.append("\n");
        report.append(doubleDefined).append(" packages are multiply defined by dependencies:\n");
        if (doubleDefined > 0) {
            report.append("---------------------------------------------------------------\n");
            report.append(msg);
        }

        getLog().info(report);
    }

    /**
     * Scans the given artifact for classes and add their packages to the given map.
     * @param artifactsByPackage the package to artifact lookup map
     * @param a the artifact to scan
     */
    private void scanArtifactPackages(Map<String, Set<String>> artifactsByPackage, Artifact a) {
        try {
            JarFile jar = new JarFile(a.getFile());
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String path = e.getName();
                if (path.endsWith(".class")) {
                    path = StringUtils.chomp(path, "/");
                    if (path.charAt(0) == '/') {
                        path = path.substring(1);
                    }
                    String packageName = path.replaceAll("/", ".");
                    Set<String> artifacts = artifactsByPackage.get(packageName);
                    if (artifacts == null) {
                        artifacts = new HashSet<String>();
                        artifactsByPackage.put(packageName, artifacts);
                    }
                    artifacts.add(a.getId());
                }
            }
        } catch (IOException e) {
            getLog().error("Error while accessing jar file: " + e.getMessage());
        }
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
     * @see org.apache.jasper.Options#getClassDebugInfo()
     */
    public boolean getClassDebugInfo() {
        return jasperClassDebugInfo;
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

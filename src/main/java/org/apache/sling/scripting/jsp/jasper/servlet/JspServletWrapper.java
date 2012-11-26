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
package org.apache.sling.scripting.jsp.jasper.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.TagInfo;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.scripting.jsp.SlingPageException;
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.JspCompilationContext;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.ErrorDispatcher;
import org.apache.sling.scripting.jsp.jasper.compiler.JavacErrorDetail;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.compiler.Localizer;
import org.apache.sling.scripting.jsp.jasper.runtime.AnnotationProcessor;
import org.apache.sling.scripting.jsp.jasper.runtime.JspSourceDependent;

/**
 * The JSP engine (a.k.a Jasper).
 *
 * The servlet container is responsible for providing a
 * URLClassLoader for the web application context Jasper
 * is being used in. Jasper will try get the Tomcat
 * ServletContext attribute for its ServletContext class
 * loader, if that fails, it uses the parent class loader.
 * In either case, it must be a URLClassLoader.
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Remy Maucherat
 * @author Kin-man Chung
 * @author Glenn Nielsen
 * @author Tim Fennell
 */

public class JspServletWrapper {

    // Logger
    private final Log log = LogFactory.getLog(JspServletWrapper.class);

    private final ServletConfig config;
    private final Options options;
    private final boolean isTagFile;
    private final String jspUri;
    private final JspCompilationContext ctxt;

    private volatile Servlet theServlet;
    private volatile Class<?> tagFileClass;

    private volatile long available = 0L;
    private volatile JasperException compileException;
    private volatile int tripCount;

    private volatile List<String> dependents;

    /**
     * JspServletWrapper for JSP pages.
     */
    public JspServletWrapper(final ServletConfig config,
            final Options options,
            final String jspUri,
            final boolean isErrorPage,
            final JspRuntimeContext rctxt,
            final boolean defaultIsSession) {
	    this.isTagFile = false;
        this.config = config;
        this.options = options;
        this.jspUri = jspUri;
        this.ctxt = new JspCompilationContext(jspUri, isErrorPage, options,
					 config.getServletContext(),
					 rctxt, defaultIsSession);
        if ( log.isDebugEnabled() ) {
            log.debug("Creating new wrapper for servlet " + jspUri);
        }
    }

    /**
     * JspServletWrapper for tag files.
     */
    public JspServletWrapper(final ServletContext servletContext,
			     final Options options,
			     final String tagFilePath,
			     final TagInfo tagInfo,
			     final JspRuntimeContext rctxt,
			     final boolean defaultIsSession,
			     final URL tagFileJarUrl)
    throws JasperException {
        this.isTagFile = true;
        this.config = null;	// not used
        this.options = options;
        this.jspUri = tagFilePath;
        this.ctxt = new JspCompilationContext(jspUri, tagInfo, options,
					 servletContext, rctxt, defaultIsSession,
					 tagFileJarUrl);
        if ( log.isDebugEnabled() ) {
            log.debug("Creating new wrapper for tagfile " + jspUri);
        }
    }

    public JspCompilationContext getJspEngineContext() {
        return ctxt;
    }

    public boolean isValid() {
        if ( theServlet != null ) {
            if ( theServlet.getClass().getClassLoader() instanceof DynamicClassLoader ) {
                return ((DynamicClassLoader)theServlet.getClass().getClassLoader()).isLive();
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Servlet loadServlet()
    throws ServletException, IOException {
        Servlet servlet = null;

        try {
            if ( log.isDebugEnabled() ) {
                log.debug("Loading servlet " + jspUri);
            }
            servlet = (Servlet) ctxt.load().newInstance();
            AnnotationProcessor annotationProcessor = (AnnotationProcessor) config.getServletContext().getAttribute(AnnotationProcessor.class.getName());
            if (annotationProcessor != null) {
               annotationProcessor.processAnnotations(servlet);
               annotationProcessor.postConstruct(servlet);
            }
            // update dependents
            final List<String> oldDeps = this.dependents;
            if (servlet != null && servlet instanceof JspSourceDependent) {
                this.dependents = (List<String>) ((JspSourceDependent) servlet).getDependants();
                if ( this.dependents == null ) {
                    this.dependents = Collections.EMPTY_LIST;
                }
                this.ctxt.getRuntimeContext().addJspDependencies(this, this.dependents);
            }
            if ( !equals(oldDeps, this.dependents) ) {
                this.persistDependencies();
            }
        } catch (final IllegalAccessException e) {
            throw new JasperException(e);
        } catch (final InstantiationException e) {
            throw new JasperException(e);
        } catch (final Exception e) {
            throw new JasperException(e);
        }

        servlet.init(config);

        return servlet;
    }

    /**
     * Get the name of the dependencies file.
     */
    public String getDependencyFilePath() {
        final String name;
        if (isTagFile) {
            name = this.ctxt.getTagInfo().getTagClassName();
        } else {
            name = this.ctxt.getServletPackageName() + "." + this.ctxt.getServletClassName();
        }

        final String path = ":/" + name.replace('.', '/') + ".deps";
        return path;
    }

    /**
     * Persist dependencies
     */
    private void persistDependencies() {
        final String path = this.getDependencyFilePath();
        if ( log.isDebugEnabled() ) {
            log.debug("Writing dependencies for " + jspUri);
        }
        if ( this.dependents != null && this.dependents.size() > 0 ) {
            OutputStream os = null;
            try {
                os = this.ctxt.getRuntimeContext().getIOProvider().getOutputStream(path);
                final OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                for(final String dep : this.dependents) {
                    writer.write(dep);
                    writer.write("\n");
                }
                writer.flush();
            } catch ( final IOException ioe) {
                log.warn("Unable to write dependenies file " + path + " : " + ioe.getMessage(), ioe);
            } finally {
                if ( os != null ) {
                    try {
                        os.close();
                    } catch (final IOException ioe) {
                        // ignore
                    }
                }
            }
        } else {
            this.ctxt.getRuntimeContext().getIOProvider().delete(path);
        }
    }

    /**
     * Compile (if needed) and load a tag file
     */
    @SuppressWarnings("unchecked")
    public Class<?> loadTagFile() throws JasperException {
        if ( compileException != null ) {
            throw compileException;
        }

        if ( this.tagFileClass == null ) {
            synchronized (this) {
                if ( this.tagFileClass == null ) {
                    if ( log.isDebugEnabled() ) {
                        log.debug("Compiling tagfile " + jspUri);
                    }
                    this.compileException = ctxt.compile();
                    if ( compileException != null ) {
                        throw compileException;
                    }
                    if ( log.isDebugEnabled() ) {
                        log.debug("Loading tagfile " + jspUri);
                    }
                    this.tagFileClass = this.ctxt.load();
                    try {
                        final Object tag = this.tagFileClass.newInstance();
                        // update dependents
                        final List<String> oldDeps = this.dependents;
                        if (tag != null && tag instanceof JspSourceDependent) {
                            this.dependents = (List<String>) ((JspSourceDependent) tag).getDependants();
                            this.ctxt.getRuntimeContext().addJspDependencies(this, this.dependents);
                            if ( this.dependents == null ) {
                                this.dependents = Collections.EMPTY_LIST;
                            }
                        }
                        if ( !equals(oldDeps, this.dependents) ) {
                            this.persistDependencies();
                        }
                    } catch (final Throwable t) {
                        // ignore
                    }
                }
            }
        }
        return this.tagFileClass;
    }

    /**
     * Compile and load a prototype for the Tag file.  This is needed
     * when compiling tag files with circular dependencies.  A prototpe
     * (skeleton) with no dependencies on other other tag files is
     * generated and compiled.
     */
    public Class<?> loadTagFilePrototype() throws JasperException {
    	ctxt.setPrototypeMode(true);
    	try {
    	    return loadTagFile();
    	} finally {
    	    ctxt.setPrototypeMode(false);
    	}
    }

    /**
     * Get a list of files that the current page has source dependency on.
     */
    public List<String> getDependants() {
        if ( this.dependents == null ) {
            synchronized ( this ) {
                if ( this.dependents == null ) {
                    // we load the deps file
                    final String path = this.getDependencyFilePath();
                    InputStream is = null;
                    try {
                        is = this.ctxt.getRuntimeContext().getIOProvider().getInputStream(path);
                        if ( is != null ) {
                            if ( log.isDebugEnabled() ) {
                                log.debug("Loading dependencies for " + jspUri);
                            }
                            final List<String> deps = new ArrayList<String>();
                            final InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                            final LineNumberReader lnr = new LineNumberReader(reader);
                            String line;
                            while ( (line = lnr.readLine()) != null ) {
                                deps.add(line.trim());
                            }
                            this.dependents = deps;
                        }
                    } catch ( final IOException ignore ) {
                        // excepted
                    } finally {
                        if ( is != null ) {
                            try { is.close(); } catch ( final IOException ioe ) {}
                        }
                    }

                    // use empty list, until servlet is compiled and loaded
                    if ( this.dependents == null ) {
                        this.dependents = Collections.emptyList();
                    }
                }
            }
        }
        return this.dependents;
    }

    public boolean isTagFile() {
        return this.isTagFile;
    }

    public int incTripCount() {
        return tripCount++;
    }

    public int decTripCount() {
        return tripCount--;
    }

    public String getJspUri() {
        return jspUri;
    }

    /**
     * Check if the compiled class is still current
     */
    private boolean isOutDated() {
        // check if class file exists
        final String targetFile = ctxt.getClassFileName();
        final long targetLastModified = ctxt.getRuntimeContext().getIOProvider().lastModified(targetFile);
        if (targetLastModified < 0) {
            return true;
        }

        // compare jsp time stamp with class file time stamp
        final String jsp = ctxt.getJspFile();
        final long jspRealLastModified = ctxt.getRuntimeContext().getIOProvider().lastModified(jsp);
        if (targetLastModified < jspRealLastModified) {
            if (log.isDebugEnabled()) {
                log.debug("Compiler: outdated: " + targetFile + " "
                        + targetLastModified);
            }
            return true;
        }

        // check includes
        final List<String> depends = this.getDependants();
        if (depends != null) {
            final Iterator<String> it = depends.iterator();
            while (it.hasNext()) {
                final String include = it.next();
                // ignore tag libs, we are reloaded if a taglib changes anyway
                if ( include.startsWith("tld:") ) {
                    continue;
                }
                final long includeLastModified = ctxt.getRuntimeContext().getIOProvider().lastModified(include);

                if (includeLastModified > targetLastModified) {
                    if (log.isDebugEnabled()) {
                        log.debug("Compiler: outdated: " + targetFile + " because of dependency " + include + " : "
                                + targetLastModified + " - " + includeLastModified);
                    }
                    return true;
                }
            }
        }

        return false;

    }

    /**
     * Prepare the servlet:
     * - compile it if it either hasn't been compiled yet or is out dated
     * - load the servlet
     *
     */
    private void prepareServlet(final HttpServletRequest request,
            final HttpServletResponse response)
    throws IOException, ServletException {
        if ( isOutDated() ) {
            // Compile...
            if ( log.isDebugEnabled() ) {
                log.debug("Compiling servlet " + this.jspUri);
            }
            this.compileException = ctxt.compile();
            if ( compileException != null ) {
                throw compileException;
            }
        }

        // (Re)load servlet class file
        this.theServlet = this.loadServlet();
    }

    /**
     * @param bindings
     * @throws SlingIOException
     * @throws SlingServletException
     * @throws IllegalArgumentException if the Jasper Precompile controller
     *             request parameter has an illegal value.
     */
    public void service(final SlingBindings bindings) {
        final SlingHttpServletRequest request = bindings.getRequest();
        final Object oldValue = request.getAttribute(SlingBindings.class.getName());
        try {
            request.setAttribute(SlingBindings.class.getName(), bindings);
            service(request, bindings.getResponse());
        } catch (SlingException se) {
            // rethrow as is
            throw se;
        } catch (IOException ioe) {
            throw new SlingIOException(ioe);
        } catch (ServletException se) {
            throw new SlingServletException(se);
        } finally {
            request.setAttribute(SlingBindings.class.getName(), oldValue);
        }
    }

    /**
     * Process the request.
     */
    public void service(final HttpServletRequest request,
                        final HttpServletResponse response)
	throws ServletException, IOException {
        try {
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                if (available > System.currentTimeMillis()) {
                    response.setDateHeader("Retry-After", available);
                    response.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                         Localizer.getMessage("jsp.error.unavailable"));
                    return;
                }
                // Wait period has expired. Reset.
                available = 0;
            }
            if ( theServlet == null ) {
                synchronized ( this ) {
                    if ( compileException != null ) {
                        throw compileException;
                    }
                    if ( theServlet == null ) {
                        this.prepareServlet(request, response);
                    }
                }
            }
            if ( compileException != null ) {
                throw compileException;
            }

            // Service request
            if (theServlet instanceof SingleThreadModel) {
               // sync on the wrapper so that the freshness
               // of the page is determined right before servicing
               synchronized (this) {
                   theServlet.service(request, response);
                }
            } else {
                theServlet.service(request, response);
            }

        } catch (final UnavailableException ex) {
            String includeRequestUri = (String)
                request.getAttribute("javax.servlet.include.request_uri");
            if (includeRequestUri != null) {
                // This file was included. Throw an exception as
                // a response.sendError() will be ignored by the
                // servlet engine.
                throw ex;
            }
            int unavailableSeconds = ex.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60;        // Arbitrary default
            }
            available = System.currentTimeMillis() +
                (unavailableSeconds * 1000L);
            response.sendError
                (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                 ex.getMessage());
            return;
        } catch (final ServletException ex) {
            handleJspException(ex);
        } catch (final IOException ex) {
            handleJspException(ex);
        } catch (final IllegalStateException ex) {
            handleJspException(ex);
        } catch (final SlingPageException ex) {
        	throw ex;
        }catch (final Exception ex) {
            handleJspException(ex);
        }
    }

    /**
     * Destroy this wrapper
     * @param deleteGeneratedFiles Should generated files be deleted as well?
     */
    public void destroy(final boolean deleteGeneratedFiles) {
        if ( this.isTagFile ) {
            if ( log.isDebugEnabled() ) {
                log.debug("Destroying tagfile " + jspUri);
            }
            this.tagFileClass = null;
            if ( deleteGeneratedFiles ) {
                if ( log.isDebugEnabled() ) {
                    log.debug("Deleting generated files for tagfile " + jspUri);
                }
                this.ctxt.getRuntimeContext().getIOProvider().delete(this.getDependencyFilePath());
            }
        } else {
            if ( log.isDebugEnabled() ) {
                log.debug("Destroying servlet " + jspUri);
            }
            if (theServlet != null) {
                if ( deleteGeneratedFiles ) {
                    if ( log.isDebugEnabled() ) {
                        log.debug("Deleting generated files for servlet " + jspUri);
                    }
                    final String name;
                    if (isTagFile) {
                        name = this.ctxt.getTagInfo().getTagClassName();
                    } else {
                        name = this.ctxt.getServletPackageName() + "." + this.ctxt.getServletClassName();
                    }

                    final String path = ":/" + name.replace('.', '/') + ".class";
                    this.ctxt.getRuntimeContext().getIOProvider().delete(path);
                    this.ctxt.getRuntimeContext().getIOProvider().delete(this.getDependencyFilePath());
                    final org.apache.sling.scripting.jsp.jasper.compiler.Compiler c = this.ctxt.getCompiler();
                    if ( c != null ) {
                        c.removeGeneratedFiles();
                    }
                }
                theServlet.destroy();
                AnnotationProcessor annotationProcessor = (AnnotationProcessor) config.getServletContext().getAttribute(AnnotationProcessor.class.getName());
                if (annotationProcessor != null) {
                    try {
                        annotationProcessor.preDestroy(theServlet);
                    } catch (Exception e) {
                        // Log any exception, since it can't be passed along
                        log.error(Localizer.getMessage("jsp.error.file.not.found",
                               e.getMessage()), e);
                    }
                }
                theServlet = null;
            }
        }
    }

    /**
     * <p>Attempts to construct a JasperException that contains helpful information
     * about what went wrong. Uses the JSP compiler system to translate the line
     * number in the generated servlet that originated the exception to a line
     * number in the JSP.  Then constructs an exception containing that
     * information, and a snippet of the JSP to help debugging.
     * Please see http://issues.apache.org/bugzilla/show_bug.cgi?id=37062 and
     * http://www.tfenne.com/jasper/ for more details.
     *</p>
     *
     * @param ex the exception that was the cause of the problem.
     * @throws a ServletException with more detailed information
     */
    protected void handleJspException(final Exception ex)
    throws ServletException {
        final Exception jspEx = handleJspExceptionInternal(ex);
        if ( jspEx instanceof ServletException ) {
            throw (ServletException)jspEx;
        }
        throw (SlingException)jspEx;
    }

    /**
     * Returns only a ServletException or a SlingException
     */
    private Exception handleJspExceptionInternal(final Exception ex)
    throws ServletException {
    	Throwable realException = ex;
        String exMessage = "";
        if (ex instanceof ServletException) {
            realException = ((ServletException) ex).getRootCause();
            // root cause might be null (eg. for a JasperException ex)
            if (realException == null) {
                realException = ex;
            } else {
                exMessage = ex.toString();
            }
        }

        // avoid nested ScriptEvaluationExceptions (eg. in nested jsp includes)
        while (realException instanceof ScriptEvaluationException) {
            realException = realException.getCause();
        }

        try {
            // First identify the stack frame in the trace that represents the JSP
            StackTraceElement[] frames = realException.getStackTrace();
            StackTraceElement jspFrame = null;

            for (int i=0; i<frames.length; ++i) {
                if ( frames[i].getClassName().equals(this.theServlet.getClass().getName()) ) {
                    jspFrame = frames[i];
                    break;
                }
            }

            if (jspFrame == null) {
                // If we couldn't find a frame in the stack trace corresponding
                // to the generated servlet class, we can't really add anything
                if ( ex instanceof ServletException ) {
                    return ex;
                }
                return new SlingException(ex) {};
            }
            int javaLineNumber = jspFrame.getLineNumber();
            JavacErrorDetail detail = ErrorDispatcher.createJavacError(
                    jspFrame.getMethodName(),
                    this.ctxt.getCompiler().getPageNodes(),
                    null,
                    javaLineNumber,
                    ctxt);

            // If the line number is less than one we couldn't find out
            // where in the JSP things went wrong
            int jspLineNumber = detail.getJspBeginLineNumber();
            if (jspLineNumber < 1) {
                if ( realException instanceof ServletException ) {
                    return (ServletException)realException;
                }
                return new SlingException(exMessage, realException);
            }

            if (options.getDisplaySourceFragment() && detail.getJspExtract() != null ) {
                return new SlingException(Localizer.getMessage
                        ("jsp.exception", detail.getJspFileName(),
                                "" + jspLineNumber) +
                                "\n\n" + detail.getJspExtract() +
                                "\n", realException);

            }
            return new SlingException(Localizer.getMessage
                    ("jsp.exception", detail.getJspFileName(),
                            "" + jspLineNumber), realException);
        } catch (final Exception je) {
            // If anything goes wrong, just revert to the original behaviour
            if (realException instanceof ServletException) {
                return (ServletException)realException;
            }
            return new SlingException(exMessage, realException);
        }
    }

    /**
     * Compare the dependencies.
     */
    private boolean equals(final List<String> oldDeps, final List<String> newDeps) {
        if ( oldDeps == null ) {
            if ( newDeps == null || newDeps.size() == 0 ) {
                return true;
            }
            return false;
        }
        if ( oldDeps.size() != newDeps.size() ) {
            return false;
        }
        final Iterator<String> i1 = oldDeps.iterator();
        final Iterator<String> i2 = newDeps.iterator();
        while ( i1.hasNext() ) {
            if ( !i1.next().equals(i2.next()) ) {
                return false;
            }
        }
        return true;
    }
}

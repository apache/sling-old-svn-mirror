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

package org.apache.sling.scripting.jsp.jasper.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.JspCompilationContext;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.Node.CustomTag;

/**
 * Main JSP compiler class. This class uses Ant for compiling.
 *
 * @author Anil K. Vijendran
 * @author Mandar Raje
 * @author Pierre Delisle
 * @author Kin-man Chung
 * @author Remy Maucherat
 * @author Mark Roth
 */
public abstract class Compiler {

    protected org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory
            .getLog(Compiler.class);

    // ----------------------------------------------------- Instance Variables

    protected JspCompilationContext ctxt;

    protected ErrorDispatcher errDispatcher;

    protected PageInfo pageInfo;

    protected TagFileProcessor tfp;

    protected Options options;

    protected Node.Nodes pageNodes;

    private final boolean defaultIsSession;

    // ------------------------------------------------------------ Constructor

    public Compiler(boolean defaultIsSession) {
        this.defaultIsSession = defaultIsSession;
    }

    public void init(final JspCompilationContext ctxt) {
        this.ctxt = ctxt;
        this.options = ctxt.getOptions();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>
     * Retrieves the parsed nodes of the JSP page, if they are available. May
     * return null. Used in development mode for generating detailed error
     * messages. http://issues.apache.org/bugzilla/show_bug.cgi?id=37062.
     * </p>
     */
    public Node.Nodes getPageNodes() {
        return this.pageNodes;
    }

    /**
     * Compile the jsp file into equivalent servlet in .java file
     *
     * @return a smap for the current JSP page, if one is generated, null
     *         otherwise
     */
    protected String[] generateJava() throws Exception {

        String[] smapStr = null;

        long t1, t2, t3, t4;

        t1 = t2 = t3 = t4 = 0;

        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        // Setup page info area
        pageInfo = new PageInfo(new BeanRepository(ctxt.getClassLoader(),
                errDispatcher), ctxt.getJspFile(), defaultIsSession);

        JspConfig jspConfig = options.getJspConfig();
        JspConfig.JspProperty jspProperty = jspConfig.findJspProperty(ctxt
                .getJspFile());

        /*
         * If the current uri is matched by a pattern specified in a
         * jsp-property-group in web.xml, initialize pageInfo with those
         * properties.
         */
        if (jspProperty.isELIgnored() != null) {
            pageInfo.setELIgnored(JspUtil.booleanValue(jspProperty
                    .isELIgnored()));
        }
        if (jspProperty.isScriptingInvalid() != null) {
            pageInfo.setScriptingInvalid(JspUtil.booleanValue(jspProperty
                    .isScriptingInvalid()));
        }
        if (jspProperty.getIncludePrelude() != null) {
            pageInfo.setIncludePrelude(jspProperty.getIncludePrelude());
        }
        if (jspProperty.getIncludeCoda() != null) {
            pageInfo.setIncludeCoda(jspProperty.getIncludeCoda());
        }
        if (jspProperty.isDeferedSyntaxAllowedAsLiteral() != null) {
            pageInfo.setDeferredSyntaxAllowedAsLiteral(JspUtil.booleanValue(jspProperty
                    .isDeferedSyntaxAllowedAsLiteral()));
        }
        if (jspProperty.isTrimDirectiveWhitespaces() != null) {
            pageInfo.setTrimDirectiveWhitespaces(JspUtil.booleanValue(jspProperty
                    .isTrimDirectiveWhitespaces()));
        }

        ctxt.checkOutputDir();
        String javaFileName = ctxt.getServletJavaFileName();
        ServletWriter writer = null;

        try {
            // Setup the ServletWriter
            String javaEncoding = ctxt.getOptions().getJavaEncoding();
            OutputStreamWriter osw = null;

            try {
                osw = new OutputStreamWriter(
                    ctxt.getOutputStream(javaFileName), javaEncoding);
            } catch (UnsupportedEncodingException ex) {
                errDispatcher.jspError("jsp.error.needAlternateJavaEncoding",
                        javaEncoding);
            } catch (IOException ioe) {
                throw (IOException)new FileNotFoundException(ioe.getMessage()).initCause(ioe);
            }

            writer = new ServletWriter(new PrintWriter(osw));
            ctxt.setWriter(writer);

            // Reset the temporary variable counter for the generator.
            JspUtil.resetTemporaryVariableName();

            // Parse the file
            ParserController parserCtl = new ParserController(ctxt, this);
            pageNodes = parserCtl.parse(ctxt.getJspFile());

            if (ctxt.isPrototypeMode()) {
                // generate prototype .java file for the tag file
                Generator.generate(writer, this, pageNodes);
                writer.close();
                writer = null;
                return null;
            }

            // Validate and process attributes
            Validator.validate(this, pageNodes);

            if (log.isDebugEnabled()) {
                t2 = System.currentTimeMillis();
            }

            // Collect page info
            Collector.collect(this, pageNodes);

            // Compile (if necessary) and load the tag files referenced in
            // this compilation unit.
            tfp = new TagFileProcessor();
            tfp.loadTagFiles(this, pageNodes);

            if (log.isDebugEnabled()) {
                t3 = System.currentTimeMillis();
            }

            // Determine which custom tag needs to declare which scripting vars
            ScriptingVariabler.set(pageNodes, errDispatcher);

            // Optimizations by Tag Plugins
            TagPluginManager tagPluginManager = options.getTagPluginManager();
            tagPluginManager.apply(pageNodes, errDispatcher, pageInfo);

            // Optimization: concatenate contiguous template texts.
            TextOptimizer.concatenate(this, pageNodes);

            // Generate static function mapper codes.
            ELFunctionMapper.map(this, pageNodes);

            // generate servlet .java file
            Generator.generate(writer, this, pageNodes);

            // we have to use a temporary variable in order to not
            // close the writer twice if close() throws an exception
            final ServletWriter w = writer;
            writer = null;
            w.close();

            // The writer is only used during the compile, dereference
            // it in the JspCompilationContext when done to allow it
            // to be GC'd and save memory.
            ctxt.setWriter(null);

            if (log.isDebugEnabled()) {
                t4 = System.currentTimeMillis();
                log.debug("Generated " + javaFileName + " total=" + (t4 - t1)
                        + " generate=" + (t4 - t3) + " validate=" + (t2 - t1));
            }

        } catch (Exception e) {
            if (writer != null) {
                try {
                    writer.close();
                    writer = null;
                } catch (Exception e1) {
                    // do nothing
                }
            }
            // Remove the generated .java file
            ctxt.delete(javaFileName);
            throw e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e2) {
                    // do nothing
                }
            }
        }

        // JSR45 Support
        if (!options.isSmapSuppressed()) {
            smapStr = SmapUtil.generateSmap(ctxt, pageNodes);
        }

        // If any proto type .java and .class files was generated,
        // the prototype .java may have been replaced by the current
        // compilation (if the tag file is self referencing), but the
        // .class file need to be removed, to make sure that javac would
        // generate .class again from the new .java file just generated.
        tfp.removeProtoTypeFiles(ctxt.getClassFileName());

        return smapStr;
    }

    /**
     * Compile the servlet from .java file to .class file
     */
    protected abstract void generateClass(String[] smap)
            throws FileNotFoundException, JasperException, Exception;

    /**
     * Compile the jsp file from the current engine context
     */
    public void compile() throws FileNotFoundException, JasperException,
            Exception {
        compile(true);
    }

    /**
     * Compile the jsp file from the current engine context. As an side- effect,
     * tag files that are referenced by this page are also compiled.
     *
     * @param compileClass
     *            If true, generate both .java and .class file If false,
     *            generate only .java file
     */
    public void compile(boolean compileClass) throws FileNotFoundException,
            JasperException, Exception {
        compile(compileClass, false);
    }

    /**
     * Compile the jsp file from the current engine context. As an side- effect,
     * tag files that are referenced by this page are also compiled.
     *
     * @param compileClass
     *            If true, generate both .java and .class file If false,
     *            generate only .java file
     * @param jspcMode
     *            true if invoked from JspC, false otherwise
     */
    public void compile(boolean compileClass, boolean jspcMode)
            throws FileNotFoundException, JasperException, Exception {
        if (errDispatcher == null) {
            this.errDispatcher = new ErrorDispatcher(jspcMode);
        }

        try {
            String[] smap = generateJava();
            if (compileClass) {
                generateClass(smap);
            }
        } finally {
            if (tfp != null) {
                tfp.removeProtoTypeFiles(null);
            }
            // Make sure these object which are only used during the
            // generation and compilation of the JSP page get
            // dereferenced so that they can be GC'd and reduce the
            // memory footprint.
            tfp = null;
            errDispatcher = null;
            pageInfo = null;

            // Only get rid of the pageNodes if in production.
            // In development mode, they are used for detailed
            // error messages.
            // http://issues.apache.org/bugzilla/show_bug.cgi?id=37062
            //if (!this.options.getDevelopment()) {
            //    pageNodes = null;
            //}

            if (ctxt.getWriter() != null) {
                ctxt.getWriter().close();
                ctxt.setWriter(null);
            }
        }
    }

    /**
     * Gets the error dispatcher.
     */
    public ErrorDispatcher getErrorDispatcher() {
        return errDispatcher;
    }

    /**
     * Gets the info about the page under compilation
     */
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public JspCompilationContext getCompilationContext() {
        return ctxt;
    }

    /**
     * Remove generated files
     */
    public void removeGeneratedFiles() {
        this.removeGeneratedClassFiles();
        try {
            String javaFileName = ctxt.getServletJavaFileName();
            if (javaFileName != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting " + javaFileName);
                }
                ctxt.delete(javaFileName);
            }
        } catch (Exception e) {
            // Remove as much as possible, ignore possible exceptions
        }
    }

    public void removeGeneratedClassFiles() {
        try {
            String classFileName = ctxt.getClassFileName();
            if (classFileName != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting " + classFileName);
                }
                ctxt.delete(classFileName);
            }
        } catch (Exception e) {
            // Remove as much as possible, ignore possible exceptions
        }
    }

    public void clean() {
        if ( this.pageNodes != null ) {
            try {
                pageNodes.visit(new CleanVisitor());
            } catch ( final JasperException ignore) {
                // ignore
            }
        }

    }
    
    protected boolean getDefaultIsSession() {
        return defaultIsSession;
    }

    private static final class CleanVisitor extends Node.Visitor {

        public void visit(final CustomTag n) throws JasperException {
            n.clean();
            visitBody(n);
        }
    }
}

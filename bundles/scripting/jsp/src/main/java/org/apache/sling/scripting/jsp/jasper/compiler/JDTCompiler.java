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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilationUnitWithSource;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.scripting.jsp.jasper.JasperException;

/**
 * JDT class compiler. This compiler will load source dependencies from the
 * context classloader, reducing dramatically disk access during
 * the compilation process.
 */
public class JDTCompiler extends org.apache.sling.scripting.jsp.jasper.compiler.Compiler {

    public JDTCompiler(boolean defaultIsSession) {
        super(defaultIsSession);
    }

    /**
     * Compile the servlet from .java file to .class file
     */
    @Override
    protected void generateClass(String[] smap)
        throws FileNotFoundException, JasperException, Exception {

        long t1 = 0;
        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        final String sourceFile = ctxt.getServletJavaFileName();
        final String packageName = ctxt.getServletPackageName();
        final String targetClassName =
            ((packageName.length() != 0) ? (packageName + ".") : "")
                    + ctxt.getServletClassName();
        final CompilationUnit unit = new CompilationUnitWithSource() {

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getLastModified()
             */
            public long getLastModified() {
                return -1;
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getMainClassName()
             */
            public String getMainClassName() {
                return targetClassName;
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getSource()
             */
            public Reader getSource() throws IOException {
                return new BufferedReader(new InputStreamReader(ctxt.getInputStream(sourceFile),
                                ctxt.getOptions().getJavaEncoding()));
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnitWithSource#getFileName()
             */
            public String getFileName() {
                return sourceFile;
            }
        };

        final Options options = new Options();
        options.put(Options.KEY_GENERATE_DEBUG_INFO, ctxt.getOptions().getClassDebugInfo());

        // Source JVM
        if (ctxt.getOptions().getCompilerSourceVM() != null) {
            options.put(Options.KEY_SOURCE_VERSION, ctxt.getOptions().getCompilerSourceVM());
        } else {
            // Default to 1.6
            options.put(Options.KEY_SOURCE_VERSION, Options.VERSION_1_6);
        }

        // Target JVM
        if (ctxt.getOptions().getCompilerTargetVM() != null) {
            options.put(Options.KEY_TARGET_VERSION, ctxt.getOptions().getCompilerTargetVM());
        } else {
            // Default to 1.6
            options.put(Options.KEY_TARGET_VERSION, Options.VERSION_1_6);
        }

        final ArrayList<JavacErrorDetail> problemList = new ArrayList<JavacErrorDetail>();
        final CompilationResult result = this.ctxt.getRuntimeContext().getIOProvider().getJavaCompiler().compile(new CompilationUnit[] {unit}, options);
        if ( result.getErrors() != null ) {
            for(final CompilerMessage cm : result.getErrors() ) {
                final String name = cm.getFile();
                try {
                    problemList.add(ErrorDispatcher.createJavacError
                            (name, pageNodes, new StringBuffer(cm.getMessage()),
                                    cm.getLine(), ctxt));
                } catch (JasperException e) {
                    log.error("Error visiting node", e);
                }
            }
        }

        if (!ctxt.keepGenerated()) {
            ctxt.delete(ctxt.getServletJavaFileName());
        }

        if (!problemList.isEmpty()) {
            JavacErrorDetail[] jeds =
                problemList.toArray(new JavacErrorDetail[0]);
            errDispatcher.javacError(jeds);
        }

        if( log.isDebugEnabled() ) {
            long t2=System.currentTimeMillis();
            log.debug("Compiled " + ctxt.getServletJavaFileName() + " "
                      + (t2-t1) + "ms");
        }

        if (ctxt.isPrototypeMode()) {
            return;
        }

        // JSR45 Support
        if (! this.options.isSmapSuppressed()) {
            SmapUtil.installSmap(getCompilationContext(), smap);
        }

    }


}

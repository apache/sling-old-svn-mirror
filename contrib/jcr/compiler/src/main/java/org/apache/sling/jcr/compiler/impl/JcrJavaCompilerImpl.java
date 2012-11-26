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
package org.apache.sling.jcr.compiler.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.compiler.JcrJavaCompiler;

/**
 * <code>JcrJavaCompilerImpl</code> ...
 *
 */
@Component
@Service(value=JcrJavaCompiler.class)
public class JcrJavaCompilerImpl implements JcrJavaCompiler {

    @Reference
    protected JavaCompiler compiler;

    @Reference
    protected SlingRepository repository;

    /**
     * @see org.apache.sling.jcr.compiler.JcrJavaCompiler#compile(java.lang.String[], java.lang.String, org.apache.sling.commons.compiler.Options)
     */
    public CompilationResult compile(final String[] srcFiles,
                                     final String outputDir,
                                     final Options compilerOptions)
    throws Exception {
        return compile(srcFiles, compilerOptions);
    }

    /**
     * @see org.apache.sling.jcr.compiler.JcrJavaCompiler#compile(java.lang.String[], org.apache.sling.commons.compiler.Options)
     */
    public CompilationResult compile(final String[] srcFiles, final Options compilerOptions)
    throws Exception {
        // make sure we have options
        final Options options = (compilerOptions == null ? new Options() : new Options(compilerOptions));
        // open session
        Session session = null;
        try {
            session = this.repository.loginAdministrative(null);

            // create compilation units
            CompilationUnit[] units = new CompilationUnit[srcFiles.length];
            for (int i = 0; i < units.length; i++) {
                units[i] = createCompileUnit(srcFiles[i], session);
            }

            // and compile
            return mapResult(compiler.compile(units, options), srcFiles, units);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }
    }

    /**
     * Map the source information of the errors
     */
    private CompilationResult mapResult(final CompilationResult result,
            final String[] srcFiles,
            final CompilationUnit[] units) {
        if ( result == null || (result.getErrors() == null && result.getWarnings() == null ) ) {
            return result;
        }

        final Map<String, String> mapping = new HashMap<String, String>();
        for(int i=0;i<srcFiles.length;i++) {
            mapping.put(units[i].getMainClassName() + ".java", srcFiles[i]);
        }
        return new CompilationResult() {

            private List<CompilerMessage> mapMessages(final List<CompilerMessage> msgs) {
                if ( msgs == null || msgs.size() == 0 ) {
                    return msgs;
                }
                final List<CompilerMessage> newMsgs = new ArrayList<CompilerMessage>();
                for(final CompilerMessage msg : msgs) {
                    final String mapped = mapping.get(msg.getFile());

                    newMsgs.add(new CompilerMessage(
                            mapped == null ? msg.getFile() : mapped,
                            msg.getLine(), msg.getColumn(), msg.getMessage()));
                }
                return newMsgs;
            }

            public Class<?> loadCompiledClass(String className)
                    throws ClassNotFoundException {
                return result.loadCompiledClass(className);
            }

            public List<CompilerMessage> getWarnings() {
                return mapMessages(result.getWarnings());
            }

            public List<CompilerMessage> getErrors() {
                return mapMessages(result.getErrors());
            }

            public boolean didCompile() {
                return result.didCompile();
            }
        };
    }

    //--------------------------------------------------------< misc. helpers >

    private CompilationUnit createCompileUnit(final String sourceFile, final Session session)
    throws RepositoryException, IOException {
        final Source source = readTextResource(sourceFile, session);
        final String packageName = extractPackageName(source.contents);

        return new CompilationUnit() {

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getMainClassName()
             */
            public String getMainClassName() {
                return packageName + '.' + this.getMainTypeName();
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getSource()
             */
            public Reader getSource() throws IOException {
                return new StringReader(source.contents);
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getLastModified()
             */
            public long getLastModified() {
                return source.lastModified;
            }

            private String getMainTypeName() {
                String className;
                int pos = sourceFile.lastIndexOf(".java");
                if (pos != -1) {
                    className = sourceFile.substring(0, pos).trim();
                } else {
                    className = sourceFile.trim();
                }
                pos = className.lastIndexOf('/');
                return (pos == -1) ? className : className.substring(pos + 1);
            }
        };
    }

    private String extractPackageName(final String contents) {
        BufferedReader reader = new BufferedReader(new StringReader(contents));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("package")) {
                    line = line.substring("package".length());
                    line = line.substring(0, line.lastIndexOf(';'));
                    return line.trim();
                }
            }
        } catch (IOException e) {
            // should never get here...
        }

        // no package declaration found
        return "";
    }

    private Source readTextResource(final String resourcePath, final Session session)
    throws RepositoryException, IOException {
        final Source source = new Source();
        final String relPropPath = resourcePath.substring(1) + "/jcr:content/jcr:data";
        final InputStream in = session.getRootNode().getProperty(relPropPath).getStream();
        final Reader reader = new InputStreamReader(in, "UTF-8");
        final StringWriter writer = new StringWriter();
        try {
            final char[] buffer = new char[2048];
            int read = 0;
            while ((read = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
            source.contents = writer.toString();
            final String lastModPath = resourcePath + "/jcr:content/jcr:lastModified";
            source.lastModified = session.itemExists(lastModPath) ? ((Property)session.getItem(lastModPath)).getLong() : -1;
            return source;
        } finally {
            try { reader.close(); } catch (IOException ignore) {}
        }
    }

    private static final class Source {
        public String contents;
        public long lastModified;
    }
}

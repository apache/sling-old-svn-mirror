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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.compiler.ClassWriter;
import org.apache.sling.commons.compiler.CompileUnit;
import org.apache.sling.commons.compiler.CompilerEnvironment;
import org.apache.sling.commons.compiler.ErrorHandler;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.apache.sling.jcr.compiler.JcrJavaCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>JcrJavaCompilerImpl</code> ...
 * 
 * @scr.component metatype="no"
 * @scr.service interface="org.apache.sling.jcr.compiler.JcrJavaCompiler"
 */
public class JcrJavaCompilerImpl implements JcrJavaCompiler, CompilerEnvironment {

    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(JcrJavaCompilerImpl.class);

    private static final String CLASSLOADER_NAME = "admin";

    /** @scr.reference */
    protected JavaCompiler compiler;
    
    /** @scr.reference */
    protected RepositoryClassLoaderProvider clp;

    /** @scr.reference */
    protected SlingRepository rep;
    
    // only accessed locally within scope of compile() method
    private ClassLoader cl;

    // only accessed locally within scope of compile() method
    private Session session;

    public JcrJavaCompilerImpl() {
    }

    //------------------------------------------------------< JcrJavaCompiler >
    /* (non-Javadoc)
     * @see org.apache.sling.jcr.compiler.JcrJavaCompiler#compile(java.lang.String[], java.lang.String, org.apache.sling.commons.compiler.ErrorHandler, boolean, java.lang.String)
     */
    public boolean compile(String[] srcFiles, String outputDir,
            ErrorHandler errorHandler, boolean generateDebug, String javaVersion)
            throws Exception {
        
        if (javaVersion == null) {
            javaVersion = System.getProperty("java.specification.version");
        }

        if (outputDir == null) {
            throw new Exception("outputDir can not be null");
        }
        
        ErrorHandlerWrapper handler = new ErrorHandlerWrapper(errorHandler);

        cl = clp.getClassLoader(CLASSLOADER_NAME);
        try {
            session = rep.loginAdministrative(null);
            
            ClassWriter classWriter;
            if (outputDir.startsWith("file://")) {
                // write class files to local file system;
                // only subdirectories of the system temp dir 
                // will be accepted
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                File outDir = new File(outputDir.substring("file://".length()));
                if (!outDir.isAbsolute()) {
                    outDir = new File(tempDir, outputDir.substring("file://".length()));
                } 
                if (!outDir.getCanonicalPath().startsWith(tempDir.getCanonicalPath())) {
                    throw new Exception("illegal outputDir (not a temp dir): " + outputDir);
                }
                outDir.mkdir();
                classWriter = new FileClassWriter(outDir);
            } else {
                // write class files to the repository  (default)
                if (!session.itemExists(outputDir)) {
                    throw new Exception("outputDir does not exist: " + outputDir);
                }
                
                Item item = session.getItem(outputDir);
                if (item.isNode()) {
                    Node folder = (Node) item;
                    if (!folder.isNodeType("nt:folder")) {
                        throw new Exception("outputDir must be a node of type nt:folder");
                    }
                    classWriter = new JcrClassWriter(folder);
                } else {
                    throw new Exception("outputDir must be a node of type nt:folder");
                }
            }

            CompileUnit[] units = new CompileUnit[srcFiles.length];
            for (int i = 0; i < units.length; i++) {
                units[i] = createCompileUnit(srcFiles[i]);
            }
            compiler.compile(units, this, classWriter, handler, 
                    new Options(javaVersion, generateDebug));
        } finally {
            // cleanup
            clp.ungetClassLoader(cl);
            if (session != null) {
                session.logout();
                session = null;
            }
        }
        
        return handler.getNumErrors() == 0;
    }

    //--------------------------------------------------< CompilerEnvironment >
    /* (non-Javadoc)
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#findClass(java.lang.String)
     */
    public byte[] findClass(String className) throws Exception {
        InputStream in = cl.getResourceAsStream(className.replace('.', '/') + ".class");
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(0x7fff);

        try {
            byte[] buffer = new byte[0x1000];
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            //out.close();
            in.close();
        }

        return out.toByteArray();
    }

    /* (non-Javadoc)
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#isPackage(java.lang.String)
     */
    public boolean isPackage(String packageName) {
        InputStream in = cl.getResourceAsStream(packageName.replace('.', '/') + ".class");
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignore) {
            }
            return false;
        }
        // FIXME need a better way to determine whether a name resolves 
        // to a package
        int pos = packageName.lastIndexOf('.');
        if (pos != -1) {
            if (Character.isUpperCase(packageName.substring(pos + 1).charAt(0))) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#cleanup()
     */
    public void cleanup() {
    }

    //--------------------------------------------------------< misc. helpers >

    private CompileUnit createCompileUnit(final String sourceFile) throws Exception {
        final char[] chars = readTextResource(sourceFile);

        return new CompileUnit() {

            public String getSourceFileName() {
                return sourceFile;
            }

            public char[] getSourceFileContents() {
                return chars;
            }

            public String getMainTypeName() {
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

    private char[] readTextResource(String resourcePath) 
            throws RepositoryException, IOException {
        String relPropPath = resourcePath.substring(1) 
                + "/jcr:content/jcr:data";
        InputStream in = session.getRootNode().getProperty(relPropPath).getStream();
        Reader reader = new InputStreamReader(in);
        CharArrayWriter writer = new CharArrayWriter(0x7fff);
        try {
            char[] buffer = new char[0x1000];
            int read = 0;
            while ((read = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            return writer.toCharArray();
        } finally {
            //writer.close();
            reader.close();
        }
    }

    //--------------------------------------------------------< inner classes >

    class ErrorHandlerWrapper implements ErrorHandler {

        ErrorHandler handler;
        int errors;
        int warnings;

        ErrorHandlerWrapper(ErrorHandler handler) {
            this.handler = handler;
            errors = 0;
            warnings = 0;
        }

        int getNumErrors() {
            return errors;
        }

        int getNumWarnings() {
            return warnings;
        }

        public void onError(String msg, String sourceFile, int line, int position) {
            log.debug("Error in " + sourceFile + ", line " + line + ", pos. " + position + ": " + msg);
            errors++;
            handler.onError(msg, sourceFile, line, position);
        }

        public void onWarning(String msg, String sourceFile, int line, int position) {
            log.debug("Warning in " + sourceFile + ", line " + line + ", pos. " + position + ": " + msg);
            warnings++;
            handler.onWarning(msg, sourceFile, line, position);
        }
    }
}

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
package org.apache.sling.commons.compiler.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.ErrorHandler;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>EclipseJavaCompiler</code> provides platform independant
 * Java compilation support using the Eclipse Java Compiler (org.eclipse.jdt).
 *
 */
@Component
@Service(value=JavaCompiler.class)
public class EclipseJavaCompiler implements JavaCompiler {

    /** Logger instance */
    private final Logger logger = LoggerFactory.getLogger(EclipseJavaCompiler.class);

    @Reference
    private ClassLoaderWriter classLoaderWriter;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private ClassLoader classLoader;

    /** the static problem factory */
    private IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());

    /** the static policy. */
    private final IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

    /**
     * Bind the class load provider.
     * @param repositoryClassLoaderProvider the new provider
     */
    protected void bindDynamicClassLoaderManager(final DynamicClassLoaderManager rclp) {
        if ( this.classLoader != null ) {
            this.ungetClassLoader();
        }
        this.getClassLoader(rclp);
    }

    /**
     * Unbind the class loader provider.
     * @param repositoryClassLoaderProvider the old provider
     */
    protected void unbindDynamicClassLoaderManager(final DynamicClassLoaderManager rclp) {
        if ( this.dynamicClassLoaderManager == rclp ) {
            this.ungetClassLoader();
        }
    }

    /**
     * Get the class loader
     */
    private void getClassLoader(final DynamicClassLoaderManager rclp) {
        this.dynamicClassLoaderManager = rclp;
        this.classLoader = rclp.getDynamicClassLoader();
    }

    /**
     * Unget the class loader
     */
    private void ungetClassLoader() {
        this.classLoader = null;
        this.dynamicClassLoaderManager = null;
    }

    /**
     * @see org.apache.sling.commons.compiler.JavaCompiler#compile(org.apache.sling.commons.compiler.CompilationUnit[], org.apache.sling.commons.compiler.ErrorHandler, org.apache.sling.commons.compiler.Options)
     */
    public boolean compile(final CompilationUnit[] units,
                           final ErrorHandler errorHandler,
                           final Options compileOptions) {
        // make sure we have an options object (to avoid null checks all over the place)
        final Options options = (compileOptions != null ? compileOptions : new Options());

        // create properties for the settings object
        final Map<String, String> props = new HashMap<String, String>();
        if (options.isGenerateDebugInfo()) {
            props.put("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
            props.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
            props.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
        }
        if (options.getSourceVersion() != null) {
            props.put("org.eclipse.jdt.core.compiler.source", options.getSourceVersion());
            //props.put("org.eclipse.jdt.core.compiler.compliance", options.getSourceVersion());
            //props.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", options.getSourceVersion());
        }
        if (options.getTargetVersion() != null) {
            props.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", options.getTargetVersion());
        }
        props.put("org.eclipse.jdt.core.encoding", "UTF8");

        // create the settings
        final CompilerOptions settings = new CompilerOptions(props);
        logger.debug("Compiling with settings {}.", settings);

        // classloader
        final ClassLoader loader;
        if ( options.get(Options.KEY_CLASS_LOADER) != null ) {
            loader = (ClassLoader)options.get(Options.KEY_CLASS_LOADER);
        } else if ( options.get(Options.KEY_ADDITIONAL_CLASS_LOADER) != null ) {
            final ClassLoader additionalClassLoader = (ClassLoader)options.get(Options.KEY_ADDITIONAL_CLASS_LOADER);
            loader = new ClassLoader(this.classLoader) {
                protected Class<?> findClass(String name)
                throws ClassNotFoundException {
                    return additionalClassLoader.loadClass(name);
                }

                protected URL findResource(String name) {
                    return additionalClassLoader.getResource(name);
                }
            };
        } else {
            loader = this.classLoader;
        }

        // classloader writer
        final ClassLoaderWriter writer = (options.get(Options.KEY_CLASS_LOADER_WRITER) != null ?
                (ClassLoaderWriter)options.get(Options.KEY_CLASS_LOADER_WRITER) : this.classLoaderWriter);

        // create the context
        final CompileContext context = new CompileContext(units, errorHandler, writer, loader);

        // create the compiler
        final org.eclipse.jdt.internal.compiler.Compiler compiler =
                new org.eclipse.jdt.internal.compiler.Compiler(
                        context,
                        this.policy,
                        settings,
                        context,
                        this.problemFactory,
                        null);

        // compile
        compiler.compile(context.getSourceUnits());

        return !context.hasErrors;
    }

    //--------------------------------------------------------< inner classes >

    private class CompileContext implements ICompilerRequestor, INameEnvironment, ErrorHandler {

        private final Map<String,ICompilationUnit> compUnits;

        private final ErrorHandler errorHandler;
        private final ClassLoaderWriter classLoaderWriter;
        private final ClassLoader classLoader;

        /** Flag indicating if we have an error. */
        private boolean hasErrors = false;

        public CompileContext(final CompilationUnit[] units,
         		              final ErrorHandler errorHandler,
        		              final ClassLoaderWriter classWriter,
        		              final ClassLoader classLoader) {
        	this.compUnits = new HashMap<String,ICompilationUnit>();
            for (int i = 0; i < units.length; i++) {
                CompilationUnitAdapter cua = new CompilationUnitAdapter(units[i], this);
                char[][] compoundName = CharOperation.arrayConcat(cua.getPackageName(), cua.getMainTypeName());
                this.compUnits.put(CharOperation.toString(compoundName), new CompilationUnitAdapter(units[i], this));
            }

        	this.errorHandler = errorHandler;
            this.classLoaderWriter = classWriter;
            this.classLoader = classLoader;
        }

        /**
         * @see org.apache.sling.commons.compiler.ErrorHandler#onError(java.lang.String, java.lang.String, int, int)
         */
        public void onError(String msg, String sourceFile, int line, int position) {
            this.errorHandler.onError(msg, sourceFile, line, position);
            this.hasErrors = true;
        }

        /**
         * @see org.apache.sling.commons.compiler.ErrorHandler#onWarning(java.lang.String, java.lang.String, int, int)
         */
        public void onWarning(String msg, String sourceFile, int line, int position) {
            this.errorHandler.onWarning(msg, sourceFile, line, position);
        }

        public ICompilationUnit[] getSourceUnits() {
        	return compUnits.values().toArray(
        			new ICompilationUnit[compUnits.size()]);
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.ICompilerRequestor#acceptResult(org.eclipse.jdt.internal.compiler.CompilationResult)
         */
        public void acceptResult(CompilationResult result) {
            if (result.hasProblems()) {
                CategorizedProblem[] problems = result.getProblems();
                for (int i = 0; i < problems.length; i++) {
                    CategorizedProblem problem = problems[i];
                    String msg = problem.getMessage();
                    String fileName = CharOperation.charToString(problem.getOriginatingFileName());
                    int line = problem.getSourceLineNumber();
                    int pos = problem.getSourceStart();

                    if (problem.isError()) {
                        this.onError(msg, fileName, line, pos);
                    } else if (problem.isWarning()) {
                        this.onWarning(msg, fileName, line, pos);
                    } else {
                        logger.debug("unknown problem category: {}", problem);
                    }
                }
            }
            ClassFile[] classFiles = result.getClassFiles();
            for (int i = 0; i < classFiles.length; i++) {
                ClassFile classFile = classFiles[i];
                String className = CharOperation.toString(classFile.getCompoundName());
                try {
                    this.write(className, classFile.getBytes());
                } catch (IOException e) {
                    this.onError("Unable to write class file: " + e.getMessage(), className, 0, 0);
                }
            }
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#findType(char[][])
         */
        public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            // check 1st if type corresponds with any of current compilation units
            String fqn = CharOperation.toString(compoundTypeName);
            ICompilationUnit cu = compUnits.get(fqn);
            if (cu != null) {
                return new NameEnvironmentAnswer(cu, null);
            }

            // locate the class through the class loader
            try {
                byte[] bytes = this.findClass(CharOperation.toString(compoundTypeName));
                if (bytes == null) {
                    return null;
                }
                ClassFileReader classFileReader =
                        new ClassFileReader(bytes, fqn.toCharArray(), true);
                return new NameEnvironmentAnswer(classFileReader, null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#findType(char[], char[][])
         */
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            return findType(CharOperation.arrayConcat(packageName, typeName));
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#isPackage(char[][], char[])
         */
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            String fqn = CharOperation.toString(
                    CharOperation.arrayConcat(parentPackageName, packageName));
            return compUnits.get(fqn) == null && this.isPackage(fqn);
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#cleanup()
         */
        public void cleanup() {
            // nothing to do
        }

        /**
         * Write the classfile
         */
        private void write(String name, byte[] data) throws IOException {
            final OutputStream os = this.classLoaderWriter.getOutputStream('/' + name.replace('.', '/') + ".class");
            os.write(data);
            os.close();
        }

        private boolean isPackage(String result) {
            String resourceName = result.replace('.', '/') + ".class";
            if ( resourceName.startsWith("/") ) {
                resourceName = resourceName.substring(1);
            }
            final InputStream is = this.classLoader.getResourceAsStream(resourceName);
            if ( is != null ) {
                try {
                    is.close();
                } catch (IOException ignore) {}
            }
            return is == null;
        }

        private byte[] findClass(String name) throws Exception {
            final String resourceName = name.replace('.', '/') + ".class";
            final InputStream is = this.classLoader.getResourceAsStream(resourceName);
            if (is != null) {
                try {
                    byte[] buf = new byte[8192];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
                    int count;
                    while ((count = is.read(buf, 0, buf.length)) > 0) {
                        baos.write(buf, 0, count);
                    }
                    baos.flush();
                    return baos.toByteArray();
                } finally {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }
            }
            return null;
        }
    }

    private class CompilationUnitAdapter implements ICompilationUnit {

        private final ErrorHandler errorHandler;
        private final CompilationUnit compUnit;
        private final String mainTypeName;
        private final String packageName;

        public CompilationUnitAdapter(final CompilationUnit compUnit, final ErrorHandler errorHandler) {
            this.compUnit = compUnit;
            this.errorHandler = errorHandler;
            final int pos = compUnit.getMainClassName().lastIndexOf('.');
            if ( pos == -1 ) {
                this.packageName = "";
                this.mainTypeName = compUnit.getMainClassName();
            } else {
                this.packageName = compUnit.getMainClassName().substring(0, pos);
                this.mainTypeName = compUnit.getMainClassName().substring(pos + 1);
            }
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getContents()
         */
        public char[] getContents() {
            Reader fr = null;
            try {
                fr = this.compUnit.getSource();
                final Reader reader = new BufferedReader(fr);
                try {
                    char[] chars = new char[8192];
                    StringBuilder buf = new StringBuilder();
                    int count;
                    while ((count = reader.read(chars, 0, chars.length)) > 0) {
                        buf.append(chars, 0, count);
                    }
                    final char[] result = new char[buf.length()];
                    buf.getChars(0, result.length, result, 0);
                    return result;
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                this.errorHandler.onError("Unable to read source file " + this.compUnit.getMainClassName() + " : " + e.getMessage(),
                        this.compUnit.getMainClassName(), 0, 0);
                return null;
            } finally {
                if ( fr != null ) {
                    try { fr.close(); } catch (IOException ignore) {}
                }
            }
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getMainTypeName()
         */
        public char[] getMainTypeName() {
            return this.mainTypeName.toCharArray();
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getPackageName()
         */
        public char[][] getPackageName() {
            return CharOperation.splitOn('.', this.packageName.toCharArray());
        }

        /**
         * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
         */
        public char[] getFileName() {
            return (this.mainTypeName + ".java").toCharArray();
        }
    }
}

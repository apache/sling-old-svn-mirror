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
package org.apache.sling.scripting.java.jdt;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.sling.scripting.java.CompilerError;
import org.apache.sling.scripting.java.Options;
import org.apache.sling.scripting.java.SlingIOProvider;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;


public class CompilationUnit
    implements ICompilationUnit, INameEnvironment, ICompilerRequestor {

    private final Options options;
    private final SlingIOProvider ioProvider;
    private final String className;
    private final String sourceFile;

    /** The list of compile errors. */
    private final List<CompilerError> errors = new LinkedList<CompilerError>();

    public CompilationUnit(String sourceFile,
                           String className,
                           Options options,
                           SlingIOProvider ioProvider) {
        this.className = className;
        this.sourceFile = sourceFile;
        this.ioProvider = ioProvider;
        this.options = options;
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
     */
    public char[] getFileName() {
        int slash = sourceFile.lastIndexOf('/');
        if (slash > 0) {
            return sourceFile.substring(slash + 1).toCharArray();
        }
        return sourceFile.toCharArray();
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getContents()
     */
    public char[] getContents() {
        char[] result = null;
        InputStream fr = null;
        try {
            fr = ioProvider.getInputStream(this.sourceFile);
            final Reader reader = new BufferedReader(new InputStreamReader(fr, this.options.getJavaEncoding()));
            try {
                char[] chars = new char[8192];
                StringBuffer buf = new StringBuffer();
                int count;
                while ((count = reader.read(chars, 0, chars.length)) > 0) {
                    buf.append(chars, 0, count);
                }
                result = new char[buf.length()];
                buf.getChars(0, result.length, result, 0);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            handleError(-1, -1, e.getMessage());
        }
        return result;
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getMainTypeName()
     */
    public char[] getMainTypeName() {
        int dot = className.lastIndexOf('.');
        if (dot > 0) {
            return className.substring(dot + 1).toCharArray();
        }
        return className.toCharArray();
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getPackageName()
     */
    public char[][] getPackageName() {
        StringTokenizer izer = new StringTokenizer(className.replace('/', '.'), ".");
        char[][] result = new char[izer.countTokens()-1][];
        for (int i = 0; i < result.length; i++) {
            String tok = izer.nextToken();
            result[i] = tok.toCharArray();
        }
        return result;
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#findType(char[][])
     */
    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < compoundTypeName.length; i++) {
            if (i > 0) {
                result.append(".");
            }
            result.append(compoundTypeName[i]);
        }
        return findType(result.toString());
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#findType(char[], char[][])
     */
    public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < packageName.length; i++) {
            if (i > 0) {
                result.append(".");
            }
            result.append(packageName[i]);
        }
        result.append(".");
        result.append(typeName);
        return findType(result.toString());
    }

    /**
     * @param className
     * @return
     */
    private NameEnvironmentAnswer findType(String className) {
        try {
            if (className.equals(this.className)) {
                ICompilationUnit compilationUnit = this;
                return new NameEnvironmentAnswer(compilationUnit, null);
            }
            String resourceName = className.replace('.', '/') + ".class";
            InputStream is = options.getClassLoader().getResourceAsStream(resourceName);
            if (is != null) {
                byte[] classBytes;
                byte[] buf = new byte[8192];
                ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(buf.length);
                int count;
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    baos.write(buf, 0, count);
                }
                baos.flush();
                classBytes = baos.toByteArray();
                char[] fileName = className.toCharArray();
                ClassFileReader classFileReader =
                    new ClassFileReader(classBytes, fileName,
                                        true);
                return
                    new NameEnvironmentAnswer(classFileReader, null);
            }
        } catch (IOException exc) {
            handleError(-1, -1, exc.getMessage());
        } catch (org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException exc) {
            handleError(-1, -1, exc.getMessage());
        }
        return null;
    }

    private boolean isPackage(String result) {
        if (result.equals(this.className)) {
            return false;
        }
        String resourceName = result.replace('.', '/') + ".class";
        if ( resourceName.startsWith("/") ) {
            resourceName = resourceName.substring(1);
        }
        final InputStream is = options.getClassLoader().getResourceAsStream(resourceName);
        if ( is != null ) {
            try {
                is.close();
            } catch (IOException ignore) {}
        }
        return is == null;
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#isPackage(char[][], char[])
     */
    public boolean isPackage(char[][] parentPackageName, char[] packageName) {
        StringBuffer result = new StringBuffer();
        if (parentPackageName != null) {
            for (int i = 0; i < parentPackageName.length; i++) {
                if (i > 0) {
                    result.append(".");
                }
                result.append(parentPackageName[i]);
            }
        }
        String str = new String(packageName);
        if (Character.isUpperCase(str.charAt(0)) && !isPackage(result.toString())) {
                return false;
        }
        result.append(".");
        result.append(str);
        return isPackage(result.toString());
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.env.INameEnvironment#cleanup()
     */
    public void cleanup() {
        // EMPTY
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.ICompilerRequestor#acceptResult(org.eclipse.jdt.internal.compiler.CompilationResult)
     */
    public void acceptResult(CompilationResult result) {
        try {
            if (result.hasErrors()) {
                IProblem[] errors = result.getErrors();
                for (int i = 0; i < errors.length; i++) {
                    IProblem error = errors[i];
                    handleError(error.getSourceLineNumber(), -1, error.getMessage());
                }
            } else {
                ClassFile[] classFiles = result.getClassFiles();
                for (int i = 0; i < classFiles.length; i++) {
                    ClassFile classFile = classFiles[i];
                    char[][] compoundName = classFile.getCompoundName();
                    StringBuffer className = new StringBuffer();
                    for (int j = 0;  j < compoundName.length; j++) {
                        if (j > 0) {
                            className.append(".");
                        }
                        className.append(compoundName[j]);
                    }
                    byte[] bytes = classFile.getBytes();
                    final StringBuffer b = new StringBuffer(this.options.getDestinationPath());
                    b.append('/');
                    b.append(className.toString().replace('.', '/'));
                    b.append(".class");
                    OutputStream fout = ioProvider.getOutputStream(b.toString());
                    BufferedOutputStream bos = new BufferedOutputStream(fout);
                    bos.write(bytes);
                    bos.close();
                }
            }
        } catch (IOException exc) {
            handleError(-1, -1, exc.getLocalizedMessage());
        }
    }

    private void handleError(int line, int column, Object errorMessage) {
        if (column < 0) {
            column = 0;
        }
        errors.add(new CompilerError(this.sourceFile,
                                     true,
                                     line,
                                     column,
                                     line,
                                     column,
                                     errorMessage.toString()));
    }

    public List<CompilerError> getErrors() throws IOException {
        return errors;
    }
}

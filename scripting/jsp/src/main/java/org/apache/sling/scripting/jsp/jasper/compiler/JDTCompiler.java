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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
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

/**
 * JDT class compiler. This compiler will load source dependencies from the
 * context classloader, reducing dramatically disk access during
 * the compilation process.
 *
 * @author Cocoon2
 * @author Remy Maucherat
 */
public class JDTCompiler extends org.apache.sling.scripting.jsp.jasper.compiler.Compiler {


    /**
     * Compile the servlet from .java file to .class file
     */
    protected void generateClass(String[] smap)
        throws FileNotFoundException, JasperException, Exception {

        long t1 = 0;
        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        final String sourceFile = ctxt.getServletJavaFileName();
        final String outputDir = ctxt.getOptions().getScratchDir();
        String packageName = ctxt.getServletPackageName();
        final String targetClassName =
            ((packageName.length() != 0) ? (packageName + ".") : "")
                    + ctxt.getServletClassName();
        final ClassLoader classLoader = ctxt.getJspLoader();
        String[] fileNames = new String[] {sourceFile};
        String[] classNames = new String[] {targetClassName};
        final ArrayList problemList = new ArrayList();

        class CompilationUnit implements ICompilationUnit {

            String className;
            String sourceFile;

            CompilationUnit(String sourceFile, String className) {
                this.className = className;
                this.sourceFile = sourceFile;
            }

            public char[] getFileName() {
                return sourceFile.toCharArray();
            }

            public char[] getContents() {
                char[] result = null;
                InputStream is = null;
                try {
                    is = ctxt.getInputStream(sourceFile);
                    Reader reader =
                        new BufferedReader(new InputStreamReader(is, ctxt.getOptions().getJavaEncoding()));
                    if (reader != null) {
                        char[] chars = new char[8192];
                        StringBuffer buf = new StringBuffer();
                        int count;
                        while ((count = reader.read(chars, 0,
                                                    chars.length)) > 0) {
                            buf.append(chars, 0, count);
                        }
                        result = new char[buf.length()];
                        buf.getChars(0, result.length, result, 0);
                    }
                } catch (IOException e) {
                    log.error("Compilation error", e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException exc) {
                            // Ignore
                        }
                    }
                }
                return result;
            }

            public char[] getMainTypeName() {
                int dot = className.lastIndexOf('.');
                if (dot > 0) {
                    return className.substring(dot + 1).toCharArray();
                }
                return className.toCharArray();
            }

            public char[][] getPackageName() {
                StringTokenizer izer =
                    new StringTokenizer(className, ".");
                char[][] result = new char[izer.countTokens()-1][];
                for (int i = 0; i < result.length; i++) {
                    String tok = izer.nextToken();
                    result[i] = tok.toCharArray();
                }
                return result;
            }
        }

        final INameEnvironment env = new INameEnvironment() {

                public NameEnvironmentAnswer
                    findType(char[][] compoundTypeName) {
                    String result = "";
                    String sep = "";
                    for (int i = 0; i < compoundTypeName.length; i++) {
                        result += sep;
                        result += new String(compoundTypeName[i]);
                        sep = ".";
                    }
                    return findType(result);
                }

                public NameEnvironmentAnswer
                    findType(char[] typeName,
                             char[][] packageName) {
                        String result = "";
                        String sep = "";
                        for (int i = 0; i < packageName.length; i++) {
                            result += sep;
                            result += new String(packageName[i]);
                            sep = ".";
                        }
                        result += sep;
                        result += new String(typeName);
                        return findType(result);
                }

                private NameEnvironmentAnswer findType(String className) {

                    InputStream is = null;
                    try {
                        if (className.equals(targetClassName)) {
                            ICompilationUnit compilationUnit =
                                new CompilationUnit(sourceFile, className);
                            return
                                new NameEnvironmentAnswer(compilationUnit, null);
                        }
                        String resourceName =
                            className.replace('.', '/') + ".class";
                        is = classLoader.getResourceAsStream(resourceName);
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
                        log.error("Compilation error", exc);
                    } catch (org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException exc) {
                        log.error("Compilation error", exc);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException exc) {
                                // Ignore
                            }
                        }
                    }
                    return null;
                }

                private boolean isPackage(String result) {
                    if (result.equals(targetClassName)) {
                        return false;
                    }
                    String resourceName = result.replace('.', '/') + ".class";
                    InputStream is =
                        classLoader.getResourceAsStream(resourceName);
                    return is == null;
                }

                public boolean isPackage(char[][] parentPackageName,
                                         char[] packageName) {
                    String result = "";
                    String sep = "";
                    if (parentPackageName != null) {
                        for (int i = 0; i < parentPackageName.length; i++) {
                            result += sep;
                            String str = new String(parentPackageName[i]);
                            result += str;
                            sep = ".";
                        }
                    }
                    String str = new String(packageName);
                    if (Character.isUpperCase(str.charAt(0))) {
                        if (!isPackage(result)) {
                            return false;
                        }
                    }
                    result += sep;
                    result += str;
                    return isPackage(result);
                }

                public void cleanup() {
                }

            };

        final IErrorHandlingPolicy policy =
            DefaultErrorHandlingPolicies.proceedWithAllProblems();

        final Map settings = new HashMap();
        settings.put(CompilerOptions.OPTION_LineNumberAttribute,
                     CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute,
                     CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_ReportDeprecation,
                     CompilerOptions.IGNORE);
        if (ctxt.getOptions().getJavaEncoding() != null) {
            settings.put(CompilerOptions.OPTION_Encoding,
                    ctxt.getOptions().getJavaEncoding());
        }
        if (ctxt.getOptions().getClassDebugInfo()) {
            settings.put(CompilerOptions.OPTION_LocalVariableAttribute,
                         CompilerOptions.GENERATE);
        }

        // Source JVM
        if(ctxt.getOptions().getCompilerSourceVM() != null) {
            String opt = ctxt.getOptions().getCompilerSourceVM();
            if(opt.equals("1.1")) {
                settings.put(CompilerOptions.OPTION_Source,
                             CompilerOptions.VERSION_1_1);
            } else if(opt.equals("1.2")) {
                settings.put(CompilerOptions.OPTION_Source,
                             CompilerOptions.VERSION_1_2);
            } else if(opt.equals("1.3")) {
                settings.put(CompilerOptions.OPTION_Source,
                             CompilerOptions.VERSION_1_3);
            } else if(opt.equals("1.4")) {
                settings.put(CompilerOptions.OPTION_Source,
                             CompilerOptions.VERSION_1_4);
            } else if(opt.equals("1.5")) {
                settings.put(CompilerOptions.OPTION_Source,
                             CompilerOptions.VERSION_1_5);
            } else {
                log.warn("Unknown source VM " + opt + " ignored.");
                settings.put(CompilerOptions.OPTION_Source,
                        CompilerOptions.VERSION_1_5);
            }
        } else {
            // Default to 1.5
            settings.put(CompilerOptions.OPTION_Source,
                    CompilerOptions.VERSION_1_5);
        }

        // Target JVM
        if(ctxt.getOptions().getCompilerTargetVM() != null) {
            String opt = ctxt.getOptions().getCompilerTargetVM();
            if(opt.equals("1.1")) {
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                             CompilerOptions.VERSION_1_1);
            } else if(opt.equals("1.2")) {
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                             CompilerOptions.VERSION_1_2);
            } else if(opt.equals("1.3")) {
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                             CompilerOptions.VERSION_1_3);
            } else if(opt.equals("1.4")) {
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                             CompilerOptions.VERSION_1_4);
            } else if(opt.equals("1.5")) {
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                             CompilerOptions.VERSION_1_5);
                settings.put(CompilerOptions.OPTION_Compliance,
                        CompilerOptions.VERSION_1_5);
            } else {
                log.warn("Unknown target VM " + opt + " ignored.");
                settings.put(CompilerOptions.OPTION_TargetPlatform,
                        CompilerOptions.VERSION_1_5);
            }
        } else {
            // Default to 1.5
            settings.put(CompilerOptions.OPTION_TargetPlatform,
                    CompilerOptions.VERSION_1_5);
            settings.put(CompilerOptions.OPTION_Compliance,
                    CompilerOptions.VERSION_1_5);
        }

        final IProblemFactory problemFactory =
            new DefaultProblemFactory(Locale.getDefault());

        final ICompilerRequestor requestor = new ICompilerRequestor() {
                public void acceptResult(CompilationResult result) {
                    try {
                        if (result.hasProblems()) {
                            IProblem[] problems = result.getProblems();
                            for (int i = 0; i < problems.length; i++) {
                                IProblem problem = problems[i];
                                if (problem.isError()) {
                                    String name =
                                        new String(problems[i].getOriginatingFileName());
                                    try {
                                        problemList.add(ErrorDispatcher.createJavacError
                                                (name, pageNodes, new StringBuffer(problem.getMessage()),
                                                        problem.getSourceLineNumber(), ctxt));
                                    } catch (JasperException e) {
                                        log.error("Error visiting node", e);
                                    }
                                }
                            }
                        }
                        if (problemList.isEmpty()) {
                            ClassFile[] classFiles = result.getClassFiles();
                            for (int i = 0; i < classFiles.length; i++) {
                                ClassFile classFile = classFiles[i];
                                char[][] compoundName =
                                    classFile.getCompoundName();
                                String className = "";
                                String sep = "";
                                for (int j = 0;
                                     j < compoundName.length; j++) {
                                    className += sep;
                                    className += new String(compoundName[j]);
                                    sep = ".";
                                }
                                byte[] bytes = classFile.getBytes();
                                String outFile = outputDir + "/" +
                                    className.replace('.', '/') + ".class";
                                OutputStream out = ctxt.getOutputStream(outFile);
                                BufferedOutputStream bos =
                                    new BufferedOutputStream(out);
                                bos.write(bytes);
                                bos.close();
                            }
                        }
                    } catch (IOException exc) {
                        log.error("Compilation error", exc);
                    }
                }
            };

        ICompilationUnit[] compilationUnits =
            new ICompilationUnit[classNames.length];
        for (int i = 0; i < compilationUnits.length; i++) {
            String className = classNames[i];
            compilationUnits[i] = new CompilationUnit(fileNames[i], className);
        }
        Compiler compiler = new Compiler(env,
                                         policy,
                                         settings,
                                         requestor,
                                         problemFactory,
                                         true);
        compiler.compile(compilationUnits);

        if (!ctxt.keepGenerated()) {
            ctxt.delete(ctxt.getServletJavaFileName());
        }

        if (!problemList.isEmpty()) {
            JavacErrorDetail[] jeds =
                (JavacErrorDetail[]) problemList.toArray(new JavacErrorDetail[0]);
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
        if (! options.isSmapSuppressed()) {
            SmapUtil.installSmap(getCompilationContext(), smap);
        }

    }


}

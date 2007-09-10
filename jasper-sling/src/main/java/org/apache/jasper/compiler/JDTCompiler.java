/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.jasper.JasperException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
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
 * @author fmeschbe based on work by Cocoon2 and Remy Maucherat
 */
public final class JDTCompiler extends org.apache.jasper.compiler.Compiler
        implements ICompilationUnit, ICompilerRequestor, INameEnvironment {

    // the shared problem factory, need not be created on each compilation
    private static IProblemFactory PROBLEM_FACTORY =
        new DefaultProblemFactory(Locale.getDefault());
    
    private CompilerOptions jdtCompilerOptions;
    
    // list of compilation problems encountered, cleared after compilation
    private List problemList;
    
    // current class output file, cleared after compilation
    private String targetClassName;
    
    private CompilerOptions getJDTCompilerOptions() {
        if (jdtCompilerOptions == null) {
            CompilerOptions co = new CompilerOptions();
            
            // debug generation
            co.produceDebugAttributes = ClassFileConstants.ATTR_LINES | ClassFileConstants.ATTR_SOURCE;
            if (options.getClassDebugInfo()) co.produceDebugAttributes |= ClassFileConstants.ATTR_VARS;
            
            // ignore deprecated API
            co.errorThreshold &= ~CompilerOptions.UsingDeprecatedAPI;
            co.warningThreshold &= ~CompilerOptions.UsingDeprecatedAPI;
          
            // java encoding
            if (options.getJavaEncoding() != null && options.getJavaEncoding().length() > 0) {
                co.defaultEncoding = options.getJavaEncoding();
            }
    
            // source and target vm level, default to 1.4 !
            long level = 0;
            if (options.getCompilerSourceVM() != null) {
                level = CompilerOptions.versionToJdkLevel(options.getCompilerSourceVM());
            }
            co.sourceLevel = (level != 0) ? level : ClassFileConstants.JDK1_4;
            
            level = 0;
            if (options.getCompilerTargetVM() != null) {
                level = CompilerOptions.versionToJdkLevel(options.getCompilerTargetVM());
            }
            co.targetJDK = (level != 0) ? level : ClassFileConstants.JDK1_4;
            if (co.targetJDK >= ClassFileConstants.JDK1_5) {
                co.inlineJsrBytecode = true; // forced in 1.5 mode
                co.complianceLevel = co.targetJDK;
            }

            co.parseLiteralExpressionsAsConstants = true;
            
            jdtCompilerOptions = co;
        }
        
        return jdtCompilerOptions;
    }

    /** 
     * Compile the servlet from .java file to .class file
     */
    protected void generateClass(String[] smap)
        throws FileNotFoundException, JasperException, Exception {
        try {
            generateClassInternal(smap);
        } finally {
            // remove field values
            problemList = null;
            targetClassName = null;
        }
    }
    
    /** 
     * Compile the servlet from .java file to .class file
     */
    private void generateClassInternal(String[] smap)
        throws FileNotFoundException, JasperException, Exception {

        long t1 = 0;
        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }
        
        String packageName = ctxt.getServletPackageName();
        targetClassName = ((packageName.length() != 0)
                ? (packageName + ".")
                : "")
            + ctxt.getServletClassName();
        
        IErrorHandlingPolicy policy = 
            DefaultErrorHandlingPolicies.proceedWithAllProblems();

        CompilerOptions settings = getJDTCompilerOptions();

        Compiler compiler = new Compiler(
            this,             // INameEnvironment
            policy,           // IErrorHandlerPolicy
            settings,         // CompilerOptions
            this,             // ICompilationRequestor
            PROBLEM_FACTORY,  // IProblemFactory 
            null);            // PrintWriter to catch log messages ...
        compiler.compile(new ICompilationUnit[]{ this });

        if (!ctxt.keepGenerated()) {
            ctxt.delete(ctxt.getServletJavaFileName());
        }
    
        if (problemList != null && !problemList.isEmpty()) {
            JavacErrorDetail[] problems = (JavacErrorDetail[]) problemList.toArray(new JavacErrorDetail[problemList.size()]);
            errDispatcher.javacError(problems);
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
            SmapUtil.installSmap(ctxt, smap);
        }
    }
    
    //---------- ICompilationUnit ---------------------------------------------
    
    public char[] getFileName() {
        return ctxt.getServletJavaFileName().toCharArray();
    }

    public char[] getContents() {
        char[] result = null;
        try {
            InputStream ins = ctxt.getInputStream(ctxt.getServletJavaFileName());
            InputStreamReader isReader = new InputStreamReader(ins,
                ctxt.getOptions().getJavaEncoding());
            Reader reader = new BufferedReader(isReader);
            if (reader != null) {
                char[] chars = new char[8192];
                StringBuffer buf = new StringBuffer();
                int count;
                while ((count = reader.read(chars, 0, chars.length)) > 0) {
                    buf.append(chars, 0, count);
                }
                result = new char[buf.length()];
                buf.getChars(0, result.length, result, 0);
            }
        } catch (IOException e) {
            log.error("Compilation error", e);
        }
        return result;
    }

    public char[] getMainTypeName() {
        int dot = targetClassName.lastIndexOf('.');
        if (dot > 0) {
            return targetClassName.substring(dot + 1).toCharArray();
        }
        return targetClassName.toCharArray();
    }

    public char[][] getPackageName() {
        StringTokenizer izer = new StringTokenizer(targetClassName, ".");
        char[][] result = new char[izer.countTokens() - 1][];
        for (int i = 0; i < result.length; i++) {
            String tok = izer.nextToken();
            result[i] = tok.toCharArray();
        }
        return result;
    }
    
    //---------- ICOmpilerRequestor -------------------------------------------
    
    public void acceptResult(CompilationResult result) {
        try {
            if (result.hasProblems()) {
                problemList = new ArrayList();
                IProblem[] problems = result.getProblems();
                for (int i = 0; i < problems.length; i++) {
                    IProblem problem = problems[i];
                    if (problem.isError()) {
                        String name = new String(
                            problems[i].getOriginatingFileName());
                        try {
                            problemList.add(ErrorDispatcher.createJavacError(
                                name, pageNodes, new StringBuffer(
                                    problem.getMessage()),
                                problem.getSourceLineNumber()));
                        } catch (JasperException e) {
                            log.error("Error visiting node", e);
                        }
                    }
                }
            }
            if (problemList == null || problemList.isEmpty()) {
                ClassFile[] classFiles = result.getClassFiles();
                for (int i = 0; i < classFiles.length; i++) {
                    ClassFile classFile = classFiles[i];
                    char[][] compoundName = classFile.getCompoundName();
                    String className = toString(compoundName).toString();
                    writeClass(className, classFile.getBytes());
                }
            }
        } catch (IOException exc) {
            log.error("Compilation error", exc);
        }
    }

    private void writeClass(String className, byte[] bytes) throws IOException {
        String path = ctxt.getOptions().getScratchDir() + "/"
            + className.replace('.', '/') + ".class";
        OutputStream out = null;
        try {
            out = ctxt.getOutputStream(path);
            out = new BufferedOutputStream(out);
            out.write(bytes);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
    
    //---------- INameEnvironment -----------------------------------------
    
    private StringBuffer toString(char[][] compoundName) {
        StringBuffer buf = new StringBuffer();
        
        if (compoundName != null) {
            for (int i = 0; i < compoundName.length; i++) {
                if (i > 0) buf.append('.');
                buf.append(compoundName[i]);
            }
        }
        
        return buf;
    }
    
    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
        StringBuffer result = toString(compoundTypeName);
        return findType(result.toString());
    }

    public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
        StringBuffer result = toString(packageName);
        if (result.length() > 0) result.append('.');
        result.append(typeName);
        return findType(result.toString());
    }
    
    private NameEnvironmentAnswer findType(String className) {

        InputStream is = null;
        try {
            // if the class is our own, just return the answer for us
            if (className.equals(targetClassName)) {
                ICompilationUnit compilationUnit = this;
                return new NameEnvironmentAnswer(compilationUnit, null);
            }

            // otherwise locate the class through the class loader
            String resourceName = className.replace('.', '/') + ".class";
            is = ctxt.getJspLoader().getResourceAsStream(resourceName);
            if (is != null) {
                ClassFileReader classFileReader =
                    ClassFileReader.read(is, className, true);
                return new NameEnvironmentAnswer(classFileReader, null);
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
        InputStream is = ctxt.getJspLoader().getResourceAsStream(resourceName);
        return is == null;
    }

    public boolean isPackage(char[][] parentPackageName, 
                             char[] packageName) {
        StringBuffer buf = toString(parentPackageName);
        
        if (Character.isUpperCase(packageName[0])) {
            if (!isPackage(buf.toString())) {
                return false;
            }
        }
        
        if (buf.length() > 0) buf.append('.');
        buf.append(packageName);

        return isPackage(buf.toString());
    }

    public void cleanup() {
    }

}

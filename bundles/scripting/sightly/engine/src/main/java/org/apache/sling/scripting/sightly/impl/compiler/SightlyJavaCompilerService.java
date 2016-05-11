/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.impl.engine.UnitLoader;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.apache.sling.scripting.sightly.impl.engine.extension.use.UseProviderUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The {@code SightlyJavaCompiler} allows for simple instantiation of arbitrary classes that are either stored in the repository
 * or in regular OSGi bundles. It also compiles Java sources on-the-fly and can discover class' source files based on
 * {@link Resource}s (typically Sling components). It supports Sling Resource type inheritance.
 */
@Component
@Service(SightlyJavaCompilerService.class)
public class SightlyJavaCompilerService {

    private static final Logger LOG = LoggerFactory.getLogger(SightlyJavaCompilerService.class);

    public static final Pattern PACKAGE_DECL_PATTERN = Pattern.compile("(\\s*)package\\s+([a-zA-Z_$][a-zA-Z\\d_$]*\\.?)+;");

    @Reference
    private ClassLoaderWriter classLoaderWriter = null;

    @Reference
    private JavaCompiler javaCompiler = null;

    @Reference
    private UnitChangeMonitor unitChangeMonitor = null;

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    private Options options;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    /**
     * This method returns an Object instance based on a class that is either found through regular classloading mechanisms or on-the-fly
     * compilation. In case the requested class does not denote a fully qualified classname, this service will try to find the class through
     * Sling's servlet resolution mechanism and compile the class on-the-fly if required.
     *
     * @param className     name of class to use for object instantiation
     * @return object instance of the requested class
     * @throws CompilerException in case of any runtime exception
     */
    public Object getInstance(RenderContext renderContext, String className) {
        LOG.debug("Attempting to load class {}.", className);
        if (className.contains(".")) {
            if (unitChangeMonitor.getLastModifiedDateForJavaUseObject(className) > 0) {
                // it looks like the POJO comes from the repo and it was changed since it was last loaded
                LOG.debug("Class {} identifies a POJO from the repository that was changed since the last time it was instantiated.",
                        className);
                Object result = compileRepositoryJavaClass(renderContext.getScriptResourceResolver(), className);
                unitChangeMonitor.clearJavaUseObject(className);
                return result;
            }
            try {
                // the object either comes from a bundle or from the repo but it was not registered by the UnitChangeMonitor
                LOG.debug("Attempting to load class {} from the classloader cache.", className);
                return loadObject(className);
            } catch (CompilerException cex) {
                // the object definitely doesn't come from a bundle so we should attempt to compile it from the repo
                LOG.debug("Class {} identifies a POJO from the repository and it needs to be compiled.", className);
                return compileRepositoryJavaClass(renderContext.getScriptResourceResolver(), className);
            }
        } else {
            Resource pojoResource = UseProviderUtils.locateScriptResource(renderContext, className + ".java");
            if (pojoResource != null) {
                String fqcn = Utils.getJavaNameFromPath(pojoResource.getPath());
                LOG.debug("Class {} has FQCN {}.", className, fqcn);
                return getInstance(renderContext, fqcn);
            }
        }
        throw new SightlyException("Cannot find class " + className + ".");
    }

    /**
     * Compiles a class using the passed fully qualified class name and its source code.
     *
     * @param sourceCode the source code from which to generate the class
     * @return object instance of the class to compile
     * @throws CompilerException in case of any runtime exception
     */
    public Object compileSource(SourceIdentifier sourceIdentifier, String sourceCode) {
        readLock.lock();
        try {
            if (sourceIdentifier.needsUpdate()) {
                readLock.unlock();
                writeLock.lock();
                try {
                    LOG.debug("Need to recompile {}.", sourceIdentifier.toString());
                    return internalCompileSource(sourceCode, sourceIdentifier.getFullyQualifiedName());
                } finally {
                    // downgrade write lock since we've probably compiled the source code by now
                    readLock.lock();
                    writeLock.unlock();
                }
            } else {
                LOG.debug("No need to recompile {}, loading it from the classloader cache.", sourceIdentifier.toString());
                return classLoaderWriter.getClassLoader().loadClass(sourceIdentifier.getFullyQualifiedName()).newInstance();
            }
        } catch (Exception e) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, e);
        } finally {
            readLock.unlock();
        }
    }

    private Object internalCompileSource(String sourceCode, String fqcn) throws Exception {
        if (sightlyEngineConfiguration.keepGenerated()) {
            String path = "/" + fqcn.replaceAll("\\.", "/") + ".java";
            OutputStream os = classLoaderWriter.getOutputStream(path);
            IOUtils.write(sourceCode, os, "UTF-8");
            IOUtils.closeQuietly(os);
        }
        String[] sourceCodeLines = sourceCode.split("\\r\\n|[\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]");
        boolean foundPackageDeclaration = false;
        for (String line : sourceCodeLines) {
            Matcher matcher = PACKAGE_DECL_PATTERN.matcher(line);
            if (matcher.matches()) {
                /**
                 * This matching might return false positives like:
                 * // package a.b.c;
                 *
                 * where from a syntactic point of view the source code doesn't have a package declaration and the expectancy is that our
                 * SightlyJavaCompilerService will add one.
                 */
                foundPackageDeclaration = true;
                break;
            }
        }

        if (!foundPackageDeclaration) {
            sourceCode = "package " + Utils.getPackageNameFromFQCN(fqcn) + ";\n" + sourceCode;
        }

        CompilationUnit compilationUnit = new SightlyCompilationUnit(sourceCode, fqcn);
        long start = System.currentTimeMillis();
        CompilationResult compilationResult = javaCompiler.compile(new CompilationUnit[]{compilationUnit}, options);
        long end = System.currentTimeMillis();
        List<CompilerMessage> errors = compilationResult.getErrors();
        if (errors != null && errors.size() > 0) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, createErrorMsg(errors));
        }
        if (compilationResult.didCompile()) {
            LOG.debug("Class {} was compiled in {}ms.", fqcn, end - start);
        }
        /**
         * the class loader might have become dirty, so let the {@link ClassLoaderWriter} decide which class loader to return
         */
        return classLoaderWriter.getClassLoader().loadClass(fqcn).newInstance();
    }

    private Object compileRepositoryJavaClass(ResourceResolver resolver, String className) {
        String pojoPath = getPathFromJavaName(resolver, className);
        Resource pojoResource = resolver.getResource(pojoPath);
        if (pojoResource != null) {
            try {
                SourceIdentifier sourceIdentifier = new SourceIdentifier(sightlyEngineConfiguration, unitChangeMonitor,
                        classLoaderWriter, pojoResource);
                return compileSource(sourceIdentifier, IOUtils.toString(pojoResource.adaptTo(InputStream.class), "UTF-8"));
            } catch (IOException e) {
                throw new SightlyException(String.format("Unable to compile class %s from %s.", className, pojoPath), e);
            }
        }
        throw new SightlyException("Cannot find a a file corresponding to class " + className + " in the repository.");
    }

    /**
     * For a JCR path obtained from expanding a generated class name this method generates all the alternative path names that can be
     * obtained by expanding the mentioned class' name.
     *
     * @param originalPath one of the possible paths
     * @return a {@link Set} containing all the alternative paths if symbol replacement was needed; otherwise the set will contain just
     * the {@code originalPath}
     */
    private Set<String> getPossiblePojoPaths(String originalPath) {
        Set<String> possiblePaths = new LinkedHashSet<String>();
        possiblePaths.add(originalPath);
        Map<Integer, String> chars = new HashMap<Integer, String>();
        AmbiguousPathSymbol[] symbols = AmbiguousPathSymbol.values();
        for (AmbiguousPathSymbol symbol : symbols) {
            String pathCopy = originalPath.substring(0, originalPath.lastIndexOf("/"));
            int actualIndex = 0;
            boolean firstPass = true;
            while (pathCopy.indexOf(symbol.getSymbol()) != -1) {
                int pos = pathCopy.indexOf(symbol.getSymbol());
                actualIndex += pos;
                if (!firstPass) {
                    actualIndex += 1;
                }
                chars.put(actualIndex, symbol.getSymbol().toString());
                pathCopy = pathCopy.substring(pos + 1);
                firstPass = false;
            }
        }
        if (chars.size() > 0) {
            ArrayList<AmbiguousPathSymbol[]> possibleArrangements = new ArrayList<AmbiguousPathSymbol[]>();
            populateArray(possibleArrangements, new AmbiguousPathSymbol[chars.size()], 0);
            Integer[] indexes = chars.keySet().toArray(new Integer[chars.size()]);
            for (AmbiguousPathSymbol[] arrangement : possibleArrangements) {
                char[] possiblePath = originalPath.toCharArray();
                for (int i = 0; i < arrangement.length; i++) {
                    char currentSymbol = arrangement[i].getSymbol();
                    int currentIndex = indexes[i];
                    possiblePath[currentIndex] = currentSymbol;
                }
                possiblePaths.add(new String(possiblePath));
            }
        }
        return possiblePaths;
    }

    /**
     * Given an initial array with its size equal to the number of elements of a needed arrangement, this method will generate all
     * the possible arrangements of values for this array in the provided {@code arrayCollection}. The values with which the array is
     * populated are the {@link AmbiguousPathSymbol} constants.
     *
     * @param arrayCollection the collection that will store the arrays
     * @param symbolsArrangementArray an initial array that will be used for collecting the results
     * @param index the initial index of the array that will be populated (needed for recursion purposes; start with 0 for the initial call)
     */
    private void populateArray(ArrayList<AmbiguousPathSymbol[]> arrayCollection, AmbiguousPathSymbol[] symbolsArrangementArray, int index) {
        if (symbolsArrangementArray.length > 0) {
            if (index == symbolsArrangementArray.length) {
                arrayCollection.add(symbolsArrangementArray.clone());
            } else {
                for (AmbiguousPathSymbol symbol : AmbiguousPathSymbol.values()) {
                    symbolsArrangementArray[index] = symbol;
                    populateArray(arrayCollection, symbolsArrangementArray, index + 1);
                }
            }
        }
    }

    /**
     * Instantiate and return an instance of a class.
     *
     * @param className class to instantiate
     * @return instance of class
     */
    private Object loadObject(String className) {
        try {
            if (classLoaderWriter != null) {
                return classLoaderWriter.getClassLoader().loadClass(className).newInstance();
            }
            return Class.forName(className).newInstance();
        } catch (Throwable t) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, t);
        }
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate() {
        LOG.info("Activating {}", getClass().getName());

        String version = System.getProperty("java.specification.version");
        options = new Options();
        options.put(Options.KEY_GENERATE_DEBUG_INFO, true);
        options.put(Options.KEY_SOURCE_VERSION, version);
        options.put(Options.KEY_TARGET_VERSION, version);
        options.put(Options.KEY_CLASS_LOADER_WRITER, classLoaderWriter);
        options.put(Options.KEY_FORCE_COMPILATION, true);
    }

    //---------------------------------- private -----------------------------------

    private String getPathFromJavaName(ResourceResolver resolver, String className) {
        boolean sightlyGeneratedClass = false;
        if (className.contains("." + UnitLoader.CLASS_NAME_PREFIX)) {
            sightlyGeneratedClass = true;
        }
        String packageName = className.substring(0, className.lastIndexOf('.'));
        String shortClassName = className.substring(packageName.length() + 1);
        String path = "/" + packageName.replace(".", "/").replace("_", "-") + "/" + shortClassName + ".java";
        if (sightlyGeneratedClass) {
            return path;
        } else {
            Set<String> possiblePaths = getPossiblePojoPaths(path);
            for (String possiblePath : possiblePaths) {
                if (resolver.getResource(possiblePath) != null) {
                    return possiblePath;
                }
            }
            return path;
        }
    }

    private String createErrorMsg(List<CompilerMessage> errors) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Compilation errors in ");
        buffer.append(errors.get(0).getFile());
        buffer.append(":");
        StringBuilder errorsBuffer = new StringBuilder();
        boolean duplicateVariable = false;
        for (final CompilerMessage e : errors) {
            if (!duplicateVariable) {
                if (e.getMessage().contains("Duplicate local variable")) {
                    duplicateVariable = true;
                    buffer.append(" Maybe you defined more than one identical block elements without defining a different variable for "
                            + "each one?");
                }
            }
            errorsBuffer.append("\nLine ");
            errorsBuffer.append(e.getLine());
            errorsBuffer.append(", column ");
            errorsBuffer.append(e.getColumn());
            errorsBuffer.append(" : ");
            errorsBuffer.append(e.getMessage());
        }
        buffer.append(errorsBuffer);
        return buffer.toString();
    }

    private static class SightlyCompilationUnit implements CompilationUnit {

        private String fqcn;
        private String sourceCode;

        SightlyCompilationUnit(String sourceCode, String fqcn) throws Exception {
            this.sourceCode = sourceCode;
            this.fqcn = fqcn;
        }

        @Override
        public Reader getSource() throws IOException {
            return new InputStreamReader(IOUtils.toInputStream(sourceCode, "UTF-8"), "UTF-8");
        }

        @Override
        public String getMainClassName() {
            return fqcn;
        }

        @Override
        public long getLastModified() {
            return System.currentTimeMillis();
        }
    }

    /**
     * The {@code AmbiguousPathSymbol} holds symbols that are valid for a JCR path but that will get transformed to a "_" to obey the
     * Java naming conventions.
     */
    enum AmbiguousPathSymbol {
        DASH('-'),
        UNDERSCORE('_');

        private Character symbol;

        AmbiguousPathSymbol(Character symbol) {
            this.symbol = symbol;
        }

        public Character getSymbol() {
            return symbol;
        }
    }

}

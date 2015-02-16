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
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.jcr.compiler.JcrJavaCompiler;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.engine.UnitChangeMonitor;
import org.apache.sling.scripting.sightly.impl.engine.UnitLoader;
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

    @Reference
    private ClassLoaderWriter classLoaderWriter = null;

    @Reference
    private JcrJavaCompiler jcrJavaCompiler = null;

    @Reference
    private UnitChangeMonitor unitChangeMonitor = null;

    private Options options;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    /**
     * This method returns an Object instance based on a class that is either found through regular classloading mechanisms or on-the-fly
     * compilation. In case the requested class does not denote a fully qualified classname, this service will try to find the class through
     * Sling's servlet resolution mechanism and compile the class on-the-fly if required.
     *
     * @param resolver      a {@link ResourceResolver} with read access to resources from the search path (see {@link
     *                      ResourceResolver#getSearchPath()})
     * @param callingScript the lookup will be performed based on this resource only if the {@code className} does not identify a fully
     *                      qualified class name; otherwise this parameter can be {@code null}
     * @param className     name of class to use for object instantiation
     * @return object instance of the requested class
     * @throws CompilerException in case of any runtime exception
     */
    public Object getInstance(ResourceResolver resolver, Resource callingScript, String className) {
        if (className.contains(".")) {
            String pojoPath = getPathFromJavaName(resolver, className);
            if (unitChangeMonitor.getLastModifiedDateForJavaUseObject(pojoPath) > 0) {
                // it looks like the POJO comes from the repo and it was changed since it was last loaded
                Resource pojoResource = resolver.getResource(pojoPath);
                if (pojoResource != null) {
                    // clear the cache as we need to recompile the POJO object
                    unitChangeMonitor.clearJavaUseObject(pojoPath);
                    return compileSource(pojoResource, className);
                } else {
                    throw new SightlyException(String.format("Resource %s identifying class %s has been removed.", pojoPath, className));
                }
            } else {
                try {
                    // the object either comes from a bundle or from the repo but it was not registered by the UnitChangeMonitor
                    return loadObject(className);
                } catch (CompilerException cex) {
                    // the object definitely doesn't come from a bundle so we should attempt to compile it from the repo
                    Resource pojoResource = resolver.getResource(pojoPath);
                    if (pojoResource != null) {
                        return compileSource(pojoResource, className);
                    }
                }
            }
        } else {
            if (callingScript != null) {
                Resource pojoResource = ResourceResolution.getResourceFromSearchPath(callingScript, className + ".java");
                if (pojoResource != null) {
                    return getInstance(resolver, null, getJavaNameFromPath(pojoResource.getPath()));
                }
            }
        }
        throw new SightlyException("Cannot find class " + className + ".");
    }

    /**
     * Compiles a class using the passed fully qualified classname and based on the resource that represents the class' source.
     *
     * @param javaResource resource that constitutes the class' source
     * @param fqcn         fully qualified name of the class to compile
     * @return object instance of the class to compile
     * @throws CompilerException in case of any runtime exception
     */
    public Object compileSource(Resource javaResource, String fqcn) {
        LOG.debug("Compiling Sightly based Java class from resource: " + javaResource.getPath());
        try {
            CompilationUnit compilationUnit = new SightlyCompilationUnit(javaResource, fqcn);
            return compileJavaResource(compilationUnit, javaResource.getPath());
        } catch (Exception e) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, e);
        }
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
            readLock.lock();
            if (classLoaderWriter != null) {
                return classLoaderWriter.getClassLoader().loadClass(className).newInstance();
            }
            return Class.forName(className).newInstance();
        } catch (Throwable t) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, t);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Compiles a class stored in the repository and returns an instance of the compiled class.
     *
     * @param compilationUnit a compilation unit
     * @param scriptPath      the path of the script to compile
     * @return instance of compiled class
     * @throws Exception
     */
    private Object compileJavaResource(CompilationUnit compilationUnit, String scriptPath) throws Exception {
        writeLock.lock();
        try {
            long start = System.currentTimeMillis();
            CompilationResult compilationResult = jcrJavaCompiler.compile(new String[]{scriptPath}, options);
            long end = System.currentTimeMillis();
            List<CompilerMessage> errors = compilationResult.getErrors();
            if (errors != null && errors.size() > 0) {
                throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, createErrorMsg(errors));
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("script compiled: {}", compilationResult.didCompile());
                LOG.debug("compilation took {}ms", end - start);
            }
            return compilationResult.loadCompiledClass(compilationUnit.getMainClassName()).newInstance();
        } catch (Throwable t) {
            throw new CompilerException(CompilerException.CompilerExceptionCause.COMPILER_ERRORS, t);
        } finally {
            writeLock.unlock();
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
    }

    //---------------------------------- private -----------------------------------
    private String getJavaNameFromPath(String path) {
        if (path.endsWith(".java")) {
            path = path.substring(0, path.length() - 5);
        }
        return path.substring(1).replace("/", ".").replace("-", "_");
    }

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

    class SlingResourceCompilationUnit implements CompilationUnit {
        private final Resource resource;

        public SlingResourceCompilationUnit(Resource resource) {
            this.resource = resource;
        }

        public Reader getSource() throws IOException {
            return new InputStreamReader(resource.adaptTo(InputStream.class), "UTF-8");
        }

        public String getMainClassName() {
            return getJavaNameFromPath(resource.getPath());
        }

        public long getLastModified() {
            return resource.getResourceMetadata().getModificationTime();
        }
    }

    class SightlyCompilationUnit extends SlingResourceCompilationUnit {
        private String fqcn;

        public SightlyCompilationUnit(Resource resource, String fqcn) {
            super(resource);
            this.fqcn = fqcn;
        }

        public String getMainClassName() {
            return fqcn;
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

        private AmbiguousPathSymbol(Character symbol) {
            this.symbol = symbol;
        }

        public Character getSymbol() {
            return symbol;
        }
    }

}

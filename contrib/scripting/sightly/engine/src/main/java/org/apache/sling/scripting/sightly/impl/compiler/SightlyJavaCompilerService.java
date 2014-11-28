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
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.jcr.compiler.JcrJavaCompiler;
import org.apache.sling.scripting.sightly.ResourceResolution;
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
    private ResourceResolverFactory rrf = null;

    private Options options;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    /**
     * This method returns an Object instance based on a class that is either found through regular classloading mechanisms or on-the-fly
     * compilation. In case the requested class does not denote a fully qualified classname, this service will try to find the class through
     * Sling's servlet resolution mechanism and compile the class on-the-fly if required.
     *
     * @param resource  the lookup will be performed based on this resource
     * @param className name of class to use for object instantiation
     * @return object instance of the requested class
     * @throws CompilerException in case of any runtime exception
     */
    public Object getInstance(Resource resource, String className) {

        LOG.debug("Attempting to obtain bean instance of resource '{}' and class '{}'", resource.getPath(), className);

        // assume fully qualified class name
        if (className.contains(".")) {
            Resource pojoResource = checkIfPojoIsInRepo(className);
            if (pojoResource != null) {
                return compileSource(pojoResource, className);
            } else {
                LOG.debug("fully qualified classname provided, loading object directly");
                return loadObject(className);
            }
        }

        LOG.debug("trying to find Java source based on resource: {}", resource.getPath());

        // try to find Java source in JCR
        // use the servlet resolver to do all the magic lookup (resource type hierarchy and search path) for us

        Resource scriptResource = ResourceResolution
                .resolveComponentRelative(resource.getResourceResolver(), resource, className + ".java");
        if (scriptResource != null) {
            LOG.debug("found Java bean script resource: " + scriptResource.getPath());
            try {
                return compileJavaResource(new SlingResourceCompilationUnit(scriptResource), scriptResource.getPath());
            } catch (Exception e) {
                throw new CompilerException(e);
            }
        } else {
            // not found in JCR, try to load from bundle using current resource type package
            // /apps/project/components/foo => apps.project.components.foo.<scriptName>
            Resource resourceType = resource.getResourceResolver().getResource(resource.getResourceType());

            if (resourceType == null) {
                resourceType = resource;
            }

            String resourceTypeDir = resourceType.getPath();
            className = getJavaNameFromPath(resourceTypeDir) + "." + className;

            LOG.debug("Java bean source not found, trying to locate using" + " component directory as packagename: {}", resourceTypeDir);

            LOG.debug("loading Java class: " + className);
            return loadObject(className);
        }
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
            throw new CompilerException(e);
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
            return loadClass(className).newInstance();
        } catch (Exception e) {
            throw new CompilerException(e);
        }
    }

    /**
     * Retrieve a class from the ClassLoaderWriter service.
     *
     * @param name name of class to load
     * @return Class
     * @throws ClassNotFoundException
     */
    private Class loadClass(String name) throws ClassNotFoundException {
        readLock.lock();
        try {
            if (classLoaderWriter != null) {
                return classLoaderWriter.getClassLoader().loadClass(name);
            }
        } finally {
            readLock.unlock();
        }
        return Class.forName(name);
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
                throw new CompilerException(createErrorMsg(errors));
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("script compiled: {}", compilationResult.didCompile());
                LOG.debug("compilation took {}ms", end - start);
            }
            return compilationResult.loadCompiledClass(compilationUnit.getMainClassName()).newInstance();
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

    /**
     * Checks is a POJO class name is represented by a resource from the repository.
     *
     * @param className the class name
     * @return the Resource in which the class is defined, {@code null} if the POJO was not found in the repository
     */
    private Resource checkIfPojoIsInRepo(String className) {
        // POJOs will always be loaded through the compiler (prevents stale class loader issues)
        String pojoPath = "/" + className.replaceAll("\\.", "/") + ".java";
        ResourceResolver rr = null;
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            Resource pojoResource = rr.getResource(pojoPath);
            if (pojoResource != null) {
                for (String s : rr.getSearchPath()) {
                    if (pojoPath.startsWith(s)) {
                        return pojoResource;
                    }
                }
            }
        } catch (LoginException le) {
            LOG.error("Cannot search repository for POJO.", le);
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
        return null;
    }

    private String getJavaNameFromPath(String path) {
        if (path.endsWith(".java")) {
            path = path.substring(0, path.length() - 5);
        }
        return path.substring(1).replace("/", ".").replace("-", "_");
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

}

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
package org.apache.sling.scripting.sightly.impl.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Bindings;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.compiled.CompilationOutput;
import org.apache.sling.scripting.sightly.impl.compiled.JavaClassBackend;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyParsingException;
import org.apache.sling.scripting.sightly.impl.compiler.util.GlobalShadowCheckBackend;
import org.apache.sling.scripting.sightly.impl.engine.compiled.JavaClassTemplate;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create rendering units from resources.
 */
@Component
@Service(UnitLoader.class)
public class UnitLoader {

    public static final String DEFAULT_REPO_BASE_PATH = "/var/classes";
    public static final String CLASS_NAME_PREFIX = "SightlyJava_";
    private static final Logger log = LoggerFactory.getLogger(UnitLoader.class);
    private static final String MAIN_TEMPLATE_PATH = "templates/compiled_unit_template.txt";
    private static final String CHILD_TEMPLATE_PATH = "templates/subtemplate.txt";

    private static final String NT_FOLDER = "nt:folder";
    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_LASTMODIFIED = "jcr:lastModified";

    private String mainTemplate;
    private String childTemplate;

    private final Map<String, Lock> activeWrites = new HashMap<String, Lock>();

    @Reference
    private SightlyCompilerService sightlyCompilerService = null;

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService = null;

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference
    private SlingSettingsService slingSettings = null;

    @Reference
    private UnitChangeMonitor unitChangeMonitor = null;

    /**
     * Create a render unit from the given resource
     *
     * @param scriptResource the resource
     * @param bindings       the bindings
     * @param renderContext  the rendering context
     * @return the render unit
     */
    public RenderUnit createUnit(Resource scriptResource, Bindings bindings, RenderContextImpl renderContext) {
        Lock lock = null;
        try {
            SourceIdentifier sourceIdentifier = obtainIdentifier(scriptResource);
            Object obj;
            ResourceMetadata resourceMetadata = scriptResource.getResourceMetadata();
            String encoding = resourceMetadata.getCharacterEncoding();
            if (encoding == null) {
                encoding = sightlyEngineConfiguration.getEncoding();
            }
            SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
            response.setCharacterEncoding(encoding);
            ResourceResolver adminResolver = renderContext.getScriptResourceResolver();
            if (needsUpdate(sourceIdentifier)) {
                synchronized (activeWrites) {
                    String sourceFullPath = sourceIdentifier.getSourceFullPath();
                    lock = activeWrites.get(sourceFullPath);
                    if (lock == null) {
                        lock = new ReentrantLock();
                        activeWrites.put(sourceFullPath, lock);
                    }
                    lock.lock();
                }
                Resource javaClassResource = createClass(adminResolver, sourceIdentifier, bindings, encoding, renderContext);
                obj = sightlyJavaCompilerService.compileSource(javaClassResource, sourceIdentifier.getFullyQualifiedName());
            } else {
                obj = sightlyJavaCompilerService.getInstance(adminResolver, null, sourceIdentifier.getFullyQualifiedName());
            }
            if (!(obj instanceof RenderUnit)) {
                throw new SightlyException("Class is not a RenderUnit instance");
            }
            return (RenderUnit) obj;
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        mainTemplate = resourceFile(componentContext, MAIN_TEMPLATE_PATH);
        childTemplate = resourceFile(componentContext, CHILD_TEMPLATE_PATH);
        String basePath =
                DEFAULT_REPO_BASE_PATH + "/" + slingSettings.getSlingId() + "/sightly/";
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            Resource basePathResource;
            if ((basePathResource = adminResolver.getResource(basePath)) != null) {
                for (Resource resource : basePathResource.getChildren()) {
                    if (!resource.getName().equals(sightlyEngineConfiguration.getEngineVersion())) {
                        adminResolver.delete(resource);
                    }
                }
                if (adminResolver.hasChanges()) {
                    adminResolver.commit();
                }
            }
        } catch (Exception e) {
            log.error("Cannot delete stale Sightly Java classes.", e);
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
        }
    }

    private synchronized Resource writeSource(ResourceResolver resolver, String sourceFullPath, String source) {
        Resource sourceResource;
        try {
            String sourceParentPath = ResourceUtil.getParent(sourceFullPath);
            Map<String, Object> sourceFolderProperties = new HashMap<String, Object>();
            sourceFolderProperties.put(JCR_PRIMARY_TYPE, NT_FOLDER);
            createResource(resolver, sourceParentPath, sourceFolderProperties, NT_FOLDER, true, false);

            Map<String, Object> sourceFileProperties = new HashMap<String, Object>();
            sourceFileProperties.put(JCR_PRIMARY_TYPE, NT_FILE);
            sourceResource = createResource(resolver, sourceFullPath, sourceFileProperties, null, false, false);

            Map<String, Object> ntResourceProperties = new HashMap<String, Object>();
            ntResourceProperties.put(JCR_PRIMARY_TYPE, NT_RESOURCE);
            ntResourceProperties.put(JCR_DATA, new ByteArrayInputStream(source.getBytes()));
            ntResourceProperties.put(JCR_LASTMODIFIED, Calendar.getInstance());
            createResource(resolver, sourceFullPath + "/" + JCR_CONTENT, ntResourceProperties, NT_RESOURCE, true, true);
            log.debug("Successfully written Java source file to repository: {}", sourceFullPath);
        } catch (PersistenceException e) {
            throw new SightlyException("Repository error while writing Java source file: " + sourceFullPath, e);
        }
        return sourceResource;
    }

    private SourceIdentifier obtainIdentifier(Resource resource) {
        String basePath =
                DEFAULT_REPO_BASE_PATH + "/" + slingSettings.getSlingId() + "/sightly/" + sightlyEngineConfiguration.getEngineVersion();
        return new SourceIdentifier(resource, CLASS_NAME_PREFIX, basePath);
    }

    private Resource createClass(ResourceResolver resolver, SourceIdentifier identifier, Bindings bindings, String encoding,
                             RenderContextImpl renderContext) {
        String scriptSource = null;
        try {
            Resource scriptResource = resolver.getResource(identifier.getResource().getPath());
            if (scriptResource != null) {
                scriptSource = IOUtils.toString(scriptResource.adaptTo(InputStream.class), encoding);
                String javaSourceCode = obtainResultSource(scriptSource, identifier, bindings, renderContext);
                return writeSource(resolver, identifier.getSourceFullPath(), javaSourceCode);
            }
        } catch (SightlyParsingException e) {
            String offendingInput = e.getOffendingInput();
            if (StringUtils.isNotEmpty(offendingInput)) {
                offendingInput = StringEscapeUtils.unescapeHtml(offendingInput.trim());
                int errorLine = getLineWhereErrorOccurred(scriptSource, offendingInput);
                throw new SightlyException("Parsing error in template " + identifier.getResource().getPath() + " at line " +
                        errorLine + ": " + e.getMessage() + " for expression " + offendingInput);
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new SightlyException(e);
        }
        throw new SightlyException("Unable to generate Java class for template " + identifier.getResource().getPath());
    }

    private String obtainResultSource(String scriptSource, SourceIdentifier identifier, Bindings bindings, RenderContextImpl renderContext) {
        JavaClassTemplate classTemplate = newMainTemplate();
        classTemplate.setClassName(identifier.getClassName());
        classTemplate.setPackageName(identifier.getPackageName());
        CompilationOutput compilationOutput = obtainOutput(scriptSource, bindings, renderContext);
        processCompilationResult(compilationOutput, classTemplate);
        return classTemplate.toString();
    }

    private CompilationOutput obtainOutput(String source, Bindings bindings, RenderContextImpl renderContext) {
        JavaClassBackend backend = new JavaClassBackend();
        sightlyCompilerService.compile(source, new GlobalShadowCheckBackend(backend, bindings.keySet()), renderContext);
        return backend.build();
    }

    private void processCompilationResult(CompilationOutput result, JavaClassTemplate mainTemplate) {
        mainTemplate.writeMainBody(result.getMainBody());
        for (Map.Entry<String, CompilationOutput> entry : result.getSubTemplates().entrySet()) {
            JavaClassTemplate childTemplate = newChildTemplate();
            processCompilationResult(entry.getValue(), childTemplate);
            mainTemplate.writeSubTemplate(entry.getKey(), childTemplate.toString());
        }
    }

    private JavaClassTemplate newMainTemplate() {
        return new JavaClassTemplate(mainTemplate);
    }

    private JavaClassTemplate newChildTemplate() {
        return new JavaClassTemplate(childTemplate);
    }

    private String resourceFile(ComponentContext componentContext, String path) {
        InputStream inputStream = null;
        try {
            URL url = componentContext.getBundleContext().getBundle().getEntry(path);
            if (url == null) {
                throw new SightlyException("No bundle resource resides at " + path);
            }
            inputStream = componentContext.getBundleContext().getBundle().getEntry(path).openStream();
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            throw new SightlyException("Java class templates could not be found");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error when closing bundle resource stream", e);
                }
            }
        }
    }

    private int getLineWhereErrorOccurred(String documentFragment, String offendingInput) {
        int offendingInputIndex = documentFragment.indexOf(offendingInput);
        String textBeforeError = documentFragment.substring(0, offendingInputIndex);
        int line = 0;
        int newLine = 0;
        while (textBeforeError.length() > 0 && newLine != -1) {
            newLine = textBeforeError.indexOf("\n");
            if (newLine != -1) {
                line++;
                textBeforeError = textBeforeError.substring(newLine + 1, textBeforeError.length());
            }
        }
        return ++line;
    }

    private boolean needsUpdate(SourceIdentifier sourceIdentifier) {
        if (sightlyEngineConfiguration.isDevMode()) {
            return true;
        }
        String slyPath = sourceIdentifier.getResource().getPath();
        long javaFileDate = unitChangeMonitor.getLastModifiedDateForJavaSourceFile(sourceIdentifier.getSourceFullPath());
        if (javaFileDate != 0) {
            long slyScriptChangeDate = unitChangeMonitor.getLastModifiedDateForScript(slyPath);
            if (slyScriptChangeDate != 0) {
                if (slyScriptChangeDate < javaFileDate) {
                    return false;
                }
            } else {
                unitChangeMonitor.touchScript(slyPath);
            }
            return true;
        }
        unitChangeMonitor.touchScript(slyPath);
        return true;
    }

    private Resource createResource(ResourceResolver resolver, String path, Map<String, Object> resourceProperties, String intermediateType,
                                    boolean autoCommit, boolean forceOverwrite) throws PersistenceException {
        Resource rsrc = resolver.getResource(path);
        if (rsrc == null || forceOverwrite) {
            final int lastPos = path.lastIndexOf('/');
            final String name = path.substring(lastPos + 1);

            final Resource parentResource;
            if (lastPos == 0) {
                parentResource = resolver.getResource("/");
            } else {
                final String parentPath = path.substring(0, lastPos);
                Map<String, Object> parentProperties = new HashMap<String, Object>();
                parentProperties.put(JCR_PRIMARY_TYPE, intermediateType);
                parentResource = createResource(resolver, parentPath, parentProperties, intermediateType, autoCommit, false);
            }
            if (autoCommit) {
                resolver.refresh();
            }
            if (forceOverwrite) {
                Resource resource = resolver.getResource(parentResource, name);
                if (resource != null) {
                    resolver.delete(resource);
                }
            }
            try {
                rsrc = resolver.create(parentResource, name, resourceProperties);
                if (autoCommit) {
                    resolver.commit();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                }
            } catch (PersistenceException pe) {
                resolver.revert();
                resolver.refresh();
                rsrc = resolver.getResource(parentResource, name);
                if (rsrc == null) {
                    rsrc = resolver.create(parentResource, name, resourceProperties);
                }
            } finally {
                if (autoCommit) {
                    resolver.commit();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                }
            }
        }
        return rsrc;
    }

}

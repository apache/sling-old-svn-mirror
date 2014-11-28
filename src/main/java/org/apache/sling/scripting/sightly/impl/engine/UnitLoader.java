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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Bindings;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.compiled.CompilationOutput;
import org.apache.sling.scripting.sightly.impl.compiled.JavaClassBackend;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyParsingException;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.util.GlobalShadowCheckBackend;
import org.apache.sling.scripting.sightly.impl.engine.compiled.JavaClassTemplate;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.apache.sling.scripting.sightly.impl.engine.runtime.SightlyRenderException;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create rendering units from resources.
 */
@Component
@Service({UnitLoader.class, EventHandler.class})
@Properties({
        @Property(
                name = EventConstants.EVENT_TOPIC,
                value = {SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED}
        ),
        @Property(
                name = EventConstants.EVENT_FILTER,
                value = "(|(" + SlingConstants.PROPERTY_PATH + "=/apps/**/*." + SightlyScriptEngineFactory.EXTENSION + ")(" +
                        SlingConstants.PROPERTY_PATH + "=/libs/**/*." + SightlyScriptEngineFactory.EXTENSION + "))"
        )
})
public class UnitLoader implements EventHandler {

    public static final String DEFAULT_REPO_BASE_PATH = "/var/classes";
    private static final Logger log = LoggerFactory.getLogger(UnitLoader.class);
    private static final String CLASS_NAME_PREFIX = "SightlyJava_";
    private static final String MAIN_TEMPLATE_PATH = "templates/compiled_unit_template.txt";
    private static final String CHILD_TEMPLATE_PATH = "templates/subtemplate.txt";

    private static final String NT_FOLDER = "nt:folder";
    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_LASTMODIFIED = "jcr:lastModified";
    private static final String JCR_ENCODING = "jcr:encoding";

    private String mainTemplate;
    private String childTemplate;
    private Map<String, Long> slyScriptsMap = new ConcurrentHashMap<String, Long>();

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

    private static long getLastModifiedDate(ResourceResolver resolver, String path) {
        try {
            Resource ntResource = getNtResource(resolver, path);
            if (ntResource != null) {
                ValueMap ntResourceProperties = ntResource.adaptTo(ValueMap.class);
                /**
                 * make sure to use 0L for the default value; otherwise we get an Integer
                 * overflow due to the long value stored in JCR
                 */
                return ntResourceProperties.get(JCR_LASTMODIFIED, 0L);
            }
        } catch (Exception e) {
            log.error("Error while reading last modification date: ", e);
        }
        return 0L;
    }

    private static Resource getNtResource(ResourceResolver resolver, String path) {
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            if (path.endsWith(JCR_CONTENT) && resource.isResourceType(NT_RESOURCE)) {
                return resource;
            } else {
                Resource ntResource = resource.getChild(JCR_CONTENT);
                if (ntResource != null && ntResource.isResourceType(NT_RESOURCE)) {
                    return ntResource;
                }
            }
        }
        return null;
    }

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
        ResourceResolver adminResolver = null;
        try {
            SourceIdentifier sourceIdentifier = obtainIdentifier(scriptResource);
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            Object obj;
            ValueMap templateProperties = adminResolver.getResource(scriptResource.getPath()).getChild(JCR_CONTENT).adaptTo(ValueMap.class);
            String encoding = templateProperties.get(JCR_ENCODING, sightlyEngineConfiguration.getEncoding());
            SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
            response.setCharacterEncoding(encoding);
            if (needsUpdate(adminResolver, sourceIdentifier)) {
                synchronized (activeWrites) {
                    String sourceFullPath = sourceIdentifier.getSourceFullPath();
                    lock = activeWrites.get(sourceFullPath);
                    if (lock == null) {
                        lock = new ReentrantLock();
                        activeWrites.put(sourceFullPath, lock);
                    }
                    lock.lock();
                }
                createClass(adminResolver, sourceIdentifier, bindings, encoding, renderContext);
                Resource javaClassResource = adminResolver.getResource(sourceIdentifier.getSourceFullPath());
                obj = sightlyJavaCompilerService.compileSource(javaClassResource, sourceIdentifier.getFullyQualifiedName());
            } else {
                Resource javaClassResource = adminResolver.getResource(sourceIdentifier.getSourceFullPath());
                obj = sightlyJavaCompilerService.getInstance(javaClassResource, sourceIdentifier.getFullyQualifiedName());
            }
            if (!(obj instanceof RenderUnit)) {
                throw new SightlyRenderException("Class is not a RenderUnit instance");
            }
            return (RenderUnit) obj;
        } catch (LoginException e) {
            throw new SightlyRenderException("Unable to create a RenderUnit.", e);
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        String topic = event.getTopic();
        if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(topic) || SlingConstants.TOPIC_RESOURCE_CHANGED.equals(topic)) {
            slyScriptsMap.put(path, Calendar.getInstance().getTimeInMillis());
        } else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
            slyScriptsMap.remove(path);
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

    private synchronized void writeSource(ResourceResolver resolver, String sourceFullPath, String source) {
        try {
            String sourceParentPath = ResourceUtil.getParent(sourceFullPath);
            Map<String, Object> sourceFolderProperties = new HashMap<String, Object>();
            sourceFolderProperties.put(JCR_PRIMARY_TYPE, NT_FOLDER);
            createResource(resolver, sourceParentPath, sourceFolderProperties, NT_FOLDER, true, false);

            Map<String, Object> sourceFileProperties = new HashMap<String, Object>();
            sourceFileProperties.put(JCR_PRIMARY_TYPE, NT_FILE);
            createResource(resolver, sourceFullPath, sourceFileProperties, null, false, false);

            Map<String, Object> ntResourceProperties = new HashMap<String, Object>();
            ntResourceProperties.put(JCR_PRIMARY_TYPE, NT_RESOURCE);
            ntResourceProperties.put(JCR_DATA, new ByteArrayInputStream(source.getBytes()));
            ntResourceProperties.put(JCR_LASTMODIFIED, Calendar.getInstance());
            createResource(resolver, sourceFullPath + "/" + JCR_CONTENT, ntResourceProperties, NT_RESOURCE, true, true);
            log.debug("Successfully written Java source file to repository: {}", sourceFullPath);
        } catch (PersistenceException e) {
            log.error("Repository error while writing Java source file: " + sourceFullPath, e);
        }
    }

    private SourceIdentifier obtainIdentifier(Resource resource) {
        String basePath =
                DEFAULT_REPO_BASE_PATH + "/" + slingSettings.getSlingId() + "/sightly/" + sightlyEngineConfiguration.getEngineVersion();
        return new SourceIdentifier(resource, CLASS_NAME_PREFIX, basePath);
    }

    private void createClass(ResourceResolver resolver, SourceIdentifier identifier, Bindings bindings, String encoding,
                             RenderContextImpl renderContext) {
        String scriptSource = null;
        try {
            Resource scriptResource = resolver.getResource(identifier.getResource().getPath());
            if (scriptResource != null) {
                scriptSource = IOUtils.toString(scriptResource.adaptTo(InputStream.class), encoding);
                String javaSourceCode = obtainResultSource(scriptSource, identifier, bindings, renderContext);
                writeSource(resolver, identifier.getSourceFullPath(), javaSourceCode);
            }
        } catch (SightlyParsingException e) {
            String offendingInput = e.getOffendingInput();
            if (StringUtils.isNotEmpty(offendingInput)) {
                offendingInput = StringEscapeUtils.unescapeHtml(offendingInput.trim());
                int errorLine = getLineWhereErrorOccurred(scriptSource, offendingInput);
                throw new SightlyParsingException("Parsing error in template " + identifier.getResource().getPath() + " at line " +
                        errorLine + ":\n" + offendingInput + "\n");
            } else {
                throw e;
            }

        } catch (IOException e) {
            throw new SightlyRenderException(e);
        }
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

    private boolean needsUpdate(ResourceResolver resolver, SourceIdentifier sourceIdentifier) {
        if (sightlyEngineConfiguration.isDevMode()) {
            return true;
        }
        String javaPath = sourceIdentifier.getSourceFullPath();
        String slyPath = sourceIdentifier.getResource().getPath();
        Long javaFileDate = getLastModifiedDate(resolver, javaPath);
        if (javaFileDate != 0) {

            Long slyScriptChangeDate = slyScriptsMap.get(slyPath);
            if (slyScriptChangeDate != null) {
                if (slyScriptChangeDate < javaFileDate) {
                    return false;
                }
            } else {
                slyScriptsMap.put(slyPath, Calendar.getInstance().getTimeInMillis());
            }
            return true;
        }
        slyScriptsMap.put(slyPath, Calendar.getInstance().getTimeInMillis());
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

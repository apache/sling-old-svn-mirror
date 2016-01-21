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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.script.Bindings;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.compiled.CompilationOutput;
import org.apache.sling.scripting.sightly.impl.compiled.JavaClassBackend;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyParsingException;
import org.apache.sling.scripting.sightly.impl.compiler.UnitChangeMonitor;
import org.apache.sling.scripting.sightly.impl.compiler.util.GlobalShadowCheckBackend;
import org.apache.sling.scripting.sightly.impl.engine.compiled.JavaClassTemplate;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create rendering units from resources.
 */
@Component
@Service(UnitLoader.class)
public class UnitLoader {

    public static final String CLASS_NAME_PREFIX = "SightlyJava_";
    private static final Logger log = LoggerFactory.getLogger(UnitLoader.class);
    private static final String MAIN_TEMPLATE_PATH = "templates/compiled_unit_template.txt";
    private static final String CHILD_TEMPLATE_PATH = "templates/subtemplate.txt";

    private String mainTemplate;
    private String childTemplate;

    @Reference
    private SightlyCompilerService sightlyCompilerService = null;

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService = null;

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    @Reference
    private ClassLoaderWriter classLoaderWriter = null;

    @Reference
    private UnitChangeMonitor unitChangeMonitor = null;

    /**
     * Create a render unit from the given resource
     *
     * @param scriptResource the resource
     * @param renderContext  the rendering context
     * @return the render unit
     * @throws Exception if the unit creation fails
     */
    public RenderUnit createUnit(Resource scriptResource, RenderContext renderContext) throws Exception {
        ResourceResolver adminResolver = renderContext.getScriptResourceResolver();
        SourceIdentifier sourceIdentifier = new SourceIdentifier(sightlyEngineConfiguration, unitChangeMonitor, classLoaderWriter,
            scriptResource, CLASS_NAME_PREFIX);
        Object obj;
        String encoding = unitChangeMonitor.getScriptEncoding(scriptResource.getPath());
        if (sourceIdentifier.needsUpdate()) {
            String sourceCode = getSourceCodeForScript(adminResolver, sourceIdentifier, renderContext.getBindings(), encoding);
            obj = sightlyJavaCompilerService.compileSource(sourceIdentifier, sourceCode, sourceIdentifier.getFullyQualifiedName());
        } else {
            obj = sightlyJavaCompilerService.getInstance(renderContext, sourceIdentifier.getFullyQualifiedName(), false);
        }
        if (!(obj instanceof RenderUnit)) {
            throw new SightlyException("Class is not a RenderUnit instance");
        }
        SlingHttpServletResponse response = (SlingHttpServletResponse) renderContext.getBindings().get(SlingBindings.RESPONSE);
        response.setCharacterEncoding(encoding);
        return (RenderUnit) obj;
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        mainTemplate = resourceFile(componentContext, MAIN_TEMPLATE_PATH);
        childTemplate = resourceFile(componentContext, CHILD_TEMPLATE_PATH);
    }

    private String getSourceCodeForScript(ResourceResolver resolver, SourceIdentifier identifier, Bindings bindings, String encoding) {
        String scriptSource = null;
        try {
            Resource scriptResource = resolver.getResource(identifier.getResource().getPath());
            if (scriptResource != null) {
                scriptSource = IOUtils.toString(scriptResource.adaptTo(InputStream.class), encoding);
                return obtainResultSource(scriptSource, identifier, bindings);
            }
            throw new SightlyException("Cannot find template " + identifier.getResource().getPath() + " in the repository.");
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
            throw new SightlyException("Unable to read the contents of " + identifier.getResource().getPath(), e);
        }
    }

    private String obtainResultSource(String scriptSource, SourceIdentifier identifier, Bindings bindings) {
        JavaClassTemplate classTemplate = newMainTemplate();
        classTemplate.setClassName(identifier.getClassName());
        classTemplate.setPackageName(identifier.getPackageName());
        CompilationOutput compilationOutput = obtainOutput(scriptSource, bindings);
        processCompilationResult(compilationOutput, classTemplate);
        return classTemplate.toString();
    }

    private CompilationOutput obtainOutput(String source, Bindings bindings) {
        JavaClassBackend backend = new JavaClassBackend();
        sightlyCompilerService.compile(source, new GlobalShadowCheckBackend(backend, bindings.keySet()));
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
            inputStream = url.openStream();
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
}

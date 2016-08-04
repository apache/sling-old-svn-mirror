/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.bundlesupport;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adaptables;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.scannotation.AnnotationDB;

/**
 * Build Adapter Metadata from Annotated Classes
 */
@Mojo(name="generate-adapter-metadata", defaultPhase = LifecyclePhase.PROCESS_CLASSES, 
    threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateAdapterMetadataMojo extends AbstractMojo {

    private static final int JSON_INDENTATION = 4;

    private static final String ADAPTABLE_DESC = "L" + Adaptable.class.getName().replace('.', '/') + ";";

    private static final String ADAPTABLES_DESC = "L" + Adaptables.class.getName().replace('.', '/') + ";";

    private static final String DEFAULT_CONDITION = "If the adaptable is a %s.";

    private static String getSimpleName(final ClassNode clazz) {
        final String internalName = clazz.name;
        final int idx = internalName.lastIndexOf('/');
        if (idx == -1) {
            return internalName;
        } else {
            return internalName.substring(idx + 1);
        }
    }

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File buildOutputDirectory;

    /**
     * Name of the generated descriptor file.
     */
    @Parameter(property = "adapter.descriptor.name", defaultValue = "SLING-INF/adapters.json")
    private String fileName;

    @Parameter(defaultValue = "${project.build.directory}/adapter-plugin-generated", required = true, readonly = true)
    private File outputDirectory;

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final JSONObject descriptor = new JSONObject();

            final AnnotationDB annotationDb = new AnnotationDB();
            annotationDb.scanArchives(buildOutputDirectory.toURI().toURL());

            final Set<String> annotatedClassNames = new HashSet<String>();
            addAnnotatedClasses(annotationDb, annotatedClassNames, Adaptable.class);
            addAnnotatedClasses(annotationDb, annotatedClassNames, Adaptables.class);

            for (final String annotatedClassName : annotatedClassNames) {
                getLog().info(String.format("found adaptable annotation on %s", annotatedClassName));
                final String pathToClassFile = annotatedClassName.replace('.', '/') + ".class";
                final File classFile = new File(buildOutputDirectory, pathToClassFile);
                final FileInputStream input = new FileInputStream(classFile);
                final ClassReader classReader;
                try {
                    classReader = new ClassReader(input);
                } finally {
                    input.close();
                }
                final ClassNode classNode = new ClassNode();
                classReader.accept(classNode, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

                @SuppressWarnings("unchecked")
                final List<AnnotationNode> annotations = classNode.invisibleAnnotations;
                for (final AnnotationNode annotation : annotations) {
                    if (ADAPTABLE_DESC.equals(annotation.desc)) {
                        parseAdaptableAnnotation(annotation, classNode, descriptor);
                    } else if (ADAPTABLES_DESC.equals(annotation.desc)) {
                        parseAdaptablesAnnotation(annotation, classNode, descriptor);
                    }
                }

            }

            final File outputFile = new File(outputDirectory, fileName);
            outputFile.getParentFile().mkdirs();
            final FileWriter writer = new FileWriter(outputFile);
            try {
                IOUtil.copy(descriptor.toString(JSON_INDENTATION), writer);
            } finally {
                IOUtil.close(writer);
            }
            addResource();

        } catch (IOException e) {
            throw new MojoExecutionException("Unable to generate metadata", e);
        } catch (JSONException e) {
            throw new MojoExecutionException("Unable to generate metadata", e);
        }

    }

    private void addAnnotatedClasses(final AnnotationDB annotationDb, final Set<String> annotatedClassNames, final Class<? extends Annotation> clazz) {
        Set<String> classNames = annotationDb.getAnnotationIndex().get(clazz.getName());
        if (classNames == null || classNames.isEmpty()) {
            getLog().debug("No classes found with adaptable annotations.");
        } else {
            annotatedClassNames.addAll(classNames);
        }
    }

    private void addResource() {
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        @SuppressWarnings("unchecked")
        final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
        while (!found && rsrcIterator.hasNext()) {
            final Resource rsrc = rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if (!found) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }

    }

    private void parseAdaptablesAnnotation(final AnnotationNode annotation, final ClassNode classNode,
            final JSONObject descriptor) throws JSONException {
        final Iterator<?> it = annotation.values.iterator();
        while (it.hasNext()) {
            Object name = it.next();
            Object value = it.next();
            if ("value".equals(name)) {
                @SuppressWarnings("unchecked")
                final List<AnnotationNode> annotations = (List<AnnotationNode>) value;
                for (final AnnotationNode innerAnnotation : annotations) {
                    if (ADAPTABLE_DESC.equals(innerAnnotation.desc)) {
                        parseAdaptableAnnotation(innerAnnotation, classNode, descriptor);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAdaptableAnnotation(final AnnotationNode annotation, final ClassNode annotatedClass,
            final JSONObject descriptor) throws JSONException {
        String adaptableClassName = null;
        List<AnnotationNode> adapters = null;

        final List<?> values = annotation.values;

        final Iterator<?> it = values.iterator();
        while (it.hasNext()) {
            Object name = it.next();
            Object value = it.next();

            if ("adaptableClass".equals(name)) {
                adaptableClassName = ((Type) value).getClassName();
            } else if ("adapters".equals(name)) {
                adapters = (List<AnnotationNode>) value;
            }
        }

        if (adaptableClassName == null || adapters == null) {
            throw new IllegalArgumentException(
                    "Adaptable annotation is malformed. Expecting a classname and a list of adapter annotation.");
        }

        JSONObject adaptableDescription;
        if (descriptor.has(adaptableClassName)) {
            adaptableDescription = descriptor.getJSONObject(adaptableClassName);
        } else {
            adaptableDescription = new JSONObject();
            descriptor.put(adaptableClassName, adaptableDescription);
        }

        for (final AnnotationNode adapter : adapters) {
            parseAdapterAnnotation(adapter, annotatedClass, adaptableDescription);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAdapterAnnotation(final AnnotationNode annotation, final ClassNode annotatedClass,
            final JSONObject adaptableDescription) throws JSONException {
        String condition = null;
        List<Type> adapterClasses = null;

        final List<?> values = annotation.values;

        final Iterator<?> it = values.iterator();
        while (it.hasNext()) {
            final Object name = it.next();
            final Object value = it.next();

            if (StringUtils.isEmpty(condition)) {
                condition = String.format(DEFAULT_CONDITION, getSimpleName(annotatedClass));
            }

            if ("condition".equals(name)) {
                condition = (String) value;
            } else if ("value".equals(name)) {
                adapterClasses = (List<Type>) value;
            }
        }

        if (adapterClasses == null) {
            throw new IllegalArgumentException("Adapter annotation is malformed. Expecting a list of adapter classes");
        }
        
        for (final Type adapterClass : adapterClasses) {
            adaptableDescription.accumulate(condition, adapterClass.getClassName());
        }
    }
}

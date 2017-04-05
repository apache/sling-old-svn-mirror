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
package org.apache.sling.maven.jcrocm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * The <code>JcrOcmMojo</code> implements the (default) ocm goal of the
 * <em>maven-jcrocm-plugin</em>. It is by default run in the
 * <code>generate-resources</code> phase and requires the compile scoped
 * dependencies to be resolved.
 */
@Mojo(name = "ocm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class JcrOcmMojo extends AbstractMojo {

    @Parameter ( defaultValue = "${project.build.directory}/sling-generated", readonly = true)
    private File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Name and path of the generated descriptor.
     */
    @Parameter( property = "jcrocm.descriptor.name", defaultValue = "mappings.xml")
    private String finalName;

    public void execute() throws MojoFailureException {

        boolean hasFailures = false;

        // prepare QDox and prime with the compile class path of the project
        JavaDocBuilder builder = new JavaDocBuilder();
        try {
            builder.getClassLibrary().addClassLoader(this.getCompileClassLoader());
        } catch (IOException ioe) {
            throw new MojoFailureException("Cannot prepare QDox");
        }

        // add the sources from the project
        for (Iterator i = this.project.getCompileSourceRoots().iterator(); i.hasNext();) {
            try {
                builder.addSourceTree(new File((String) i.next()));
            } catch (OutOfMemoryError oome) {
                // this may be the case for big sources and not enough VM mem
                builder = null; // drop the builder to help GC now

                // fail with some explanation
                throw new MojoFailureException(
                    "Failed analyzing source due to not enough memory, try setting Max Heap Size higher, e.g. using MAVEN_OPTS=-Xmx128m");
            }
        }

        // parse the sources and get them
        JavaSource[] javaSources = builder.getSources();
        List descriptors = new ArrayList();
        for (int i = 0; i < javaSources.length; i++) {
            JavaClass[] javaClasses = javaSources[i].getClasses();
            for (int j = 0; javaClasses != null && j < javaClasses.length; j++) {
                DocletTag tag = javaClasses[j].getTagByName(ClassDescriptor.TAG_CLASS_DESCRIPTOR);
                if (tag != null) {
                    ClassDescriptor descriptor = this.createClassDescriptor(javaClasses[j]);
                    if (descriptor != null) {
                        descriptors.add(descriptor);
                    } else {
                        hasFailures = true;
                    }
                }
            }
        }

        // after checking all classes, throw if there were any failures
        if (hasFailures) {
            throw new MojoFailureException("Jackrabbit OCM Descriptor parsing had failures (see log)");
        }

        // terminate if there is nothing to write
        if (descriptors.isEmpty()) {
            this.getLog().info("No Jackrabbit OCM Descriptors found in project");
            return;
        }

        // finally the descriptors have to be written ....
        if (StringUtils.isEmpty(this.finalName)) {
            this.getLog().error("Descriptor file name must not be empty");
            return;
        }

        // prepare the descriptor output file
        File descriptorFile = new File(new File(this.outputDirectory, "SLING-INF"), this.finalName);
        descriptorFile.getParentFile().mkdirs(); // ensure parent dir

        this.getLog().info("Generating " + descriptors.size()
            + " OCM Mapping Descriptors to " + descriptorFile);

        // write out all the class descriptors in parse order
        FileOutputStream descriptorStream = null;
        XMLWriter xw = null;
        try {
            descriptorStream = new FileOutputStream(descriptorFile);
            xw = new XMLWriter(descriptorStream, false);
            xw.printElementStart("jackrabbit-ocm", false);
            for (Iterator di=descriptors.iterator(); di.hasNext(); ) {
                ClassDescriptor sd = (ClassDescriptor) di.next();
                sd.generate(xw);
            }
            xw.printElementEnd("jackrabbit-ocm");

        } catch (IOException ioe) {
            hasFailures = true;
            this.getLog().error("Cannot write descriptor to " + descriptorFile, ioe);
            throw new MojoFailureException("Failed to write descriptor to " + descriptorFile);
        } finally {
            IOUtil.close(xw);
            IOUtil.close(descriptorStream);

            // remove the descriptor file in case of write failure
            if (hasFailures) {
                descriptorFile.delete();
            }
        }

        // now add the descriptor file to the maven resources
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        final Iterator rsrcIterator = this.project.getResources().iterator();
        while ( !found && rsrcIterator.hasNext() ) {
            final Resource rsrc = (Resource)rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if ( !found ) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }
        // and set include accordingly
        this.project.getProperties().setProperty("Sling-Mappings", "SLING-INF/" + this.finalName);
    }

    /**
     * Creates an URL class loader containing all compile artifacts.
     *
     * @return The URLClassLoader for the compile artifacts.
     * @throws IOException If any error occurrs creating URLs for the compile
     *             artifact files.
     */
    private ClassLoader getCompileClassLoader() throws IOException {
        List artifacts = this.project.getCompileArtifacts();
        URL[] path = new URL[artifacts.size()];
        int i = 0;
        for (Iterator ai=artifacts.iterator(); ai.hasNext(); ) {
            Artifact a = (Artifact) ai.next();
            path[i++] = a.getFile().toURI().toURL();
        }
        return new URLClassLoader(path);
    }

    private ClassDescriptor createClassDescriptor(JavaClass javaClass) {

        // create the class descriptor for the java class, return early if none
        ClassDescriptor cd = ClassDescriptor.fromClass(this.getLog(), javaClass);
        if (cd == null) {
            return null;
        }

        // analyze fields for mapping descriptors
        JavaField[] fields = javaClass.getFields();
        for (int i=0; fields != null && i < fields.length; i++) {
            cd.addChild(FieldDescriptor.fromField(this.getLog(), fields[i]));
            cd.addChild(BeanDescriptor.fromField(this.getLog(), fields[i]));
            cd.addChild(CollectionDescriptor.fromField(this.getLog(), fields[i]));
        }

        // analyze methods for mapping descriptors
        JavaMethod[] methods = javaClass.getMethods();
        for (int i=0; methods != null && i < methods.length; i++) {
            cd.addChild(FieldDescriptor.fromMethod(this.getLog(), methods[i]));
            cd.addChild(BeanDescriptor.fromMethod(this.getLog(), methods[i]));
            cd.addChild(CollectionDescriptor.fromMethod(this.getLog(), methods[i]));
        }

        // return nothing if validation fails
        return cd.validate() ? cd : null;
    }
}

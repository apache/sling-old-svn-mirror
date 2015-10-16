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
package org.apache.sling.ide.eclipse.m2e;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.installer.DefaultArtifactInstaller;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.sling.ide.eclipse.m2e.internal.Activator;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;

@SuppressWarnings("restriction")
public class EmbeddedArchetypeInstaller {

	private final String groupId;
	private final String artifactId;
	private final String version;

	private final Map<String,InputStream> origins = new HashMap<>();
	
	public EmbeddedArchetypeInstaller(final String groupId,
			final String artifactId,
			final String version) {
		if (groupId==null || groupId.length()==0) {
			throw new IllegalArgumentException("groupId must not be empty");
		}
		if (artifactId==null || artifactId.length()==0) {
			throw new IllegalArgumentException("artifactId must not be empty");
		}
		if (version==null || version.length()==0) {
			throw new IllegalArgumentException("version must not be empty");
		}
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	
	public void addResource(String fileExtension, URL origin) throws IOException {
	    if (origin==null) {
	        throw new IllegalArgumentException("origin must not be null");
	    }
		origins.put(fileExtension, origin.openStream());
	}

    public void addResource(String fileExtension, InputStream inputStream) throws IOException {
        origins.put(fileExtension, inputStream);
    }
	
	public void addResource(String fileExtension, File origin) throws FileNotFoundException {
		origins.put(fileExtension, new FileInputStream(origin));
	}
	
    public void installArchetype() throws CoreException {
        try {
            IMaven maven = MavenPlugin.getMaven();
            // first get the plexus container
            PlexusContainer container = ((MavenImpl) MavenPlugin.getMaven()).getPlexusContainer();

            // then get the DefaultMaven
            DefaultMaven mvn = (DefaultMaven) container.lookup(Maven.class);

            // now create a RepositorySystemSession
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            request.setLocalRepository(maven.getLocalRepository());

            // We need to support Maven 3.0.x as well, so we use reflection to
            // access Aether APIs in a manner which is compatible with all Maven 3.x versions
            // See https://maven.apache.org/docs/3.1.0/release-notes.html
            MavenSession session = reflectiveCreateMavenSession(container, mvn, request);
            LegacySupport legacy = container.lookup(LegacySupport.class);
            legacy.setSession(session);

            // then lookup the DefaultArtifactInstaller
            DefaultArtifactInstaller dai = (DefaultArtifactInstaller) container.lookup(ArtifactInstaller.class);

            final Set<Entry<String, InputStream>> entries = origins.entrySet();
            for (Iterator<Entry<String, InputStream>> it = entries.iterator(); it.hasNext();) {
                final Entry<String, InputStream> entry = it.next();
                final String fileExtension = entry.getKey();
                File tmpFile = File.createTempFile("slingClipseTmp", fileExtension);
                

                try (InputStream in = entry.getValue(); 
                        FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    IOUtils.copy(in, fos);
                    // the below code uses the fileExtension as a type. Most of the time this is correct
                    // and should be fine for our usage
                    Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, version, "", fileExtension, "",
                            new DefaultArtifactHandler(fileExtension));
                    dai.install(tmpFile, jarArtifact, maven.getLocalRepository());
                } finally {
                    FileUtils.deleteQuietly(tmpFile);
                }
            }

            Archetype archetype = new Archetype();
            archetype.setGroupId(groupId);
            archetype.setArtifactId(artifactId);
            archetype.setVersion(version);
            org.apache.maven.archetype.Archetype archetyper = MavenPluginActivator.getDefault().getArchetype();
            archetyper.updateLocalCatalog(archetype);
        } catch (CoreException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
	}

    private MavenSession reflectiveCreateMavenSession(PlexusContainer container, DefaultMaven mvn, MavenExecutionRequest request)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Method newRepoSessionMethod = null;
        for (Method m : mvn.getClass().getMethods()) {
            if ("newRepositorySession".equals(m.getName())) {
                newRepoSessionMethod = m;
                break;
            }
        }

        if (newRepoSessionMethod == null) {
            throw new IllegalArgumentException("No 'newRepositorySession' method found on object " + mvn + " of type "
                    + mvn.getClass().getName());
        }

        Object repositorySession = newRepoSessionMethod.invoke(mvn, request);

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        Constructor<?> constructor = null;

        outer: for (Constructor<?> c : MavenSession.class.getConstructors()) {

            for (Class<?> klazz : getClasses(repositorySession)) {
                Class<?>[] check = new Class<?>[] { PlexusContainer.class, klazz, MavenExecutionRequest.class,
                        MavenExecutionResult.class };

                if (Arrays.equals(c.getParameterTypes(), check)) {
                    constructor = c;
                    break outer;
                }
            }
        }

        if (constructor == null) {
            throw new IllegalArgumentException("Unable to found matching MavenSession constructor");
        }

        return (MavenSession) constructor.newInstance(container, repositorySession, request, result);
    }

    private Class<?>[] getClasses(Object repositorySession) {

        List<Class<?>> accu = new ArrayList<>();
        Class<? extends Object> klazz = repositorySession.getClass();

        getClasses(klazz, accu);
        if (klazz.getSuperclass() != null) {
            getClasses(klazz.getSuperclass(), accu);
        }

        return accu.toArray(new Class<?>[accu.size()]);
    }

    private void getClasses(Class<? extends Object> klazz, List<Class<?>> accu) {
        accu.add(klazz);
        for (Class<?> iface : klazz.getInterfaces()) {
            accu.add(iface);
        }
    }
}

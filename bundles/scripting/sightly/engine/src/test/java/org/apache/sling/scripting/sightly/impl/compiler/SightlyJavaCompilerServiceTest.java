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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.apache.sling.scripting.sightly.impl.engine.ResourceBackedPojoChangeMonitor;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.impl.engine.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SightlyJavaCompilerServiceTest {

    private SightlyJavaCompilerService compiler;
    private ResourceBackedPojoChangeMonitor resourceBackedPojoChangeMonitor;
    private ClassLoaderWriter classLoaderWriter;
    private ScriptingResourceResolverProvider scriptingResourceResolverProvider;

    @Before
    public void setUp() throws Exception {
        compiler = spy(new SightlyJavaCompilerService());
        resourceBackedPojoChangeMonitor = spy(new ResourceBackedPojoChangeMonitor());
        SightlyEngineConfiguration sightlyEngineConfiguration = mock(SightlyEngineConfiguration.class);
        when(sightlyEngineConfiguration.getBundleSymbolicName()).thenReturn("org.apache.sling.scripting.sightly");
        when(sightlyEngineConfiguration.getScratchFolder()).thenReturn("/org/apache/sling/scripting/sightly");
        Whitebox.setInternalState(compiler, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        Whitebox.setInternalState(compiler, "resourceBackedPojoChangeMonitor", resourceBackedPojoChangeMonitor);
        classLoaderWriter = Mockito.mock(ClassLoaderWriter.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        when(classLoaderWriter.getClassLoader()).thenReturn(classLoader);
    }

    @After
    public void tearDown() throws Exception {
        compiler = null;
        resourceBackedPojoChangeMonitor = null;
        classLoaderWriter = null;
    }

    @Test
    public void testDiskCachedUseObject() throws Exception {
        String pojoPath = "/apps/myproject/testcomponents/a/Pojo.java";
        String className = "apps.myproject.testcomponents.a.Pojo";
        scriptingResourceResolverProvider = Mockito.mock(ScriptingResourceResolverProvider.class);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(scriptingResourceResolverProvider.getRequestScopedResourceResolver()).thenReturn(resolver);
        Resource pojoResource = Mockito.mock(Resource.class);
        when(pojoResource.getPath()).thenReturn(pojoPath);
        ResourceMetadata mockMetadata = Mockito.mock(ResourceMetadata.class);
        when(mockMetadata.getModificationTime()).thenReturn(1l);
        when(pojoResource.getResourceMetadata()).thenReturn(mockMetadata);
        when(pojoResource.adaptTo(InputStream.class)).thenReturn(IOUtils.toInputStream("DUMMY", "UTF-8"));
        when(resolver.getResource(pojoPath)).thenReturn(pojoResource);
        when(classLoaderWriter.getLastModified("/apps/myproject/testcomponents/a/Pojo.class")).thenReturn(2l);
        getInstancePojoTest(className);
        /*
         * assuming the compiled class has a last modified date greater than the source, then the compiler should not recompile the Use
         * object
         */
        verify(compiler, never()).compileSource(any(SourceIdentifier.class), anyString());
    }

    @Test
    public void testObsoleteDiskCachedUseObject() throws Exception {
        String pojoPath = "/apps/myproject/testcomponents/a/Pojo.java";
        String className = "apps.myproject.testcomponents.a.Pojo";
        scriptingResourceResolverProvider = Mockito.mock(ScriptingResourceResolverProvider.class);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(scriptingResourceResolverProvider.getRequestScopedResourceResolver()).thenReturn(resolver);
        Resource pojoResource = Mockito.mock(Resource.class);
        when(pojoResource.getPath()).thenReturn(pojoPath);
        ResourceMetadata mockMetadata = Mockito.mock(ResourceMetadata.class);
        when(mockMetadata.getModificationTime()).thenReturn(2l);
        when(pojoResource.getResourceMetadata()).thenReturn(mockMetadata);
        when(pojoResource.adaptTo(InputStream.class)).thenReturn(IOUtils.toInputStream("DUMMY", "UTF-8"));
        when(resolver.getResource(pojoPath)).thenReturn(pojoResource);
        when(classLoaderWriter.getLastModified("/apps/myproject/testcomponents/a/Pojo.class")).thenReturn(1l);
        getInstancePojoTest(className);
        /*
         * assuming the compiled class has a last modified date greater than the source, then the compiler should not recompile the Use
         * object
         */
        verify(compiler).compileSource(any(SourceIdentifier.class), anyString());
    }

    @Test
    public void testMemoryCachedUseObject() throws Exception {
        String pojoPath = "/apps/myproject/testcomponents/a/Pojo.java";
        String className = "apps.myproject.testcomponents.a.Pojo";
        scriptingResourceResolverProvider = Mockito.mock(ScriptingResourceResolverProvider.class);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(scriptingResourceResolverProvider.getRequestScopedResourceResolver()).thenReturn(resolver);
        Resource pojoResource = Mockito.mock(Resource.class);
        when(pojoResource.getPath()).thenReturn(pojoPath);
        when(resourceBackedPojoChangeMonitor.getLastModifiedDateForJavaUseObject(pojoPath)).thenReturn(1l);
        when(pojoResource.adaptTo(InputStream.class)).thenReturn(IOUtils.toInputStream("DUMMY", "UTF-8"));
        when(resolver.getResource(pojoPath)).thenReturn(pojoResource);
        when(classLoaderWriter.getLastModified("/apps/myproject/testcomponents/a/Pojo.class")).thenReturn(2l);
        getInstancePojoTest(className);
        /*
         * assuming the compiled class has a last modified date greater than the source, then the compiler should not recompile the Use
         * object
         */
        verify(compiler, never()).compileSource(any(SourceIdentifier.class), anyString());
    }

    @Test
    public void testObsoleteMemoryCachedUseObject() throws Exception {
        String pojoPath = "/apps/myproject/testcomponents/a/Pojo.java";
        String className = "apps.myproject.testcomponents.a.Pojo";
        scriptingResourceResolverProvider = Mockito.mock(ScriptingResourceResolverProvider.class);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(scriptingResourceResolverProvider.getRequestScopedResourceResolver()).thenReturn(resolver);
        Resource pojoResource = Mockito.mock(Resource.class);
        when(pojoResource.getPath()).thenReturn(pojoPath);
        when(resourceBackedPojoChangeMonitor.getLastModifiedDateForJavaUseObject(pojoPath)).thenReturn(2l);
        when(pojoResource.adaptTo(InputStream.class)).thenReturn(IOUtils.toInputStream("DUMMY", "UTF-8"));
        when(resolver.getResource(pojoPath)).thenReturn(pojoResource);
        when(classLoaderWriter.getLastModified("/apps/myproject/testcomponents/a/Pojo.class")).thenReturn(1l);
        getInstancePojoTest(className);
        /*
         * assuming the compiled class has a last modified date greater than the source, then the compiler should not recompile the Use
         * object
         */
        verify(compiler).compileSource(any(SourceIdentifier.class), anyString());
    }

    private void getInstancePojoTest(String className) throws Exception {
        RenderContextImpl renderContext = Mockito.mock(RenderContextImpl.class);


        JavaCompiler javaCompiler = Mockito.mock(JavaCompiler.class);
        CompilationResult compilationResult = Mockito.mock(CompilationResult.class);
        when(compilationResult.getErrors()).thenReturn(new ArrayList<CompilerMessage>());
        when(javaCompiler.compile(Mockito.any(CompilationUnit[].class), Mockito.any(Options.class))).thenReturn(compilationResult);
        when(classLoaderWriter.getClassLoader().loadClass(className)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return MockPojo.class;
            }
        });
        Whitebox.setInternalState(compiler, "classLoaderWriter", classLoaderWriter);
        Whitebox.setInternalState(compiler, "javaCompiler", javaCompiler);
        Whitebox.setInternalState(compiler, "scriptingResourceResolverProvider", scriptingResourceResolverProvider);
        Object obj = compiler.getResourceBackedUseObject(renderContext, className);
        assertTrue("Expected to obtain a " + MockPojo.class.getName() + " object.", obj instanceof MockPojo);
    }
}

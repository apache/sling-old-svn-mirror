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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SightlyJavaCompilerServiceTest {

    private SightlyJavaCompilerService compiler;
    private UnitChangeMonitor ucm;

    @Before
    public void setUp() throws Exception {
        compiler = new SightlyJavaCompilerService();
        ucm = spy(new UnitChangeMonitor());
        SightlyEngineConfiguration sightlyEngineConfiguration = mock(SightlyEngineConfiguration.class);
        when(sightlyEngineConfiguration.isDevMode()).thenReturn(false);
        Whitebox.setInternalState(compiler, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        Whitebox.setInternalState(compiler, "unitChangeMonitor", ucm);
    }

    @After
    public void tearDown() throws Exception {
        compiler = null;
        ucm = null;
    }

    @Test
    /**
     * Tests that class names whose packages contain underscores are correctly expanded to JCR paths containing symbols that might be
     * replaced with an underscores in order to obey Java naming conventions.
     */
    public void testGetInstanceForPojoFromRepoWithAmbigousPath() throws Exception {
        String pojoPath = "/apps/my-project/test_components/a/Pojo.java";
        String className = "apps.my_project.test_components.a.Pojo";
        getInstancePojoTest(pojoPath, className);
    }

    @Test
    /**
     * Tests that class names whose package names don't contain underscores are correctly expanded to JCR paths.
     */
    public void testGetInstanceForPojoFromRepo() throws Exception {
        String pojoPath = "/apps/myproject/testcomponents/a/Pojo.java";
        String className = "apps.myproject.testcomponents.a.Pojo";
        getInstancePojoTest(pojoPath, className);
    }

    @Test
    public void testGetInstanceForCachedPojoFromRepo() throws Exception {
        final String pojoPath = "/apps/my-project/test_components/a/Pojo.java";
        final String className = "apps.my_project.test_components.a.Pojo";
        Map<String, Long> slyJavaUseMap = new ConcurrentHashMap<String, Long>() {{
            put(className, System.currentTimeMillis());
        }};
        Whitebox.setInternalState(ucm, "slyJavaUseMap", slyJavaUseMap);
        getInstancePojoTest(pojoPath, className);
        verify(ucm).clearJavaUseObject(className);
    }

    private void getInstancePojoTest(String pojoPath, String className) throws Exception {
        RenderContext renderContext = Mockito.mock(RenderContext.class);
        Resource pojoResource = Mockito.mock(Resource.class);
        when(pojoResource.getPath()).thenReturn(pojoPath);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(renderContext.getScriptResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(pojoPath)).thenReturn(pojoResource);
        when(pojoResource.adaptTo(InputStream.class)).thenReturn(IOUtils.toInputStream("DUMMY"));
        JavaCompiler javaCompiler = Mockito.mock(JavaCompiler.class);
        CompilationResult compilationResult = Mockito.mock(CompilationResult.class);
        when(compilationResult.getErrors()).thenReturn(new ArrayList<CompilerMessage>());
        when(javaCompiler.compile(Mockito.any(CompilationUnit[].class), Mockito.any(Options.class))).thenReturn(compilationResult);
        ClassLoaderWriter clw = Mockito.mock(ClassLoaderWriter.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        when(clw.getClassLoader()).thenReturn(classLoader);
        when(classLoader.loadClass(className)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return MockPojo.class;
            }
        });
        Whitebox.setInternalState(compiler, "classLoaderWriter", clw);
        Whitebox.setInternalState(compiler, "javaCompiler", javaCompiler);
        SightlyEngineConfiguration sightlyEngineConfiguration = mock(SightlyEngineConfiguration.class);
        when(sightlyEngineConfiguration.getBundleSymbolicName()).thenReturn("org.apache.sling.scripting.sightly");
        when(sightlyEngineConfiguration.getScratchFolder()).thenReturn("/org/apache/sling/scripting/sightly");
        Whitebox.setInternalState(compiler, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        Object obj = compiler.getInstance(renderContext, className);
        assertTrue("Expected to obtain a " + MockPojo.class.getName() + " object.", obj instanceof MockPojo);
    }
}

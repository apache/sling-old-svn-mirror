/*
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
 */
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link FileVaultContentSerializer}
 */
public class FileVaultContentSerializerTest {

    private MockHelper helper;
    private ResourceResolver resourceResolver;

    @Before
    public void setUp() throws Exception {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);
        helper = MockHelper.create(resourceResolver).resource("/libs").p("prop", "value")
                .resource("sub").p("sub", "hello")
                .resource(".sameLevel")
                .resource("/apps").p("foo", "baa");
        helper.commit();
    }

    @Test
    public void testExportToStream() throws Exception {
        Packaging packaging = mock(Packaging.class);

        ImportMode importMode = ImportMode.REPLACE;
        AccessControlHandling aclHandling = AccessControlHandling.IGNORE;

        String[] packageRoots = new String[]{"/etc/packages"};
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        boolean useReferences = false;
        int threshold = 1024;
        FileVaultContentSerializer fileVaultContentSerializer = new FileVaultContentSerializer("vlt", packaging, importMode,
                aclHandling, packageRoots, nodeFilters, propertyFilters, useReferences, threshold);

        ResourceResolver sessionResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        PackageManager pm = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(pm);
        OutputStream outputStream = new ByteArrayOutputStream();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                return null;
            }
        }).when(pm).assemble(same(session), any(ExportOptions.class), same(outputStream));

        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(sessionResolver.adaptTo(Session.class)).thenReturn(session);
        DistributionExportFilter filter = mock(DistributionExportFilter.class);
        DistributionRequest request = mock(DistributionRequest.class);
        when(request.getPaths()).thenReturn(new String[]{"/libs"});
        when(request.getFilters("/libs")).thenReturn(new String[0]);
        DistributionExportOptions exportOptions = new DistributionExportOptions(request, filter);

        fileVaultContentSerializer.exportToStream(sessionResolver, exportOptions, outputStream);
    }

    @Test
    public void testImportFromStream() throws Exception {
        Packaging packaging = mock(Packaging.class);
        ImportMode importMode = ImportMode.REPLACE;
        AccessControlHandling aclHandling = AccessControlHandling.IGNORE;

        String[] packageRoots = new String[]{"/"};
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        boolean useReferences = false;
        int thershold = 1024;
        FileVaultContentSerializer fileVaultContentSerializer = new FileVaultContentSerializer("vlt", packaging, importMode,
                aclHandling, packageRoots, nodeFilters, propertyFilters, useReferences, thershold);

        ResourceResolver sessionResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        File file = new File(getClass().getResource("/vlt/dp.vlt").getFile());

        PackageManager pm = mock(PackageManager.class);
        VaultPackage vaultPackage = mock(VaultPackage.class);
        when(pm.open(any(File.class))).thenReturn(vaultPackage);
        when(packaging.getPackageManager()).thenReturn(pm);

        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(sessionResolver.adaptTo(Session.class)).thenReturn(session);

        fileVaultContentSerializer.importFromStream(sessionResolver, new FileInputStream(file));
    }
}
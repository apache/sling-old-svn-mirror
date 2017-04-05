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
package org.apache.sling.installer.core.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.apache.sling.installer.api.InstallableResource;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/** Partial tests of the OsgiInstallerImpl. A number of things
 *  are tested in the "it" module.
 */
public class OsgiInstallerImplTest {
    private OsgiInstallerImpl installer;
    private File storageDir;
    private static final String SCHEME = OsgiInstallerImplTest.class.getSimpleName();
    
    private static final Random random = new Random();
    
    private static final String A = "a-" + random.nextInt();
    private static final String B = "b-" + random.nextInt();
    private static final String C = "c-" + random.nextInt();
    private static final String D = "d-" + random.nextInt();
    
    private InstallableResource mockBundle(String id) {
        final InputStream data = new ByteArrayInputStream(id.getBytes());
        final String digest = id + id.length();
        final int priority = 10;
        final String type = InstallableResource.TYPE_FILE;
        return new InstallableResource(id, data, null, digest, type, priority);
    }
    
    private InstallableResource [] mockBundles(String ... ids) {
        final InstallableResource [] result = new InstallableResource[ids.length];
        for(int i=0; i < ids.length; i++) {
            result[i] = mockBundle(ids[i]);
        }
        return result;
    }
    
    private void assertDataFiles(String ... ids) {
        final List<String> dataFileNames = new ArrayList<>();
        
        final String [] filenames = storageDir.list();
        if(filenames != null) {
            dataFileNames.addAll(Arrays.asList(filenames));
        }
        
        final List<String> notFound = new ArrayList<>(Arrays.asList(ids));
        for(String id : ids) {
            final Iterator<String> it = dataFileNames.iterator();
            while(it.hasNext()) {
                final String filename = it.next();
                
                // This assumes FileDataStore creates files that
                // contain the resource ID in their filename, and
                // that our IDs are sufficiently unique. Works for
                // this test but somewhat hacky.
                if(filename.contains(id)) {
                    notFound.remove(id);
                    it.remove();
                    break;
                }
            }
        }
        
        
        if(!notFound.isEmpty()) {
            fail("Some expected data files were not found:" + notFound);
        }
        
        if(!dataFileNames.isEmpty()) {
            fail("Extra data files found: " + dataFileNames);
        }
    }
    
    @Before
    public void setup() {
        final BundleContext ctx = new MockBundleContext();
        installer = new OsgiInstallerImpl(ctx);
        
        // storageDir points to the folder used by FileDataStore
        // to store the private files that it creates, so
        // that we can check them.
        final FileDataStore fds = new FileDataStore(ctx);
        final File testFile = fds.getDataFile("f00");
        storageDir = testFile.getParentFile();
        
        // Cleanup storage dir so that we can check
        // exactly which files are created there
        for(String f : storageDir.list()) {
            new File(storageDir, f).delete();
        }
    }
    
    @Test
    public void testDataFiles() {
        // The installer creates data files for all installed bundles,
        // and deletes them when they are not needed anymore.
        // This verifies the create/delete logic
        
        installer.registerResources(SCHEME, mockBundles(A, B));
        assertDataFiles(A, B);
        
        installer.updateResources(SCHEME, mockBundles(A, C), null);
        assertDataFiles(A, B, C);
        
        installer.registerResources(SCHEME, mockBundles(C, D));
        // TODO B should be gone...not critical but suboptimal,
        // we might need to review this private files logic more broadly
        assertDataFiles(B, C, D);
    }
}
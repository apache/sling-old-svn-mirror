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
package org.apache.sling.installer.core.impl.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class RefreshDependenciesUtilTest {
    
    private Mockery jmock;
    private Bundle target;
    private Bundle A;
    private Bundle B;
    private Bundle C;
    private Bundle D;
    private Bundle E;
    private Bundle F;
    private PackageAdmin pa;
    private RefreshDependenciesUtil rdu;
    private long counter = 1;
    
    private final Map<String, List<Bundle>> importingBundles = new HashMap<String, List<Bundle>>();
    
    private Bundle setupBundle(String mockName, String importPackages, final String exportsPackages) {
        final Bundle result = jmock.mock(Bundle.class, mockName);
        
        jmock.checking(new Expectations() {{
            allowing(result).getBundleId();
            will(returnValue(counter++));
        }});
        
        if(importPackages != null) {
            for(String pack : importPackages.split(";")) {
                List<Bundle> list = importingBundles.get(pack);
                if(list == null) {
                    list = new ArrayList<Bundle>();
                    importingBundles.put(pack, list);
                }
                list.add(result);
            }
        }

        final List<ExportedPackage> eps = new ArrayList<ExportedPackage>();
        if(exportsPackages != null) {
            for(final String pack : exportsPackages.split(";")) {
                final ExportedPackage ep = jmock.mock(ExportedPackage.class, "ExportedPackage." + pack + "." + mockName);
                eps.add(ep);
                jmock.checking(new Expectations() {{
                    allowing(ep).getImportingBundles();
                    will(returnValue(getImportingBundles(pack)));
                    allowing(ep).getName();
                    will(returnValue(pack));
                }});
            }
        }
            
        jmock.checking(new Expectations() {{
            allowing(pa).getExportedPackages(result);
            will(returnValue(eps.toArray(new ExportedPackage[]{})));
        }});
        
        return result;
    }
    
    private Bundle [] getImportingBundles(String packageName) {
        final List<Bundle> list = importingBundles.get(packageName);
        if(list == null) {
            return null;
        } else {
            return list.toArray(new Bundle[] {});
        }
    }
    
    @Before
    public void setup() {
        jmock = new Mockery();
        pa = jmock.mock(PackageAdmin.class);
        rdu = new RefreshDependenciesUtil(pa);
        
        // Test bundle depends on A directly and does not depend on B
        target = setupBundle("testBundle", "com.targetImportsOne;com.targetImportsTwo", null);
        A = setupBundle("A", null, "com.targetImportsOne");
        B = setupBundle("B", "some.import", "some.export");
        
        // Test bundle depends on C which in turns depends on D
        C = setupBundle("C", "com.CimportsOne", "com.targetImportsTwo");
        D = setupBundle("D", null, "com.CimportsOne");
        E = setupBundle("E", null, null);
        
        // F imports and exports the same packages
        F = setupBundle("F", "foo", "foo");
    }
    
    @Test
    public void testTargetDependsOnSelf() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(F);
        assertTrue(rdu.isBundleAffected(F, bundles));
    }
        
    @Test
    public void testTargetDependsOnBundleA() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(A);
        assertTrue(rdu.isBundleAffected(target, bundles));
    }
        
    @Test
    public void testTargetDoesNotDependOnBundleB() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(B);
        assertFalse(rdu.isBundleAffected(target, bundles));
    }
    
    @Test
    public void testTargetDoesNotDependOnBundleBorE() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(B);
        bundles.add(E);
        assertFalse(rdu.isBundleAffected(target, bundles));
    }
    
    @Test
    public void testTargetDependsOnBundleC() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(C);
        assertTrue(rdu.isBundleAffected(target, bundles));
    }
    
    @Test
    public void testTargetDependsOnBundleD() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(D);
        assertTrue(rdu.isBundleAffected(target, bundles));
    }
    
    @Test
    public void testAllBundlesInList() {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(A);
        bundles.add(B);
        bundles.add(C);
        bundles.add(D);
        bundles.add(E);
        bundles.add(target);
        assertTrue(rdu.isBundleAffected(target, bundles));
    }
}
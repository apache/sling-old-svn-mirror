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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
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
    private PackageAdmin pa;
    private RefreshDependenciesUtil rdu;
    private long counter = 1;
    
    private Bundle setupBundle(String mockName, String importPackages, final String exportsPackages) {
        final Bundle result = jmock.mock(Bundle.class, mockName);
        
        final Dictionary<Object, Object> headers = new Hashtable<Object, Object>();
        if(importPackages != null) {
            headers.put(Constants.IMPORT_PACKAGE, importPackages);
        }
        
        jmock.checking(new Expectations() {{
            allowing(result).getBundleId();
            will(returnValue(counter++));
            
            allowing(result).getHeaders();
            will(returnValue(headers));
        }});
        
        final List<ExportedPackage> eps = new ArrayList<ExportedPackage>();
        
        if(exportsPackages != null) {
            final ExportedPackage ep = jmock.mock(ExportedPackage.class, "ExportedPackage" + mockName);
            eps.add(ep);
            jmock.checking(new Expectations() {{
                allowing(ep).getExportingBundle();
                will(returnValue(result));
                allowing(ep).getName();
                will(returnValue(exportsPackages));
            }});
        }
            
        jmock.checking(new Expectations() {{
            allowing(pa).getExportedPackages(exportsPackages);
            will(returnValue(eps.toArray(new ExportedPackage[]{})));
        }});
        
        return result;
    }
    
    @Before
    public void setup() {
        jmock = new Mockery();
        pa = jmock.mock(PackageAdmin.class);
        rdu = new RefreshDependenciesUtil(pa);
        
        // Target depends on A directly and does not depend on B
        target = setupBundle("testTarget", "com.targetImportsOne;com.targetImportsTwo", null);
        A = setupBundle("A", null, "com.targetImportsOne");
        B = setupBundle("B", "some.import", "some.export");
        
        // Target depends on C which in turns depends on D
        C = setupBundle("C", "com.CimportsOne", "com.targetImportsTwo");
        D = setupBundle("D", null, "com.CimportsOne");
        E = setupBundle("E", null, null);
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
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
package org.apache.sling.ide.test.impl;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

public class TychoJREHelper {

    public static boolean hasDefaultVM() {
        IVMInstall vm = JavaRuntime.getDefaultVMInstall();
        return vm!=null;
    }
    
    public static void installDefaultVM(String jrePath) throws CoreException {
        final IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
        String id = null;
        do {
            id = String.valueOf(System.currentTimeMillis());
        } while (vmInstallType.findVMInstall(id) != null);

        VMStandin newVm = new VMStandin(vmInstallType, id);
        newVm.setName("Default-VM");
        File installLocation = new File(jrePath);
        newVm.setInstallLocation(installLocation);
        IVMInstall realVm = newVm.convertToRealVM();
        JavaRuntime.setDefaultVMInstall(realVm, new NullProgressMonitor());
    }

}

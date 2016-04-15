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
package org.apache.sling.commons.osgi;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/** Processes a bundle file by changing its Bundle-SymbolicName.
 *  The original BSN is copied to a an {@link #X_ORIG_BSN} header,
 *  to allow users to find out what happened.
 * @since 2.4
 */
public class BSNRenamer extends BundleFileProcessor {
    private final String newBSN;
    public static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    public static final String X_ORIG_BSN = "X-Original-Bundle-SymbolicName"; 
    public static final String BUNDLE_VERSION = "Bundle-Version"; 
    
    public BSNRenamer(File input, File outputFolder, String newBSN) {
        super(input, outputFolder);
        this.newBSN = newBSN;
    }
    
    protected Manifest processManifest(Manifest inputMF) {
        Attributes inputAttrs = inputMF.getMainAttributes();
        String orgBSN = inputAttrs.getValue(BUNDLE_SYMBOLIC_NAME);
        Manifest newMF = new Manifest(inputMF);
        Attributes outputAttrs = newMF.getMainAttributes();
        outputAttrs.putValue(BUNDLE_SYMBOLIC_NAME, newBSN);
        outputAttrs.putValue(X_ORIG_BSN, orgBSN);
        return newMF;
    }
    
    protected String getTargetFilename(Manifest inputJarManifest) {
        String bver = inputJarManifest.getMainAttributes().getValue(BUNDLE_VERSION);
        if (bver == null) {
            bver = "0.0.0";
        }
        return newBSN + "-" + bver + ".jar";
    }
}

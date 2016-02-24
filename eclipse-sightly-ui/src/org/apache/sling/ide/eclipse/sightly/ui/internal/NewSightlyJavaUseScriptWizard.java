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
package org.apache.sling.ide.eclipse.sightly.ui.internal;

import org.eclipse.core.runtime.IPath;

public class NewSightlyJavaUseScriptWizard extends AbstractNewSightlyFileWizard {

    public NewSightlyJavaUseScriptWizard() {
        super("New Sightly Java Use-Script", "Create a new Sightly Java Use-Script");
    }
    
    @Override
    protected String getInitialContents() {
        
        IPath fullPath = fileCreationPage.getContainerFullPath();
        
        String className = fileCreationPage.getFileName().replace(".java", "");
        
        String inferredPackage = JavaUtils.inferPackage(fullPath);
        if ( inferredPackage == null ) {
            inferredPackage = "unknown // TODO - replace with actual path";
        }
        
        return "" +
               "package " + inferredPackage + ";\n" + 
               "\n" +
               "import java.util.Date;\n" +
               "\n" +
               "public class " + className + " {\n" +
               "    public String getDate() {\n" +
               "        return new Date().toString();\n" +
               "    }\n" +
               "}";
    }

}

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
package org.apache.sling.maven.htl.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.sling.scripting.sightly.compiler.CompilationUnit;

public class ScriptCompilationUnit implements CompilationUnit {

    private Reader reader;
    private File sourceDirectory;
    private File script;
    private String scriptName;
    private static final int _16K = 16384;

    public ScriptCompilationUnit(File sourceDirectory, File script) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(script), _16K);
        this.sourceDirectory = sourceDirectory;
        this.script = script;
    }

    @Override
    public String getScriptName() {
        if (scriptName == null) {
            scriptName = script.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
        }
        return scriptName;
    }

    @Override
    public Reader getScriptReader() {
        return reader;
    }

    public void dispose() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

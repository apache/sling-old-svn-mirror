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
package org.apache.sling.scripting.sightly.impl.compiled;

import org.apache.sling.scripting.sightly.impl.compiler.CompilerBackend;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.VisitorHandler;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.CodeGenVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.StatefulVisitor;

/**
 * Backend which generates a Java class.
 */
public class JavaClassBackend implements CompilerBackend {

    private UnitBuilder unitBuilder = new UnitBuilder();

    @Override
    public void handle(CommandStream stream) {
        StatefulVisitor statefulVisitor = new StatefulVisitor();
        final CodeGenVisitor visitor = new CodeGenVisitor(unitBuilder, statefulVisitor.getControl());
        statefulVisitor.initializeWith(visitor);
        stream.addHandler(new VisitorHandler(statefulVisitor) {
            @Override
            public void onDone() {
                super.onDone();
                visitor.finish();
            }
        });
    }

    public CompilationOutput build() {
        return this.unitBuilder.build();
    }
}

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
package org.apache.sling.apt.parser.internal;

import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.macro.manager.MacroNotFoundException;
import org.apache.maven.doxia.module.apt.AptParser;
import org.apache.maven.doxia.sink.Sink;

/** Customize the doxia APT parser to use our MacroResolver */
class CustomAptParser extends AptParser{

    private final MacroResolver macroProvider;

    /** Instead of throwing an Exception we execute this Macro
     *  if we didn't find the requested one
     */ 
    static class DefaultMacro implements Macro {
        private final String id;
        
        DefaultMacro(String id) {
            this.id = id;
        }
        
        public void execute(Sink sink, MacroRequest request) throws MacroExecutionException {
            sink.text("APT macro not found: '" + id + "'");
        }
    }
    
    CustomAptParser(MacroResolver mp) {
        macroProvider = mp;
    }

    @Override
    public void executeMacro( String macroId, MacroRequest request, Sink sink )
    throws MacroExecutionException, MacroNotFoundException
    {
        Macro m = macroProvider.resolveMacro(macroId);
        if(m == null) {
            m = new DefaultMacro(macroId);
        }
        m.execute( sink, request );
    }
}

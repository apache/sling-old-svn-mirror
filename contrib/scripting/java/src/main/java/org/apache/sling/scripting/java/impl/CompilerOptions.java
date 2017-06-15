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
package org.apache.sling.scripting.java.impl;

import org.apache.sling.commons.compiler.Options;

public class CompilerOptions extends Options {

    private static final long serialVersionUID = 6526386931840426139L;

    private String encoding;

    /**
     * Create an compiler options object using data available from
     * the component configuration.
     */
    public static CompilerOptions createOptions(final JavaScriptEngineFactory.Config config) {
        final String currentVersion = System.getProperty("java.specification.version");
        final CompilerOptions opts = new CompilerOptions();

        opts.put(Options.KEY_GENERATE_DEBUG_INFO, config.java_classdebuginfo());

        opts.put(Options.KEY_SOURCE_VERSION, config.java_compilerSourceVM());
        if ( JavaScriptEngineFactory.VERSION_AUTO.equalsIgnoreCase((String)opts.get(Options.KEY_SOURCE_VERSION)) ) {
            opts.put(Options.KEY_SOURCE_VERSION, currentVersion);
        }

        opts.put(Options.KEY_TARGET_VERSION, config.java_compilerTargetVM());
        if ( JavaScriptEngineFactory.VERSION_AUTO.equalsIgnoreCase((String)opts.get(Options.KEY_TARGET_VERSION)) ) {
            opts.put(Options.KEY_TARGET_VERSION, currentVersion);
        }

        opts.encoding = config.java_javaEncoding();

        opts.put(Options.KEY_IGNORE_WARNINGS, true);

        return opts;
    }

    /**
     * Copy options
     */
    public static CompilerOptions copyOptions(final CompilerOptions opt) {
        final CompilerOptions opts = new CompilerOptions();
        opts.putAll(opt);

        opts.encoding = opt.getJavaEncoding();

        return opts;
    }

    public String getJavaEncoding() {
        return this.encoding;
    }
}

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

import java.util.Dictionary;

import org.apache.sling.commons.compiler.Options;

public class CompilerOptions extends Options {

    private static final long serialVersionUID = 6526386931840426139L;

    private String encoding;

    /**
     * Create an compiler options object using data available from
     * the component configuration.
     */
    public static CompilerOptions createOptions(final Dictionary<String, Object> props) {
        final String currentVersion = System.getProperty("java.specification.version");
        final CompilerOptions opts = new CompilerOptions();

        final Boolean classDebugInfo = (Boolean)props.get(JavaScriptEngineFactory.PROPERTY_CLASSDEBUGINFO);
        opts.put(Options.KEY_GENERATE_DEBUG_INFO, classDebugInfo != null ? classDebugInfo : true);

        final String sourceVM = (String) props.get(JavaScriptEngineFactory.PROPERTY_COMPILER_SOURCE_V_M);
        opts.put(Options.KEY_SOURCE_VERSION, sourceVM != null && sourceVM.trim().length() > 0 ? sourceVM.trim() : JavaScriptEngineFactory.VERSION_AUTO);
        if ( JavaScriptEngineFactory.VERSION_AUTO.equalsIgnoreCase((String)opts.get(Options.KEY_SOURCE_VERSION)) ) {
            opts.put(Options.KEY_SOURCE_VERSION, currentVersion);
        }

        final String targetVM = (String) props.get(JavaScriptEngineFactory.PROPERTY_COMPILER_TARGET_V_M);
        opts.put(Options.KEY_TARGET_VERSION, targetVM != null && targetVM.trim().length() > 0 ? targetVM.trim() : JavaScriptEngineFactory.VERSION_AUTO);
        if ( JavaScriptEngineFactory.VERSION_AUTO.equalsIgnoreCase((String)opts.get(Options.KEY_TARGET_VERSION)) ) {
            opts.put(Options.KEY_TARGET_VERSION, currentVersion);
        }

        final String encoding = (String) props.get(JavaScriptEngineFactory.PROPERTY_ENCODING);
        opts.encoding = encoding != null && encoding.length() > 0 ? encoding : "UTF-8";

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

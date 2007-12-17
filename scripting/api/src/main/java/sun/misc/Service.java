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
package sun.misc;

import java.util.Collections;
import java.util.Iterator;

/**
 * The <code>Service</code> class is a primitive stub of the original
 * <code>sun.misc.Service</code> class used by the
 * <code>javax.script.ScriptEngineManager</code> to find script engine
 * factories in factory service files.
 * <p>
 * This stub is provided because the original class is not part of the official
 * Java API and may not be available on all platforms. In addition even if the
 * class would be available on the Java platform, it may not be visible inside
 * the OSGi framework. Finally, the <em>org.apache.sling.scripting.resolver</em>
 * bundle implements its own resolution of script engine factories and thus the
 * <code>Service</code> method is not used.
 */
public class Service {

    /** Returns an empty iterator */
    public static Iterator<String> providers(Class<?> type, ClassLoader loader) {
        return Collections.<String> emptyList().iterator();
    }

}

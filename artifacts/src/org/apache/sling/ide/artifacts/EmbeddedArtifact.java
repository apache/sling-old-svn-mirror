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
package org.apache.sling.ide.artifacts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class EmbeddedArtifact {

    private final String name;
    private final String version;
    private final URL source;

    public EmbeddedArtifact(String name, String version, URL source) {

        if (name == null) {
            throw new IllegalArgumentException("The name may not be null");
        }

        if (version == null) {
            throw new IllegalArgumentException("The version may not be null");
        }

        if (source == null) {
            throw new IllegalArgumentException("The source may not be null");
        }

        this.version = version;
        this.source = source;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * 
     * Returns a new input stream to this embedded artifact
     * 
     * <p>
     * It is the responsibility of the caller to close this input stream when no longer needed.
     * </p>
     * 
     * @return an input stream to this source
     * @throws IOException
     */
    public InputStream openInputStream() throws IOException {

        return source.openStream();
    }

}

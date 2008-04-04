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
package org.apache.sling.samples.webloader.internal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** InputStream that calls reportProgress during each read() call */
public class ProgressInputStream extends FilterInputStream {

    private final int length;

    public ProgressInputStream(InputStream in, int length) {
        super(in);
        this.length = length;
    }

    public int read() throws IOException {
        int r = super.read();
        reportProgress(r, length);
        return r;
    }

    public int read(byte b[]) throws IOException {
        int r = super.read(b);
        reportProgress(r, length);
        return r;
    }

    public int read(byte b[], int off, int len) throws IOException {
        int r = super.read(b, off, len);
        reportProgress(r, length);
        return r;
    }

    protected void reportProgress(int bytesRead, int totalBytesToRead) {
    }
}

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
package org.apache.sling.ftpserver.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;

public class SlingFtpOutputStream extends FilterOutputStream {

    private final Resource content;

    private final File tmpFile;

    public SlingFtpOutputStream(final Resource content) throws IOException {
        super(null);

        this.content = content;
        this.tmpFile = File.createTempFile("slingftp", ".uploadtmp");
        this.out = new FileOutputStream(this.tmpFile);
    }

    @Override
    public void close() throws IOException {
        InputStream ins = null;
        try {
            super.close();

            ModifiableValueMap map = this.content.adaptTo(ModifiableValueMap.class);
            if (map != null) {
                ins = new FileInputStream(tmpFile);
                map.put("jcr:lastModified", System.currentTimeMillis());
                map.put("jcr:data", ins);
                this.content.getResourceResolver().commit();
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            this.tmpFile.delete();
        }
    }
}

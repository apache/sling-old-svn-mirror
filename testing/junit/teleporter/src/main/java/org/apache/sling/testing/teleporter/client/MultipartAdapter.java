/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.teleporter.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

/** Minimal HTTP client functionality for multipart POST requests */
class MultipartAdapter {
    final OutputStream out;
    final PrintWriter pw;
    final String boundary = "______________" + Double.toHexString(Math.random()) + "______________";
    final String eol = "\r\n";
    final String dashes = "--";
    final String charset;
    int counter = 0;
    
    MultipartAdapter(HttpURLConnection c, String charset) throws IOException {
        this.charset = charset;
        c.setUseCaches(false);
        c.setDoOutput(true);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        out = c.getOutputStream();
        pw = new PrintWriter(new OutputStreamWriter(out, charset), true); 
    }
    
    void close() throws IOException {
        pw.append(dashes).append(boundary).append(dashes).append(eol).flush();
        out.close();
    }
    
    MultipartAdapter parameter(String name, String value) {
        pw.append(dashes).append(boundary).append(eol);
        pw.append("Content-Disposition: form-data; name=\"" + name + "\"").append(eol);
        pw.append("Content-Type: text/plain; charset=").append(charset).append(eol);
        pw.append(eol).append(value).append(eol).flush();
        return this;
    }
    
    MultipartAdapter file(String fieldName, String filename, String contentType, InputStream data) throws IOException {
        pw.append(dashes).append(boundary).append(eol);
        pw.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"");
        pw.append("; filename=\"").append(filename).append("\"").append(eol);
        pw.append("Content-Type: ").append(contentType).append(eol);
        pw.append("Content-Transfer-Encoding: binary").append(eol);
        pw.append(eol).flush();
        copy(data, out);
        pw.append(eol).flush();
        return this;
    }
    
    private void copy(InputStream is, OutputStream os) throws IOException {
        final byte[] buffer = new byte[16384];
        int n=0;
        while((n = is.read(buffer, 0, buffer.length)) > 0) {
            os.write(buffer, 0, n);
        }
        os.flush();
    }
}

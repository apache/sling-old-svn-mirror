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
package org.apache.sling.testing.tools.http;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;


/** Generate RESTful API documentation based on actual requests
 *  executed during integration tests, enhanced with user-supplied
 *  bits of documentation. 
 */
public class RequestDocumentor {
    public static final String OUTPUT_BASE = "./target/" + RequestDocumentor.class.getSimpleName();
    private final String name;
    
    public RequestDocumentor(String name) {
        this.name = name;
    }
    
    public String toString() {
        return getClass().getSimpleName() + " (" + name + ")";
    }
    
    void generateDocumentation(RequestExecutor executor, String [] metadata) throws IOException {
        final File f = getOutputFile();
        final File dir = f.getParentFile();
        dir.mkdirs();
        if(!dir.isDirectory()) {
            throw new IOException("Failed to create output folder " + dir.getAbsolutePath());
        }
        final PrintWriter pw = new PrintWriter(new FileWriter(f, true));
        try {
            System.out.println("Appending documentation of " + executor + " to " + f.getAbsolutePath());
            documentRequest(pw, executor, metadata);
        } finally {
            pw.flush();
            pw.close();
        }
    }
    
    protected File getOutputFile() {
        return new File(OUTPUT_BASE + "/" + name + ".txt");
    }
    
    protected void documentRequest(PrintWriter pw, RequestExecutor executor, String [] metadataArray) throws IOException {
        // Convert metadata to more convenient Map 
        final Map<String, String> m = new HashMap<String, String>();
        if(metadataArray.length % 2 != 0) {
            throw new IllegalArgumentException("Metadata array must be of even size, got " + metadataArray.length);
        }
        for(int i=0 ; i < metadataArray.length; i += 2) {
            m.put(metadataArray[i], metadataArray[i+1]);
        }
        
        // TODO use velocity or other templates? Just a rough prototype for now
        // Also need to filter overly long input/output, binary etc.
        pw.println();
        pw.println("====================================================================================");
        pw.print("=== ");
        pw.print(m.get("title"));
        pw.println(" ===");
        pw.println(m.get("description"));

        pw.print("\n=== ");
        pw.print("REQUEST");
        pw.println(" ===");
        
        pw.print("Method: ");
        pw.println(executor.getRequest().getMethod());
        pw.print("URI: ");
        pw.println(executor.getRequest().getURI());
        
        final Header [] allHeaders = executor.getRequest().getAllHeaders();
        if(allHeaders != null && allHeaders.length > 0) {
            pw.println("Headers:");
            for(Header h : allHeaders) {
                pw.print(h.getName());
                pw.print(":");
                pw.println(h.getValue());
            }
        }
        
        if(executor.getRequest() instanceof HttpEntityEnclosingRequestBase) {
            final HttpEntityEnclosingRequestBase heb = (HttpEntityEnclosingRequestBase)executor.getRequest();
            if(heb.getEntity() != null) {
                pw.print("Content-Type:");
                pw.println(heb.getEntity().getContentType().getValue());
                pw.println("Content:");
                final InputStream is = heb.getEntity().getContent();
                final byte[] buffer = new byte[16384];
                int count = 0;
                while( (count = is.read(buffer, 0, buffer.length)) > 0) {
                    // TODO encoding??
                    pw.write(new String(buffer, 0, count));
                }
                pw.println();
            }
        }
        
        pw.print("\n=== ");
        pw.print("RESPONSE");
        pw.println(" ===");
        pw.print("Content-Type:");
        pw.println(executor.getResponse().getEntity().getContentType().getValue());
        pw.println("Content:");
        pw.println(executor.getContent());
        
        pw.println("====================================================================================");
    }
}

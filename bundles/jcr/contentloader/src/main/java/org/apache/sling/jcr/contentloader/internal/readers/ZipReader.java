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
package org.apache.sling.jcr.contentloader.internal.readers;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;


/**
 * The <code>ZipReader</code> TODO
 *
 * @since 2.0.4
 */
@Component
@Service
@Properties({
    @Property(name = ContentReader.PROPERTY_EXTENSIONS, value = {"zip", "jar"}),
    @Property(name = ContentReader.PROPERTY_TYPES, value = {"application/zip", "application/java-archive"})
})
public class ZipReader implements ContentReader {

    private static final String NT_FOLDER = "nt:folder";

    /**
     * @see org.apache.sling.jcr.contentloader.ContentReader#parse(java.net.URL, org.apache.sling.jcr.contentloader.ContentCreator)
     */
    public void parse(java.net.URL url, ContentCreator creator)
    		throws IOException, RepositoryException {
    	parse(url.openStream(), creator);
    }

	/**
	 * @see org.apache.sling.jcr.contentloader.ContentReader#parse(java.io.InputStream, org.apache.sling.jcr.contentloader.ContentCreator)
	 */
	public void parse(InputStream ins, ContentCreator creator)
			throws IOException, RepositoryException {
        try {
            creator.createNode(null, NT_FOLDER, null);
            final ZipInputStream zis = new ZipInputStream(ins);
            ZipEntry entry;
            do {
                entry = zis.getNextEntry();
                if ( entry != null ) {
                    if ( !entry.isDirectory() ) {
                        String name = entry.getName();
                        int pos = name.lastIndexOf('/');
                        if ( pos != -1 ) {
                            creator.switchCurrentNode(name.substring(0, pos), NT_FOLDER);
                        }
                        creator.createFileAndResourceNode(name, new CloseShieldInputStream(zis), null, entry.getTime());
                        creator.finishNode();
                        creator.finishNode();
                        if ( pos != -1 ) {
                            creator.finishNode();
                        }
                    }
                    zis.closeEntry();
                }

            } while ( entry != null );
            creator.finishNode();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }
	}

}

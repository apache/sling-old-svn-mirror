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
package org.apache.sling.installer.provider.jcr.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.installer.api.InstallableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a Node that is a file to an InstallableResource that wraps an InputStream
 */
public class FileNodeConverter implements JcrInstaller.NodeConverter {

    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_CONTENT_DATA = JCR_CONTENT + "/jcr:data";
    private static final String JCR_LAST_MODIFIED = "jcr:lastModified";
    private static final String JCR_CONTENT_LAST_MODIFIED = JCR_CONTENT + "/" + JCR_LAST_MODIFIED;

    private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @see org.apache.sling.installer.provider.jcr.impl.JcrInstaller.NodeConverter#convertNode(javax.jcr.Node, int)
	 */
	public InstallableResource convertNode(
	        final Node n,
	        final int priority)
	throws RepositoryException {
		InstallableResource result = null;
		if (n.hasProperty(JCR_CONTENT_DATA) && n.hasProperty(JCR_CONTENT_LAST_MODIFIED)) {
			try {
				result = convert(n, n.getPath(), priority);
			} catch(IOException ioe) {
				log.info("Conversion failed, node {} ignored ({})", n.getPath(), ioe);
			}
			return result;
		}
		log.debug("Node {} has no {} properties, ignored", n.getPath(),
				JCR_CONTENT_DATA + " or " + JCR_CONTENT_LAST_MODIFIED);
		return null;
	}

	private InstallableResource convert(
	        final Node n,
	        final String path,
	        final int priority)
    throws IOException, RepositoryException {
		String digest = null;
        if (n.hasProperty(JCR_CONTENT_LAST_MODIFIED)) {
        	digest = String.valueOf(n.getProperty(JCR_CONTENT_LAST_MODIFIED).getDate().getTimeInMillis());
        } else {
        	throw new IOException("Missing " + JCR_CONTENT_LAST_MODIFIED + " property");
	    }

        InputStream is = null;
        if(n.hasProperty(JCR_CONTENT_DATA)) {
        	is = n.getProperty(JCR_CONTENT_DATA).getStream();
        } else {
        	throw new IOException("Missing " + JCR_CONTENT_DATA + " property");
        }

        return new InstallableResource(path, is, null, digest, null, priority);
	}
}
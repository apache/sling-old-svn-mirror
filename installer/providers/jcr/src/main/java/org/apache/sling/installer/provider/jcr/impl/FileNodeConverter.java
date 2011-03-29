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
import java.util.Dictionary;
import java.util.Hashtable;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * @see org.apache.sling.installer.provider.jcr.impl.JcrInstaller.NodeConverter#convertNode(javax.jcr.Node, int)
	 */
	public InstallableResource convertNode(
	        final Node n,
	        final int priority)
	throws RepositoryException {
	    final String nodePath = n.getPath();
		if (n.hasProperty(JCR_CONTENT_DATA) && n.hasProperty(JCR_CONTENT_LAST_MODIFIED)) {
			try {
				return convert(n, nodePath, priority);
			} catch (final IOException ioe) {
			    logger.info("Conversion failed, node {} ignored ({})", nodePath, ioe);
			}
		} else {
		    logger.debug("Node {} has no {} properties, ignored", nodePath,
    				JCR_CONTENT_DATA + " or " + JCR_CONTENT_LAST_MODIFIED);
	    }
		return null;
	}

	private InstallableResource convert(
	        final Node n,
	        final String path,
	        final int priority)
    throws IOException, RepositoryException {
		final String digest = String.valueOf(n.getProperty(JCR_CONTENT_LAST_MODIFIED).getDate().getTimeInMillis());

        final InputStream is =  n.getProperty(JCR_CONTENT_DATA).getStream();
        final Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(InstallableResource.INSTALLATION_HINT, n.getParent().getName());
        return new InstallableResource(path, is, dict, digest, null, priority);
	}
}
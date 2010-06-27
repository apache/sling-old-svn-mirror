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
package org.apache.sling.servlets.post.impl.operations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * The <code>ImportOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_IMPORT}
 * import operation for the Sling default POST servlet.
 */
public class ImportOperation extends AbstractSlingPostOperation {

    /**
     * The default node name generator
     */
    private final NodeNameGenerator defaultNodeNameGenerator;

    /**
     * utility class for generating node names
     */
    private NodeNameGenerator[] extraNodeNameGenerators;

    /**
     * Reference to the content importer service
     */
	private ContentImporter contentImporter;

    public ImportOperation(NodeNameGenerator defaultNodeNameGenerator,
            ContentImporter contentImporter) {
        this.defaultNodeNameGenerator = defaultNodeNameGenerator;
        this.contentImporter = contentImporter;
    }
    
	public void setContentImporter(ContentImporter importer) {
		this.contentImporter = importer;
	}

    public void setExtraNodeNameGenerators(NodeNameGenerator[] extraNodeNameGenerators) {
        this.extraNodeNameGenerators = extraNodeNameGenerators;
    }
	
    @Override
    protected void doRun(SlingHttpServletRequest request, HtmlResponse response, final List<Modification> changes)
    		throws RepositoryException {
    	ContentImporter importer = contentImporter;
    	if (importer == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Missing content importer for import");
            return;
    	}
    	
        Resource resource = request.getResource();
        Node node = resource.adaptTo(Node.class);
        if (node == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    "Missing target node " + resource + " for import");
            return;
        }

        String contentType = request.getParameter(SlingPostConstants.RP_CONTENT_TYPE);
        if (contentType == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Required :contentType parameter is missing");
            return;
        }

        //import options passed as request parameters.
        final boolean replace = "true".equals(request.getParameter(SlingPostConstants.RP_REPLACE));
        final boolean checkin = "true".equals(request.getParameter(SlingPostConstants.RP_CHECKIN));
        
        String basePath = getItemPath(request);
        if (basePath.endsWith("/")) {
        	//remove the trailing slash
        	basePath = basePath.substring(0, basePath.length() - 1);
        }
		String name = generateName(request, basePath);
        String contentRootName = name + "." + contentType;
        response.setCreateRequest(true);
        
        try {
            InputStream contentStream = null;
            String content = request.getParameter(SlingPostConstants.RP_CONTENT);
            if (content != null) {
                contentStream = new ByteArrayInputStream(content.getBytes());
            } else {
            	RequestParameter contentFile = request.getRequestParameter(SlingPostConstants.RP_CONTENT_FILE);
            	if (contentFile != null) {
            		contentStream = contentFile.getInputStream();
            	}
            }

            if (contentStream == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Missing content for import");
                return;
            } else {
            	importer.importContent(node, contentRootName, contentStream,
    					new ImportOptions() {

							@Override
							public boolean isCheckin() {
								return checkin;
							}

							@Override
							public boolean isIgnoredImportProvider(
									String extension) {
    							// this probably isn't important in this context.
								return false;
							}

							@Override
							public boolean isOverwrite() {
								return replace;
							}
    					},
    					new ContentImportListener() {
							
							public void onReorder(String orderedPath, String beforeSibbling) {
								changes.add(Modification.onOrder(orderedPath, beforeSibbling));
							}
							
							public void onMove(String srcPath, String destPath) {
								changes.add(Modification.onMoved(srcPath, destPath));
							}
							
							public void onModify(String srcPath) {
								changes.add(Modification.onModified(srcPath));
							}
							
							public void onDelete(String srcPath) {
								changes.add(Modification.onDeleted(srcPath));
							}
							
							public void onCreate(String srcPath) {
								changes.add(Modification.onCreated(srcPath));
							}
							
							public void onCopy(String srcPath, String destPath) {
								changes.add(Modification.onMoved(srcPath, destPath));
							}
						});
            }

            if (!changes.isEmpty()) {
                //fill in the data for the response report
                Modification modification = changes.get(0);
                if (modification.getType() == ModificationType.CREATE) {
                	String importedPath = modification.getSource();
                	response.setLocation(importedPath);
                	response.setPath(importedPath);
                	int lastSlashIndex = importedPath.lastIndexOf('/');
                	if (lastSlashIndex != -1) {
                		String parentPath = importedPath.substring(0, lastSlashIndex);
                		response.setParentLocation(parentPath);
                	}
                }
            }
        } catch (IOException e) {
        	throw new RepositoryException(e);
        }
    }

    
    private String generateName(SlingHttpServletRequest request, String basePath)
    		throws RepositoryException {
    	boolean requirePrefix = requireItemPathPrefix(request);

    	String generatedName = null;
    	if (extraNodeNameGenerators != null) {
    		for (NodeNameGenerator generator : extraNodeNameGenerators) {
    			generatedName = generator.getNodeName(request, basePath, requirePrefix, defaultNodeNameGenerator);
    			if (generatedName != null) {
    				break;
    			}
    		}
    	}
    	if (generatedName == null) {
    		generatedName = defaultNodeNameGenerator.getNodeName(request, basePath, requirePrefix, defaultNodeNameGenerator);
    	}

    	// If the path ends with a *, create a node under its parent, with
    	// a generated node name
    	basePath += "/" + generatedName;

    	// if resulting path exists, add a suffix until it's not the case
    	// anymore
    	Session session = request.getResourceResolver().adaptTo(Session.class);

    	// if resulting path exists, add a suffix until it's not the case
    	// anymore
    	if (session.itemExists(basePath)) {
    		for (int idx = 0; idx < 1000; idx++) {
    			String newPath = basePath + "_" + idx;
    			if (!session.itemExists(newPath)) {
    				basePath = newPath;
    				break;
    			}
    		}
    	}

    	// if it still exists there are more than 1000 nodes ?
    	if (session.itemExists(basePath)) {
    		throw new RepositoryException(
    				"Collision in generated node names for path=" + basePath);
    	}

    	//the last segment is the name.
    	String name = basePath.substring(basePath.lastIndexOf('/') + 1);
		return name;
    }

}
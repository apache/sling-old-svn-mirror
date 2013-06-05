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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;

/**
 * The <code>ImportOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_IMPORT}
 * import operation for the Sling default POST servlet.
 */
public class ImportOperation extends AbstractCreateOperation {

    /**
     * Reference to the content importer service
     */
    private ContentImporter contentImporter;

    public void setContentImporter(ContentImporter importer) {
        this.contentImporter = importer;
    }

    private String getRequestParamAsString(SlingHttpServletRequest request, String key) {
    	RequestParameter requestParameter = request.getRequestParameter(key);
    	if (requestParameter == null) {
    		return null;
    	}
    	return requestParameter.getString();
    }

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, final List<Modification> changes)
            throws RepositoryException {
        ContentImporter importer = contentImporter;
        if (importer == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Missing content importer for import");
            return;
        }
        Map<String, RequestProperty> reqProperties = collectContent(request,
             response);

        VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

        // do not change order unless you have a very good reason.
        Session session = request.getResourceResolver().adaptTo(Session.class);

        try {
            processCreate(request.getResourceResolver(), reqProperties, response, changes, versioningConfiguration);
        } catch ( final PersistenceException pe) {
            if ( pe.getCause() instanceof RepositoryException ) {
                throw (RepositoryException)pe.getCause();
            }
            throw new RepositoryException(pe);
        }
        String path = response.getPath();
        Node node = null;
        try {
            node = (Node) session.getItem(path);
        } catch ( RepositoryException e ) {
            log.warn(e.getMessage(),e);
            // was not able to resolve the node
        } catch ( ClassCastException e) {
            log.warn(e.getMessage(),e);
            // it was not a node
        }
        if (node == null) {

            response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    "Missing target node " + path + " for import");
            return;
        }

        String contentType = getRequestParamAsString(request, SlingPostConstants.RP_CONTENT_TYPE);
        if (contentType == null) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
            "Required :contentType parameter is missing");
            return;
        }

        //import options passed as request parameters.
        final boolean replace = "true".equalsIgnoreCase(getRequestParamAsString(request, SlingPostConstants.RP_REPLACE));
        final boolean replaceProperties = "true".equalsIgnoreCase(getRequestParamAsString(request, SlingPostConstants.RP_REPLACE_PROPERTIES));
        final boolean checkin = "true".equalsIgnoreCase(getRequestParamAsString(request, SlingPostConstants.RP_CHECKIN));
        final boolean autoCheckout = "true".equalsIgnoreCase(getRequestParamAsString(request, SlingPostConstants.RP_AUTO_CHECKOUT));

        String basePath = getItemPath(request);
        basePath = removeAndValidateWorkspace(basePath, request.getResourceResolver().adaptTo(Session.class));
        if (basePath.endsWith("/")) {
            //remove the trailing slash
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        // default to creating content
        response.setCreateRequest(true);

        final String targetName;
        //check if a name was posted to use as the name of the imported root node
        if (getRequestParamAsString(request, SlingPostConstants.RP_NODE_NAME) != null) {
            // exact name
            targetName = getRequestParamAsString(request, SlingPostConstants.RP_NODE_NAME);
            if (targetName.length() > 0 && node.hasNode(targetName)) {
                if (replace) {
                    response.setCreateRequest(false);
                } else {
                    response.setStatus(
                        HttpServletResponse.SC_PRECONDITION_FAILED,
                        "Cannot import " + path + "/" + targetName
                            + ": node exists");
                    return;
                }
            }
        } else if (getRequestParamAsString(request, SlingPostConstants.RP_NODE_NAME_HINT) != null) {
            // node name hint only
            String nodePath = generateName(request, basePath);
            String name = nodePath.substring(nodePath.lastIndexOf('/') + 1);
            targetName = name;
        } else {
            // no name posted, so the import won't create a root node
            targetName = "";
        }
        final String contentRootName = targetName + "." + contentType;

        try {
            InputStream contentStream = null;
        	RequestParameter contentParameter = request.getRequestParameter(SlingPostConstants.RP_CONTENT);
            if (contentParameter != null) {
                contentStream = contentParameter.getInputStream();
            } else {
                RequestParameter contentFile = request.getRequestParameter(SlingPostConstants.RP_CONTENT_FILE);
                if (contentFile != null) {
                    contentStream = contentFile.getInputStream();
                }
            }

            if (contentStream == null) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
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
							public boolean isAutoCheckout() {
								return autoCheckout;
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

                            /* (non-Javadoc)
                             * @see org.apache.sling.jcr.contentloader.ImportOptions#isPropertyOverwrite()
                             */
                            @Override
                            public boolean isPropertyOverwrite() {
                                return replaceProperties;
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
                                changes.add(Modification.onCopied(srcPath, destPath));
                            }

                            public void onCheckin(String srcPath) {
                                changes.add(Modification.onCheckin(srcPath));
                            }
                            public void onCheckout(String srcPath) {
                                changes.add(Modification.onCheckout(srcPath));
                            }
                        });
            }

            if (!changes.isEmpty()) {
                //fill in the data for the response report
                Modification modification = changes.get(0);
                if (modification.getType() == ModificationType.CREATE) {
                    String importedPath = modification.getSource();
                    response.setLocation(externalizePath(request, importedPath));
                    response.setPath(importedPath);
                    int lastSlashIndex = importedPath.lastIndexOf('/');
                    if (lastSlashIndex != -1) {
                        String parentPath = importedPath.substring(0, lastSlashIndex);
                        response.setParentLocation(externalizePath(request, parentPath));
                    }
                }
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}
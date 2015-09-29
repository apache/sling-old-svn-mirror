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
package org.apache.sling.ide.eclipse.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ResourceUtil;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.core.internal.ResourceAndInfo;
import org.apache.sling.ide.eclipse.core.internal.ResourceChangeCommandFactory;
import org.apache.sling.ide.eclipse.core.progress.ProgressUtils;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.filter.IgnoredResources;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;

// intentionally does not implement IRunnableWithProgress to cut dependency on JFace
public class ImportRepositoryContentAction {

    private final IServer server;
    private final IPath projectRelativePath;
    private final IProject project;
    private final Logger logger;

    private SerializationManager serializationManager;
	private SerializationDataBuilder builder;
    private IgnoredResources ignoredResources;
    private IProgressMonitor monitor;
    private Repository repository;
    private Filter filter;
    private File contentSyncRoot;
    private IFolder contentSyncRootDir;
    private Set<IResource> currentResources;
    private IPath repositoryImportRoot;

    /**
     * @param server
     * @param projectRelativePath
     * @param project
     * @throws SerializationException 
     */
    public ImportRepositoryContentAction(IServer server, IPath projectRelativePath, IProject project,
            SerializationManager serializationManager) throws SerializationException {
        this.logger = Activator.getDefault().getPluginLogger();
        this.server = server;
        this.projectRelativePath = projectRelativePath;
        this.project = project;
        this.serializationManager = serializationManager;
        this.ignoredResources = new IgnoredResources();
        this.currentResources = new HashSet<IResource>();
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException,
            SerializationException, CoreException {

        // TODO: We should try to make this give 'nice' progress feedback (aka here's what I'm processing)
        monitor.beginTask("Repository import", IProgressMonitor.UNKNOWN);

        this.monitor = monitor;

        repository = ServerUtil.getConnectedRepository(server, monitor);

        this.builder = serializationManager.newBuilder(
        		repository, ProjectUtil.getSyncDirectoryFile(project));

        SerializationKindManager skm;
        
        try {
            skm = new SerializationKindManager();
            skm.init(repository);
        } catch (RepositoryException e1) {
            throw new InvocationTargetException(e1);
        }

        filter = ProjectUtil.loadFilter(project);

        ProgressUtils.advance(monitor, 1);

        try {

            contentSyncRootDir = ProjectUtil.getSyncDirectory(project);
            repositoryImportRoot = projectRelativePath
                    .makeRelativeTo(contentSyncRootDir.getProjectRelativePath())
                    .makeAbsolute();

            contentSyncRoot = ProjectUtil.getSyncDirectoryFullPath(project).toFile();

            readVltIgnoresNotUnderImportRoot(contentSyncRootDir, repositoryImportRoot);

            ProgressUtils.advance(monitor, 1);

            Activator
                    .getDefault()
                    .getPluginLogger()
                    .trace("Starting import; repository start point is {0}, workspace start point is {1}",
                            repositoryImportRoot, projectRelativePath);

            recordNotIgnoredResources();

            ProgressUtils.advance(monitor, 1);

            crawlChildrenAndImport(repositoryImportRoot.toPortableString());

            removeNotIgnoredAndNotUpdatedResources(new NullProgressMonitor());

            ProgressUtils.advance(monitor, 1);

        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            if (builder!=null) {
            	builder.destroy();
            	builder = null;
            }
            monitor.done();
        }

    }

    private void readVltIgnoresNotUnderImportRoot(IFolder syncDir, IPath repositoryImportRoot) throws IOException,
            CoreException {

        IFolder current = syncDir;
        for (int i = 0; i < repositoryImportRoot.segmentCount(); i++) {
            IPath repoPath = current.getProjectRelativePath().makeRelativeTo(syncDir.getProjectRelativePath())
                    .makeAbsolute();
            parseIgnoreFiles(current, repoPath.toPortableString());
            current = (IFolder) current.findMember(repositoryImportRoot.segment(i));
        }

    }

    private void recordNotIgnoredResources() throws CoreException {

        final ResourceChangeCommandFactory rccf = new ResourceChangeCommandFactory(serializationManager);

        IResource importStartingPoint = contentSyncRootDir.findMember(repositoryImportRoot);
        if (importStartingPoint == null) {
            return;
        }
        importStartingPoint.accept(new IResourceVisitor() {

            @Override
            public boolean visit(IResource resource) throws CoreException {

                try {
                    ResourceAndInfo rai = rccf.buildResourceAndInfo(resource, repository);

                    if (rai == null) {
                        // can be a prerequisite
                        return true;
                    }

                    String repositoryPath = rai.getResource().getPath();

                    FilterResult filterResult = filter.filter(repositoryPath);

                    if (ignoredResources.isIgnored(repositoryPath)) {
                        return false;
                    }

                    if (filterResult == FilterResult.ALLOW) {
                        currentResources.add(resource);
                        return true;
                    }

                    return false;
                } catch (IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "Failed reading current project's resources", e));
                }
            }
        });

        logger.trace("Found {0} not ignored local resources", currentResources.size());
    }

    private void removeNotIgnoredAndNotUpdatedResources(IProgressMonitor monitor) throws CoreException {

        logger.trace("Found {0} resources to clean up", currentResources.size());

        for (IResource resource : currentResources) {
            if (resource.exists()) {
                logger.trace("Deleting {0}", resource);
                resource.delete(true, monitor);
            }
        }

    }

    /**
     * Crawls the repository and recursively imports founds resources
     * @param path the current path to import from
     * @param tracer
     * @throws JSONException
     * @throws RepositoryException
     * @throws CoreException
     * @throws IOException
     */
    // TODO: This probably should be pushed into the service layer
    private void crawlChildrenAndImport(String path)
            throws RepositoryException, CoreException, IOException, SerializationException {

        logger.trace("crawlChildrenAndImport({0},  {1}, {2}, {3}", repository, path, project, projectRelativePath);

        ResourceProxy resource = executeCommand(repository.newListChildrenNodeCommand(path));
        
        SerializationData serializationData = builder.buildSerializationData(contentSyncRoot, resource);
        logger.trace("For resource at path {0} got serialization data {1}", resource.getPath(), serializationData);

        final List<ResourceProxy> resourceChildren = new LinkedList<ResourceProxy>(resource.getChildren());
		if (serializationData != null) {

            IPath serializationFolderPath = contentSyncRootDir.getProjectRelativePath().append(
                    serializationData.getFolderPath());
	
	        switch (serializationData.getSerializationKind()) {
	            case FILE: {
	                byte[] contents = executeCommand(repository.newGetNodeCommand(path));
                    createFile(project, getPathForPlainFileNode(resource, serializationFolderPath), contents);
	
	                if (serializationData.hasContents()) {
                        createFolder(project, serializationFolderPath);
                        createFile(project, serializationFolderPath.append(serializationData.getFileName()),
	                            serializationData.getContents());
	                    
                        // special processing for nt:resource nodes
                        for (Iterator<ResourceProxy> it = resourceChildren.iterator(); it.hasNext();) {
	                    	ResourceProxy child = it.next();
	                        if (Repository.NT_RESOURCE.equals(child.getProperties().get(Repository.JCR_PRIMARY_TYPE))) {

                                ResourceProxy reloadedChildResource = executeCommand(repository
                                        .newListChildrenNodeCommand(child.getPath()));

                                logger.trace(
                                        "Skipping direct handling of {0} node at {1} ; will additionally handle {2} direct children",
                                        Repository.NT_RESOURCE, child.getPath(), reloadedChildResource.getChildren()
                                                .size());

                                if (reloadedChildResource.getChildren().size() != 0) {

                                    String pathName = Text.getName(reloadedChildResource.getPath());
                                    pathName = serializationManager.getOsPath(pathName);
                                    createFolder(project, serializationFolderPath.append(pathName));

                                    // 2. recursively handle all resources
                                    for (ResourceProxy grandChild : reloadedChildResource.getChildren()) {
                                        crawlChildrenAndImport(grandChild.getPath());
                                    }
                                }
	                            
	                        	it.remove();
	                        	break;
	                        }
						}
	                }
	                break;
	            }
	            case FOLDER:
	            case METADATA_PARTIAL: {

                    IFolder folder = createFolder(project, serializationFolderPath);

                    parseIgnoreFiles(folder, path);

	                if (serializationData.hasContents()) {
                        createFile(project, serializationFolderPath.append(serializationData.getFileName()),
	                            serializationData.getContents());
	                }
	                break;
	            }
	
	            case METADATA_FULL: {
	                if (serializationData.hasContents()) {
                        createFile(project, serializationFolderPath.append(serializationData.getFileName()),
                                serializationData.getContents());
	                }
	                break;
	            }
	        }
	
            logger.trace("Resource at {0} has children: {1}", resource.getPath(), resourceChildren);
	
	        if (serializationData.getSerializationKind() == SerializationKind.METADATA_FULL) {
	            return;
	        }
        } else {
            logger.trace("No serialization data found for {0}", resource.getPath());
        }
		
        ProgressUtils.advance(monitor, 1);

        for (ResourceProxy child : resourceChildren) {

            if (ignoredResources.isIgnored(child.getPath())) {
                continue;
            }

            if (filter != null) {
                FilterResult filterResult = filter.filter(child.getPath());
                if (filterResult == FilterResult.DENY) {
                    continue;
                }
            }

            crawlChildrenAndImport(child.getPath());
        }
    }

    /**
     * Returns the path for serializing the nt:resource data of a nt:file node
     * 
     * <p>
     * The path will be one level above the <tt>serializationFolderPath</tt>, and the name will be the last path segment
     * of the resource.
     * </p>
     * 
     * @param resource The resource
     * @param serializationFolderPath the folder where the serialization data should be stored
     * @return the path for the plain file node
     */
    private IPath getPathForPlainFileNode(ResourceProxy resource, IPath serializationFolderPath) {

        // TODO - can we just use the serializationFolderPath ?

        String name = serializationManager.getOsPath(Text.getName(resource.getPath()));

        return serializationFolderPath.removeLastSegments(1).append(name);
    }

    private void parseIgnoreFiles(IFolder folder, String path) throws IOException, CoreException {
        // TODO - the parsing should be extracted
        IResource vltIgnore = folder.findMember(".vltignore");
        if (vltIgnore != null && vltIgnore instanceof IFile) {

            logger.trace("Found ignore file at {0}", vltIgnore.getFullPath());

            InputStream contents = ((IFile) vltIgnore).getContents();
            try {
                List<String> ignoreLines = IOUtils.readLines(contents);
                for (String ignoreLine : ignoreLines) {
                    logger.trace("Registering ignore rule {0}:{1}", path, ignoreLine);
                    ignoredResources.registerRegExpIgnoreRule(path, ignoreLine);
                }
            } finally {
                IOUtils.closeQuietly(contents);
            }
        }
    }

    private <T> T executeCommand(Command<T> command) throws RepositoryException {

        Result<T> result = command.execute();
        return result.get();
    }

    private IFolder createFolder(IProject project, IPath destinationPath) throws CoreException {

        IFolder destinationFolder = project.getFolder(destinationPath);
        if (!destinationFolder.exists()) {
            logger.trace("Creating folder {0}", destinationFolder.getFullPath());

            createParents(destinationFolder.getParent());
            destinationFolder.create(true, true, null /* TODO progress monitor */);
        }

        destinationFolder.setSessionProperty(ResourceUtil.QN_IMPORT_MODIFICATION_TIMESTAMP,
                destinationFolder.getModificationStamp());
        
        removeTouchedResource(destinationFolder);

        return destinationFolder;
    }

    private void createParents(IContainer container) throws CoreException {
        if (container.exists() || container.getType() != IResource.FOLDER) {
            return;
        }

        createParents(container.getParent());
        createFolder(container.getProject(), container.getProjectRelativePath());
    }

    private void removeTouchedResource(IResource resource) {

        IResource current = resource;
        do {
            currentResources.remove(current);
        } while ((current = current.getParent()) != null);
    }

    private void createFile(IProject project, IPath path, byte[] node) throws CoreException {
        if (node==null) {
            throw new IllegalArgumentException("node must not be null");
        }

        IFile destinationFile = project.getFile(path);

        logger.trace("Writing content file at {0}", path);

        if (destinationFile.exists()) {
            /* TODO progress monitor */
            destinationFile.setContents(new ByteArrayInputStream(node), IResource.KEEP_HISTORY, null);
        } else {
            /* TODO progress monitor */
        	if (!destinationFile.getParent().exists()) {
        		createParents(destinationFile.getParent());
        	}
        	destinationFile.create(new ByteArrayInputStream(node), true, null);
        }

        removeTouchedResource(destinationFile);

        destinationFile.setSessionProperty(ResourceUtil.QN_IMPORT_MODIFICATION_TIMESTAMP,
                destinationFile.getModificationStamp());
    }
}
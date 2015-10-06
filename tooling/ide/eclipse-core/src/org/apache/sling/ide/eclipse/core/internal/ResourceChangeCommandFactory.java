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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ResourceUtil;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.util.PathUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The <tt>ResourceChangeCommandFactory</tt> creates new {@link #Command commands} correspoding to resource addition,
 * change, or removal
 *
 */
public class ResourceChangeCommandFactory {

    private final Set<String> ignoredFileNames = new HashSet<>();
    {
        ignoredFileNames.add(".vlt");
        ignoredFileNames.add(".vltignore");
    }

    private final SerializationManager serializationManager;

    public ResourceChangeCommandFactory(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
    }

    public Command<?> newCommandForAddedOrUpdated(Repository repository, IResource addedOrUpdated) throws CoreException {
        try {
            return addFileCommand(repository, addedOrUpdated);
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed updating " + addedOrUpdated,
                    e));
        }
    }

    private Command<?> addFileCommand(Repository repository, IResource resource) throws CoreException, IOException {

        ResourceAndInfo rai = buildResourceAndInfo(resource, repository);
        
        if (rai == null) {
            return null;
        }
        
        CommandContext context = new CommandContext(ProjectUtil.loadFilter(resource.getProject()));

        if (rai.isOnlyWhenMissing()) {
            return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource(),
                    Repository.CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING);
        }

        return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource());
    }

    /**
     * Convenience method which builds a <tt>ResourceAndInfo</tt> info for a specific <tt>IResource</tt>
     * 
     * @param resource the resource to process
     * @param repository the repository, used to extract serialization information for different resource types
     * @return the build object, or null if one could not be built
     * @throws CoreException
     * @throws SerializationException
     * @throws IOException
     */
    public ResourceAndInfo buildResourceAndInfo(IResource resource, Repository repository) throws CoreException,
            IOException {
        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        Long modificationTimestamp = (Long) resource.getSessionProperty(ResourceUtil.QN_IMPORT_MODIFICATION_TIMESTAMP);

        if (modificationTimestamp != null && modificationTimestamp >= resource.getModificationStamp()) {
            Activator.getDefault().getPluginLogger()
                    .trace("Change for resource {0} ignored as the import timestamp {1} >= modification timestamp {2}",
                            resource, modificationTimestamp, resource.getModificationStamp());
            return null;
        }

        if (resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)) {
            Activator.getDefault().getPluginLogger().trace("Skipping team-private resource {0}", resource);
            return null;
        }

        FileInfo info = createFileInfo(resource);
        Activator.getDefault().getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);

        File syncDirectoryAsFile = ProjectUtil.getSyncDirectoryFullPath(resource.getProject()).toFile();
        IFolder syncDirectory = ProjectUtil.getSyncDirectory(resource.getProject());

        Filter filter = ProjectUtil.loadFilter(resource.getProject());

        ResourceProxy resourceProxy = null;

        if (serializationManager.isSerializationFile(resource.getLocation().toOSString())) {
            IFile file = (IFile) resource;
            try (InputStream contents = file.getContents()) {
                String resourceLocation = file.getFullPath().makeRelativeTo(syncDirectory.getFullPath())
                        .toPortableString();
                resourceProxy = serializationManager.readSerializationData(resourceLocation, contents);
                normaliseResourceChildren(file, resourceProxy, syncDirectory, repository);


                // TODO - not sure if this 100% correct, but we definitely should not refer to the FileInfo as the
                // .serialization file, since for nt:file/nt:resource nodes this will overwrite the file contents
                String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
                if (Repository.NT_FILE.equals(primaryType)) {
                    // TODO move logic to serializationManager
                    File locationFile = new File(info.getLocation());
                    String locationFileParent = locationFile.getParent();
                    int endIndex = locationFileParent.length() - ".dir".length();
                    File actualFile = new File(locationFileParent.substring(0, endIndex));
                    String newLocation = actualFile.getAbsolutePath();
                    String newName = actualFile.getName();
                    String newRelativeLocation = actualFile.getAbsolutePath().substring(
                            syncDirectoryAsFile.getAbsolutePath().length());
                    info = new FileInfo(newLocation, newRelativeLocation, newName);

                    Activator.getDefault().getPluginLogger()
                            .trace("Adjusted original location from {0} to {1}", resourceLocation, newLocation);

                }

            } catch (IOException e) {
                Status s = new Status(Status.WARNING, Activator.PLUGIN_ID, "Failed reading file at "
                        + resource.getFullPath(), e);
                StatusManager.getManager().handle(s, StatusManager.LOG | StatusManager.SHOW);
                return null;
            }
        } else {

            // TODO - move logic to serializationManager
            // possible .dir serialization holder
            if (resource.getType() == IResource.FOLDER && resource.getName().endsWith(".dir")) {
                IFolder folder = (IFolder) resource;
                IResource contentXml = folder.findMember(".content.xml");
                // .dir serialization holder ; nothing to process here, the .content.xml will trigger the actual work
                if (contentXml != null && contentXml.exists()
                        && serializationManager.isSerializationFile(contentXml.getLocation().toOSString())) {
                    return null;
                }
            }

            resourceProxy = buildResourceProxyForPlainFileOrFolder(resource, syncDirectory, repository);
        }

        FilterResult filterResult = getFilterResult(resource, resourceProxy, filter);

        switch (filterResult) {

            case ALLOW:
                return new ResourceAndInfo(resourceProxy, info);
            case PREREQUISITE:
                // never try to 'create' the root node, we assume it exists
                if (!resourceProxy.getPath().equals("/")) {
                    // we don't explicitly set the primary type, which will allow the the repository to choose the best
                    // suited one ( typically nt:unstructured )
                    return new ResourceAndInfo(new ResourceProxy(resourceProxy.getPath()), null, true);
                }
            case DENY: // falls through
            default:
                return null;
        }
    }

    private FileInfo createFileInfo(IResource resource) throws CoreException {

        if (resource.getType() != IResource.FILE) {
            return null;
        }

        IProject project = resource.getProject();

        IFolder syncFolder = project.getFolder(ProjectUtil.getSyncDirectoryValue(project));

        IPath relativePath = resource.getFullPath().makeRelativeTo(syncFolder.getFullPath());

        FileInfo info = new FileInfo(resource.getLocation().toOSString(), relativePath.toOSString(), resource.getName());

        Activator.getDefault().getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);

        return info;
    }

    /**
     * Gets the filter result for a resource/resource proxy combination
     * 
     * <p>
     * The resourceProxy may be null, typically when a resource is already deleted.
     * 
     * <p>
     * The filter may be null, in which case all combinations are included in the filed, i.e. allowed.
     * 
     * @param resource the resource to filter for, must not be <code>null</code>
     * @param resourceProxy the resource proxy to filter for, possibly <code>null</code>
     * @param filter the filter to use, possibly <tt>null</tt>
     * @return the filtering result, never <code>null</code>
     */
    private FilterResult getFilterResult(IResource resource, ResourceProxy resourceProxy, Filter filter) {

        if (filter == null) {
            return FilterResult.ALLOW;
        }

        File contentSyncRoot = ProjectUtil.getSyncDirectoryFile(resource.getProject());

        String repositoryPath = resourceProxy != null ? resourceProxy.getPath() : getRepositoryPathForDeletedResource(
                resource, contentSyncRoot);

        FilterResult filterResult = filter.filter(repositoryPath);

        Activator.getDefault().getPluginLogger().trace("Filter result for {0} for {1}", repositoryPath, filterResult);

        return filterResult;
    }

    private String getRepositoryPathForDeletedResource(IResource resource, File contentSyncRoot) {
        IFolder syncFolder = ProjectUtil.getSyncDirectory(resource.getProject());
        IPath relativePath = resource.getFullPath().makeRelativeTo(syncFolder.getFullPath());

        String absFilePath = new File(contentSyncRoot, relativePath.toOSString()).getAbsolutePath();
        String filePath = serializationManager.getBaseResourcePath(absFilePath);

        IPath osPath = Path.fromOSString(filePath);
        String repositoryPath = serializationManager.getRepositoryPath(osPath.makeRelativeTo(syncFolder.getLocation())
                .makeAbsolute().toPortableString());

        Activator.getDefault().getPluginLogger()
                .trace("Repository path for deleted resource {0} is {1}", resource, repositoryPath);

        return repositoryPath;
    }

    private ResourceProxy buildResourceProxyForPlainFileOrFolder(IResource changedResource, IFolder syncDirectory,
            Repository repository)
            throws CoreException, IOException {

        SerializationKind serializationKind;
        String fallbackNodeType;
        if (changedResource.getType() == IResource.FILE) {
            serializationKind = SerializationKind.FILE;
            fallbackNodeType = Repository.NT_FILE;
        } else { // i.e. IResource.FOLDER
            serializationKind = SerializationKind.FOLDER;
            fallbackNodeType = Repository.NT_FOLDER;
        }

        String resourceLocation = '/' + changedResource.getFullPath().makeRelativeTo(syncDirectory.getFullPath())
                .toPortableString();
        IPath serializationFilePath = Path.fromOSString(serializationManager.getSerializationFilePath(
                resourceLocation, serializationKind));
        IResource serializationResource = syncDirectory.findMember(serializationFilePath);

        if (serializationResource == null && changedResource.getType() == IResource.FOLDER) {
            ResourceProxy dataFromCoveringParent = findSerializationDataFromCoveringParent(changedResource,
                    syncDirectory, resourceLocation, serializationFilePath);

            if (dataFromCoveringParent != null) {
                return dataFromCoveringParent;
            }
        }
        return buildResourceProxy(resourceLocation, serializationResource, syncDirectory, fallbackNodeType, repository);
    }

    /**
     * Tries to find serialization data from a resource in a covering parent
     * 
     * <p>
     * If the serialization resource is null, it's valid to look for a serialization resource higher in the filesystem,
     * given that the found serialization resource covers this resource
     * 
     * @param changedResource the resource which has changed
     * @param syncDirectory the content sync directory for the resource's project
     * @param resourceLocation the resource location relative to the sync directory
     * @param serializationFilePath the location
     * @return a <tt>ResourceProxy</tt> if there is a covering parent, or null is there is not
     * @throws CoreException
     * @throws IOException
     */
    private ResourceProxy findSerializationDataFromCoveringParent(IResource changedResource, IFolder syncDirectory,
            String resourceLocation, IPath serializationFilePath) throws CoreException, IOException {

        // TODO - this too should be abstracted in the service layer, rather than in the Eclipse-specific code

        Logger logger = Activator.getDefault().getPluginLogger();
        logger.trace("Found plain nt:folder candidate at {0}, trying to find a covering resource for it",
                changedResource.getProjectRelativePath());
        // don't use isRoot() to prevent infinite loop when the final path is '//'
        while (serializationFilePath.segmentCount() != 0) {
            serializationFilePath = serializationFilePath.removeLastSegments(1);
            IFolder folderWithPossibleSerializationFile = (IFolder) syncDirectory.findMember(serializationFilePath);
            if (folderWithPossibleSerializationFile == null) {
                logger.trace("No folder found at {0}, moving up to the next level", serializationFilePath);
                continue;
            }

            // it's safe to use a specific SerializationKind since this scenario is only valid for METADATA_PARTIAL
            // coverage
            String possibleSerializationFilePath = serializationManager.getSerializationFilePath(
                    ((IFolder) folderWithPossibleSerializationFile).getLocation().toOSString(),
                    SerializationKind.METADATA_PARTIAL);

            logger.trace("Looking for serialization data in {0}", possibleSerializationFilePath);

            if (serializationManager.isSerializationFile(possibleSerializationFilePath)) {

                IPath parentSerializationFilePath = Path.fromOSString(possibleSerializationFilePath).makeRelativeTo(
                        syncDirectory.getLocation());
                IFile possibleSerializationFile = syncDirectory.getFile(parentSerializationFilePath);
                if (!possibleSerializationFile.exists()) {
                    logger.trace("Potential serialization data file {0} does not exist, moving up to the next level",
                            possibleSerializationFile.getFullPath());
                    continue;
                }

                
                ResourceProxy serializationData;
                try (InputStream contents = possibleSerializationFile.getContents()) {
                    serializationData = serializationManager.readSerializationData(
                            parentSerializationFilePath.toPortableString(), contents);
                }

                String repositoryPath = serializationManager.getRepositoryPath(resourceLocation);
                String potentialPath = serializationData.getPath();
                boolean covered = serializationData.covers(repositoryPath);

                logger.trace(
                        "Found possible serialization data at {0}. Resource :{1} ; our resource: {2}. Covered: {3}",
                        parentSerializationFilePath, potentialPath, repositoryPath, covered);
                // note what we don't need to normalize the children here since this resource's data is covered by
                // another resource
                if (covered) {
                    return serializationData.getChild(repositoryPath);
                }

                break;
            }
        }

        return null;
    }

    private ResourceProxy buildResourceProxy(String resourceLocation, IResource serializationResource,
            IFolder syncDirectory, String fallbackPrimaryType, Repository repository) throws CoreException, IOException {
        if (serializationResource instanceof IFile) {
            IFile serializationFile = (IFile) serializationResource;
            try (InputStream contents = serializationFile.getContents() ) {
                
                String serializationFilePath = serializationResource.getFullPath()
                        .makeRelativeTo(syncDirectory.getFullPath()).toPortableString();
                ResourceProxy resourceProxy = serializationManager.readSerializationData(serializationFilePath, contents);
                normaliseResourceChildren(serializationFile, resourceProxy, syncDirectory, repository);

                return resourceProxy;
            }
        }

        return new ResourceProxy(serializationManager.getRepositoryPath(resourceLocation), Collections.singletonMap(
                Repository.JCR_PRIMARY_TYPE, (Object) fallbackPrimaryType));
    }

    /**
     * Normalises the of the specified <tt>resourceProxy</tt> by comparing the serialization data and the filesystem
     * data
     * 
     * @param serializationFile the file which contains the serialization data
     * @param resourceProxy the resource proxy
     * @param syncDirectory the sync directory
     * @param repository TODO
     * @throws CoreException
     */
    private void normaliseResourceChildren(IFile serializationFile, ResourceProxy resourceProxy, IFolder syncDirectory,
            Repository repository) throws CoreException {

        // TODO - this logic should be moved to the serializationManager
        try {
            SerializationKindManager skm = new SerializationKindManager();
            skm.init(repository);

            String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
            List<String> mixinTypesList = getMixinTypes(resourceProxy);
            SerializationKind serializationKind = skm.getSerializationKind(primaryType, mixinTypesList);

            if (serializationKind == SerializationKind.METADATA_FULL) {
                return;
            }
        } catch (RepositoryException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed creating a "
                    + SerializationDataBuilder.class.getName(), e));
        }

        IPath serializationDirectoryPath = serializationFile.getFullPath().removeLastSegments(1);

        Iterator<ResourceProxy> childIterator = resourceProxy.getChildren().iterator();
        Map<String, IResource> extraChildResources = new HashMap<>();
        for (IResource member : serializationFile.getParent().members()) {
            if (member.equals(serializationFile)) {
                continue;
            }
            extraChildResources.put(member.getName(), member);
        }

        while (childIterator.hasNext()) {
            ResourceProxy child = childIterator.next();
            String childName = PathUtil.getName(child.getPath());
            String osPath = serializationManager.getOsPath(childName);

            // covered children might have a FS representation, depending on their child nodes, so
            // accept a directory which maps to their name
            extraChildResources.remove(osPath);

            // covered children do not need a filesystem representation
            if (resourceProxy.covers(child.getPath())) {
                continue;
            }

            IPath childPath = serializationDirectoryPath.append(osPath);

            IResource childResource = ResourcesPlugin.getWorkspace().getRoot().findMember(childPath);
            if (childResource == null) {

                Activator.getDefault().getPluginLogger()
                        .trace("For resource at with serialization data {0} the serialized child resource at {1} does not exist in the filesystem and will be ignored",
                                serializationFile, childPath);
                childIterator.remove();
            }
        }

        for ( IResource extraChildResource : extraChildResources.values()) {
            IPath extraChildResourcePath = extraChildResource.getFullPath()
                    .makeRelativeTo(syncDirectory.getFullPath()).makeAbsolute();
            resourceProxy.addChild(new ResourceProxy(serializationManager
                    .getRepositoryPath(extraChildResourcePath.toPortableString())));
            
            Activator.getDefault().getPluginLogger()
                .trace("For resource at with serialization data {0} the found a child resource at {1} which is not listed in the serialized child resources and will be added",
                            serializationFile, extraChildResource);
        }
    }

    private List<String> getMixinTypes(ResourceProxy resourceProxy) {

        Object mixinTypesProp = resourceProxy.getProperties().get(Repository.JCR_MIXIN_TYPES);

        if (mixinTypesProp == null) {
            return Collections.emptyList();
        }

        if (mixinTypesProp instanceof String) {
            return Collections.singletonList((String) mixinTypesProp);
        }

        return Arrays.asList((String[]) mixinTypesProp);
    }

    public Command<?> newCommandForRemovedResources(Repository repository, IResource removed) throws CoreException {
        
        try {
            return removeFileCommand(repository, removed);
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed removing" + removed, e));
        }
    }

    private Command<?> removeFileCommand(Repository repository, IResource resource) throws CoreException, IOException {

        if (resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)) {
            Activator.getDefault().getPluginLogger().trace("Skipping team-private resource {0}", resource);
            return null;
        }

        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        IFolder syncDirectory = ProjectUtil.getSyncDirectory(resource.getProject());

        Filter filter = ProjectUtil.loadFilter(syncDirectory.getProject());

        FilterResult filterResult = getFilterResult(resource, null, filter);
        if (filterResult == FilterResult.DENY || filterResult == FilterResult.PREREQUISITE) {
            return null;
        }
        
        String resourceLocation = getRepositoryPathForDeletedResource(resource,
                ProjectUtil.getSyncDirectoryFile(resource.getProject()));
        
        // verify whether a resource being deleted does not signal that the content structure
        // was rearranged under a covering parent aggregate
        IPath serializationFilePath = Path.fromOSString(serializationManager.getSerializationFilePath(resourceLocation,
                SerializationKind.FOLDER));

        ResourceProxy coveringParentData = findSerializationDataFromCoveringParent(resource, syncDirectory,
                resourceLocation, serializationFilePath);
        if (coveringParentData != null) {
            Activator
                    .getDefault()
                    .getPluginLogger()
                    .trace("Found covering resource data ( repository path = {0} ) for resource at {1},  skipping deletion and performing an update instead",
                            coveringParentData.getPath(), resource.getFullPath());
            FileInfo info = createFileInfo(resource);
            return repository.newAddOrUpdateNodeCommand(new CommandContext(filter), info, coveringParentData);
        }
        
        return repository.newDeleteNodeCommand(serializationManager.getRepositoryPath(resourceLocation));
    }

    public Command<Void> newReorderChildNodesCommand(Repository repository, IResource res) throws CoreException {

        try {
            ResourceAndInfo rai = buildResourceAndInfo(res, repository);

            if (rai == null || rai.isOnlyWhenMissing()) {
                return null;
            }

            return repository.newReorderChildNodesCommand(rai.getResource());
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed reordering child nodes for "
                    + res, e));
        }
    }
}

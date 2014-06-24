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
package org.apache.sling.ide.impl.vlt.serialization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.Aggregator;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.impl.aggregator.FileAggregator;
import org.apache.jackrabbit.vault.fs.impl.aggregator.GenericAggregator;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.jackrabbit.vault.util.MimeTypes;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.impl.vlt.Activator;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.transport.ResourceProxy;

public class VltSerializationDataBuilder implements SerializationDataBuilder {

    private VaultFsLocator fsLocator;
    private SerializationKindManager skm;
	private org.apache.sling.ide.transport.Repository repo;
	private Session session;
	private VaultFileSystem fs;

    public void init(org.apache.sling.ide.transport.Repository repository, File contentSyncRoot)
            throws SerializationException {

    	this.repo = repository;
    	
        try {
            this.skm = new SerializationKindManager();
            this.skm.init(repository);
            if (!contentSyncRoot.exists()) {
            	throw new IllegalArgumentException("contentSyncRoot does not exist: "+contentSyncRoot);
            }

            Repository jcrRepo = RepositoryUtils.getRepository(repo.getRepositoryInfo());
            Credentials credentials = RepositoryUtils.getCredentials(repo.getRepositoryInfo());
            
            session = jcrRepo.login(credentials);

            RepositoryAddress address = RepositoryUtils.getRepositoryAddress(repo.getRepositoryInfo());

            fs = fsLocator.getFileSystem(address, contentSyncRoot, session);
        
        } catch (org.apache.sling.ide.transport.RepositoryException e) {
            throw new SerializationException(e);
        } catch (RepositoryException e) {
            throw new SerializationException(e);
		} catch (IOException e) {
            throw new SerializationException(e);
		} catch (ConfigurationException e) {
            throw new SerializationException(e);
		}
    }
    
    @Override
    public void destroy() {
        if (session != null) {
            session.logout();
        }
    }

    @Override
    public SerializationData buildSerializationData(File contentSyncRoot, ResourceProxy resource) throws SerializationException {

        // TODO - there is a small mismatch here since we're doing remote calls to the repository
        // but taking a resourceProxy - not sure if we'll run into problems down the road or not

        try {

            AggregateWrapper wrapper = findAggregate(resource);

            if (wrapper == null || wrapper.aggregate == null) {
            	//TODO: there are valid cases apparently when aggregate is null and yet there
            	// are children which must be honored.. so we can't throw an exception here
            	// but we should review why this aggregate is null here and if that's valid.
            	System.err.println("No aggregate found for path " + resource.getPath());
            	return null;
            }

            String fileOrFolderPathHint;
            if (wrapper.parent == null) {
                fileOrFolderPathHint = PlatformNameFormat.getPlatformPath(wrapper.aggregate.getPath());
            } else {
                fileOrFolderPathHint = PlatformNameFormat.getPlatformPath(wrapper.parent.getPath()) + ".dir"
                        + File.separatorChar + PlatformNameFormat.getPlatformPath(wrapper.aggregate.getRelPath());
            }

            String nameHint = PlatformNameFormat.getPlatformName(wrapper.aggregate.getName());

            NodeType[] mixinNodeTypes = wrapper.aggregate.getNode().getMixinNodeTypes();
            List<String> mixinNodeTypeNames = new ArrayList<String>(mixinNodeTypes.length);
            for (NodeType nodeType : mixinNodeTypes)
                mixinNodeTypeNames.add(nodeType.getName());

            SerializationKind serializationKind = skm.getSerializationKind(wrapper.aggregate.getNode()
                    .getPrimaryNodeType()
                    .getName(), mixinNodeTypeNames);

            if (resource.getPath().equals("/") || serializationKind == SerializationKind.METADATA_PARTIAL
                    || serializationKind == SerializationKind.FILE || serializationKind == SerializationKind.FOLDER) {
                nameHint = Constants.DOT_CONTENT_XML;
            } else if (serializationKind == SerializationKind.METADATA_FULL) {
                nameHint += ".xml";
                fileOrFolderPathHint += ".xml";
            }

            Activator.getDefault().getPluginLogger()
                    .trace("Got location {0} for path {1}", fileOrFolderPathHint, resource.getPath());

            Aggregator aggregator = fs.getAggregateManager().getAggregator(wrapper.aggregate.getNode(), null);
            if (aggregator instanceof FileAggregator) {
                // TODO - copy-pasted from FileAggregator, and really does not belong here...
                Node content = wrapper.aggregate.getNode();
                if (content.isNodeType(JcrConstants.NT_FILE)) {
                    content = content.getNode(JcrConstants.JCR_CONTENT);
                }
                String mimeType = null;
                if (content.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                    try {
                        mimeType = content.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                    } catch (RepositoryException e) {
                        // ignore
                    }
                }
                if (mimeType == null) {
                    // guess mime type from name
                    mimeType = MimeTypes.getMimeType(wrapper.aggregate.getNode().getName(),
                            MimeTypes.APPLICATION_OCTET_STREAM);
                }

                boolean needsDir = !MimeTypes.matches(wrapper.aggregate.getNode().getName(), mimeType,
                        MimeTypes.APPLICATION_OCTET_STREAM);

                if (!needsDir) {
                    if (content.hasProperty(JcrConstants.JCR_MIXINTYPES)) {
                        for (Value v : content.getProperty(JcrConstants.JCR_MIXINTYPES).getValues()) {
                            if (!v.getString().equals(JcrConstants.MIX_LOCKABLE)) {
                                needsDir = true;
                                break;
                            }
                        }
                    }
                }

                if (!needsDir) {
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
            } else if (aggregator instanceof GenericAggregator) {
                // TODO - copy-pasted from GenericAggregator
                if (wrapper.aggregate.getNode().getPrimaryNodeType().getName().equals("nt:folder")
                        && wrapper.aggregate.getNode().getMixinNodeTypes().length == 0) {
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
            }


            DocViewSerializer s = new DocViewSerializer(wrapper.aggregate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.writeContent(out);
            
            byte[] result = out.toByteArray();

            return new SerializationData(fileOrFolderPathHint, nameHint, result, serializationKind);

        } catch (RepositoryException e) {
            throw new SerializationException(e);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    private AggregateWrapper findAggregate(ResourceProxy resource) throws IOException, RepositoryException {

        VaultFile vaultFile = fs.getFile(resource.getPath());
        String platformPath = resource.getPath();

        if (vaultFile == null) {

            // TODO - not sure why we need to try both ... not a performance impact but ugly nonetheless
            platformPath = PlatformNameFormat.getPlatformPath(resource.getPath()) + VltSerializationManager.EXTENSION_XML;
            vaultFile = fs.getFile(platformPath);

            if (vaultFile == null) {
                platformPath = PlatformNameFormat.getPlatformPath(resource.getPath());
                vaultFile = fs.getFile(platformPath);
            }

            if (vaultFile == null) {
                // TODO proper logging ; discover if this is expected or not and fail hard if it's not
                // this file might be a leaf aggregate of a vaultfile higher in the resource path ; so look for a
                // parent higher

                String parentPath = Text.getRelativeParent(resource.getPath(), 1);
                while (!parentPath.equals("/")) {
                    VaultFile parentFile = fs.getFile(parentPath);
                    if (parentFile != null && parentFile.getAggregate() != null
                            && parentFile.getAggregate().getLeaves() != null) {
                        for (Aggregate leaf : parentFile.getAggregate().getLeaves()) {
                            if (leaf.getPath().equals(resource.getPath())) {

                                return new AggregateWrapper(leaf, parentFile.getAggregate());
                            }
                        }
                    }

                    parentPath = Text.getRelativeParent(parentPath, 1);
                }

                System.err.println("No vaultFile at path " + resource.getPath());
                return null;
            }
        }

        return new AggregateWrapper(vaultFile.getAggregate(), null);
    }

    public void setLocator(VaultFsLocator locator) {

        this.fsLocator = locator;
    }

    static class AggregateWrapper {

        private AggregateWrapper(Aggregate aggregate, Aggregate parent) {

            this.aggregate = aggregate;
            this.parent = parent;
        }

        public Aggregate aggregate;
        public Aggregate parent;
    }

}

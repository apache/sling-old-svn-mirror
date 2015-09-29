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
package org.apache.sling.ide.eclipse.ui.nav.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.apache.sling.ide.eclipse.ui.views.PropertyTypeSupport;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.xml.sax.SAXException;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Namespace;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLTokenizer.Type;

/** WIP: model object for a jcr node shown in the content package view in project explorer **/
public class JcrNode implements IAdaptable {

	private final static WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
	
	final GenericJcrRootFile underlying;

    JcrNode parent;
	
    DirNode dirSibling;

	final List<JcrNode> children = new LinkedList<JcrNode>();

    Element domElement;

	private IResource resource;
	
	private boolean resourceChildrenAdded = false;
	
	final ModifiableProperties properties = new ModifiableProperties(this);

	final Set<JcrNode> hiddenChildren = new HashSet<JcrNode>();
	
	JcrNode() {
		// for subclass use only
		if (this instanceof GenericJcrRootFile) {
			this.underlying = (GenericJcrRootFile) this;
		} else {
			this.underlying = null;
		}
	}
	
	JcrNode(JcrNode parent, Element domElement, IResource resource) {
		this(parent, domElement, parent.underlying, resource);
	}
	
	
	JcrNode(JcrNode parent, Element domElement, GenericJcrRootFile underlying, IResource resource) {
		if (parent == null) {
			throw new IllegalArgumentException("parent must not be null");
		}
		this.parent = parent;
		// domElement can be null
		this.domElement = domElement;
		this.underlying = underlying;
		this.resource = resource;
		parent.addChild(this);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[dom:"+domElement+", file:"+resource+", jcrPath:"+getJcrPath()+"]";
	}
	
	@Override
	public int hashCode() {
		if (underlying==null) {
			if (resource==null) {
				if (domElement==null) {
					return toString().hashCode();
				} else {
				    Element domElementCopy = domElement.copy();
				    domElementCopy.clearChildren();
					return domElementCopy.toString().hashCode() + parent.hashCode();
				}
			} else {
				return resource.getFullPath().hashCode();
			}
		} else {
			if (domElement==null) {
				return underlying.hashCode();
			}
            Element domElementCopy = domElement.copy();
            domElementCopy.clearChildren();
			return underlying.hashCode() + domElementCopy.toString().hashCode();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this==obj) {
			return true;
		}
		if (!(obj instanceof JcrNode)) {
			return false;
		}
		JcrNode other = (JcrNode) obj;
		if (resource!=null && other.resource!=null) {
			if (resource.equals(other.resource)) {
				return true;
			} else {
				return false;
			}
		} else if (resource!=null && other.resource==null) {
			return false;
		} else if (resource==null && other.resource!=null) {
			return false;
		}
		if (other.underlying==null && underlying!=null) {
			return false;
		} else if (other.underlying!=null && underlying==null) {
			return false;
		}
		if (underlying!=null && !underlying.equals(other.underlying)) {
			return false;
		}
		if (parent!=null && other.parent!=null) {
			if (!parent.equals(other.parent)) {
				return false;
			}
			Element domElementCopy = domElement.copy();
			domElementCopy.clearChildren();
			Element otherDomElementCopy = other.domElement.copy();
			otherDomElementCopy.clearChildren();
			return domElementCopy.toString().equals(otherDomElementCopy.toString());
		}
		return toString().equals(obj.toString());
	}

	protected void addChild(JcrNode jcrNode) {
		if (!children.contains(jcrNode)) {
			// check to see if there is a same-named node though
			// that is the dom/resource case
			for (Iterator<JcrNode> it = children.iterator(); it.hasNext();) {
				JcrNode existingChild = it.next();
				if (existingChild.getName().equals(jcrNode.getName())) {
					// then merge the two
					existingChild.setResource(jcrNode.resource);
					return;
				}
			}
			children.add(jcrNode);
		}
	}

	/** shown in the navigator (project explorer) as the label of this element **/
	public String getLabel() {
		if (domElement!=null && resource!=null) {
			return ISO9075.decode(getDomName());// + "[domnode+file]";
		} else if (domElement!=null && resource==null) {
			return ISO9075.decode(getDomName());// + "[domnode]";
		} else if (resource!=null) {
			return resource.getName();//+" [file]";
		} else {
			return "n/a";
		}
	}

	/** shown in the statusbar when this element is selected **/
	public String getDescription() {
		return getJcrPath();
	}

	public boolean hasChildren() {
		boolean members;
		try {
			members = resource!=null && resource instanceof IFolder && ((IFolder)resource).members().length>0;
		} catch (CoreException e) {
			members = false;
		}
		return children.size()>0 || members;
	}
	
	public void hide(JcrNode node) {
		hiddenChildren.add(node);
	}

	Object[] filterHiddenChildren(final Collection<JcrNode> collection, boolean hideEmptyNodes) {
		final Collection<JcrNode> values = new LinkedList<JcrNode>(collection);
		
		for (Iterator<JcrNode> it = hiddenChildren.iterator(); it.hasNext();) {
			final JcrNode hiddenNode = it.next();
			values.remove(hiddenNode);
		}
		if (hideEmptyNodes) {
			for (Iterator<JcrNode> it = values.iterator(); it.hasNext();) {
				JcrNode jcrNode = it.next();
				if (jcrNode.isEmptyNode()) {
					it.remove();
				}
			}
		}
        for (Iterator<JcrNode> it = values.iterator(); it.hasNext();) {
            JcrNode jcrNode = it.next();
            if (jcrNode instanceof DirNode) {
                DirNode dirNode = (DirNode)jcrNode;
                // DirNodes are candidates for hiding
                JcrNode effectiveSibling = dirNode.getEffectiveSibling();
                if (effectiveSibling!=null) {
                    it.remove();
                    continue;
                }
            }
        }
		
		return values.toArray();
	}
	
	private boolean isEmptyNode() {
		if (resource!=null) {
			return false;
		}
		if (children.size()!=0) {
			return false;
		}
		if (domElement==null) {
			return true;
		}
		if (domElement.hasChildren()) {
			return false;
		}
		if (domElement.getAttributes().size()!=0) {
			return false;
		}
		return true;
	}

	public Object[] getChildren(boolean hideEmptyNodes) {
		if (!resourceChildrenAdded) {
			initChildren();
		}
		return filterHiddenChildren(children, true);
	}
	
	void initChildren() {
		try {
			if (resourceChildrenAdded) {
				throw new IllegalStateException("Children already loaded");
			}
			Set<String> childrenNames = new HashSet<String>();
            for (Iterator<JcrNode> it = children.iterator(); it.hasNext();) {
                JcrNode node = it.next();
				childrenNames.add(node.getName());
			}
			
			if (resource!=null && resource instanceof IFolder) {
				IFolder folder = (IFolder)resource;
				IResource[] members = folder.members();
                List<IResource> membersList = new LinkedList<IResource>(Arrays.asList(members));
				outerLoop: while(membersList.size()>0) {
                    for (Iterator<IResource> it = membersList.iterator(); it.hasNext();) {
                        IResource iResource = it.next();
                        if (isDotVltFile(iResource)) {
                            it.remove();
                            continue;
                        }
						if (isVaultFile(iResource)) {
							GenericJcrRootFile gjrf;
                            try {
                                gjrf = new GenericJcrRootFile(this, (IFile)iResource);
                                it.remove();
                                // gjrf.getChildren();
                                gjrf.pickResources(membersList);
                            } catch (XMLParseException e) {
                                // don't try to parse it
                                // errors will be reported by the XML validation infrastructure
                                it.remove();
                            }
							
							// as this might have added some new children, go through the children again and
							// add them if they're not already added
                            for (Iterator<JcrNode> it3 = children.iterator(); it3
									.hasNext();) {
								JcrNode node = (JcrNode) it3.next();
								if (!childrenNames.contains(node.getName())) {
									childrenNames.add(node.getName());
								}
							}
							
							continue outerLoop;
						}
					}
					List<JcrNode> newNodes = new LinkedList<JcrNode>();
                    for (Iterator<IResource> it = membersList.iterator(); it.hasNext();) {
						IResource iResource = (IResource) it.next();
						JcrNode node;
						if (DirNode.isDirNode(iResource)) {
							node = new DirNode(this, (Element)null, iResource);
						} else {
							node = new JcrNode(this, (Element)null, iResource);
						}
						childrenNames.add(node.getName());
						newNodes.add(node);
						it.remove();
					}
				}
			}
			resourceChildrenAdded = true;
		} catch (CoreException e) {
			e.printStackTrace();
			org.apache.sling.ide.eclipse.ui.internal.Activator.getDefault().getPluginLogger().error("Error initializing children: "+e, e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
            org.apache.sling.ide.eclipse.ui.internal.Activator.getDefault().getPluginLogger().error("Error initializing children: "+e, e);
		} catch (SAXException e) {
			e.printStackTrace();
            org.apache.sling.ide.eclipse.ui.internal.Activator.getDefault().getPluginLogger().error("Error initializing children: "+e, e);
		} catch (IOException e) {
			e.printStackTrace();
            org.apache.sling.ide.eclipse.ui.internal.Activator.getDefault().getPluginLogger().error("Error initializing children: "+e, e);
		}
	}

    private boolean isDotVltFile(IResource res) {
        return res.getType() == IResource.FILE && res.getName().equals(".vlt");
    }

    private boolean isVaultFile(IResource iResource) {

        return Activator.getDefault().getSerializationManager()
                .isSerializationFile(iResource.getLocation().toOSString());
	}

	public void setResource(IResource resource) {
		if (this.resource!=null) {
		    if (resource.equals(this.resource)) {
		        return;
		    }
			throw new IllegalStateException("can only set resource once");
		}
		this.resource = resource;
	}

	public Image getImage() {
		boolean plainFolder = resource!=null && (resource instanceof IFolder);
		String primaryType = getProperty("jcr:primaryType").getValueAsString();
		boolean typeFolder = probablyFolderType(primaryType);
		boolean typeFile = primaryType!=null && ((primaryType.equals("nt:file") || primaryType.equals("nt:resource") || primaryType.equals("sling:File")));
		typeFile |= (resource!=null && primaryType==null);
		boolean typeUnstructured = primaryType!=null && ((primaryType.equals("nt:unstructured")));
		
        boolean isVaultFile = resource != null && isVaultFile(resource);
		
		String mimeType = null;
		mimeType = getJcrContentProperty("jcr:mimeType");
		if (mimeType == null) {
			mimeType = getProperty("jcr:mimeType").getValueAsString();
		}
		
		if (typeUnstructured) {
            return WhitelabelSupport.getJcrNodeIcon().createImage();
		} else if (plainFolder || typeFolder) {
			return workbenchLabelProvider.getImage(ProjectUtil.getSyncDirectory(getProject()));
		} else if (typeFile && resource!=null) {
			if (mimeType!=null && mimeType.length()!=0) {
				ImageDescriptor desc = getImageDescriptor(resource.getName(), mimeType);
				if (desc!=null) {
					return desc.createImage();
				}
			}
			if (isVaultFile) {
				return WhitelabelSupport.getJcrNodeIcon().createImage();
			}
			return workbenchLabelProvider.getImage(resource);
		} else {
			if (resource!=null && !isVaultFile) {
				return workbenchLabelProvider.getImage(resource);
			}
			return WhitelabelSupport.getJcrNodeIcon().createImage();
		}
		
	}

    private boolean probablyFolderType(String primaryType) {
        return primaryType != null && 
                (primaryType.equals("nt:folder") || primaryType.equals("sling:Folder"));
    }

	private ImageDescriptor getImageDescriptor(String filename, String jcrMimeType) {
		final String modifiedFilename;
		if (jcrMimeType.equals("image/jpeg")) {
			modifiedFilename = filename + ".jpg";
		} else if (jcrMimeType.contains("/")) {
			modifiedFilename = filename + "." + (jcrMimeType.substring(jcrMimeType.indexOf("/")+1));
		} else {
			return null;
		}
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(modifiedFilename, null);
	}

	private String getJcrContentProperty(String propertyKey) {
		final Object[] chldrn = getChildren(false);
		for (int i = 0; i < chldrn.length; i++) {
			JcrNode jcrNode = (JcrNode) chldrn[i];
			if ("jcr:content".equals(jcrNode.getName())) {
				return jcrNode.getProperty(propertyKey).getValueAsString();
			}
		}
		return null;
	}

	private String getPropertyAsString(String propertyKey) {
		if (properties!=null) {
			Object propertyValue = properties.getValue(propertyKey);
			if (propertyValue!=null) {
				return String.valueOf(propertyValue);
			}
		}
		return null;
	}

	public String getName() {
		if (domElement!=null) {
			return ISO9075.decode(getDomName());
		} else if (resource!=null) {
			return resource.getName();
		} else {
			return "";
		}
	}

	private String getDomName() {
		String domName = domElement.getName();
		Namespace ns = domElement.getNamespace();
		if (ns!=null) {
			String prefix = ns.getPrefix();
			if (!prefix.isEmpty()) {
				domName = prefix + ":" + domName;
			}
		}
		return domName;
	}
		
	public String getJcrPath() {
		String prefix;
		if (parent==null) {
			prefix = "";
		} else {
			prefix = parent.getJcrPath();
			if (!prefix.endsWith("/")) {
				prefix = prefix + "/";
			}
		}
		return prefix + getJcrPathName();
	}

	String getJcrPathName() {
	    if (domElement!=null) {
            return ISO9075.decode(getDomName());
        } else if (resource!=null) {
            if (underlying!=null && resource==underlying.getResource()) {
                // nodename.xml
                IResource res = underlying.getResource();
                String fullname = res.getName();
                if (fullname.endsWith(".xml")) {
                    return fullname.substring(0, fullname.length()-4);
                } else {
                    return res.getName();
                }
            } else {
                return resource.getName();
            }
        } else {
            return "";
	    }
	}

	@Override
	public Object getAdapter(Class adapter) {
        return doGetAdapter(adapter);
	}
	
	private Object doGetAdapter(Class adapter) {
		if (adapter==IActionFilter.class) {
			return new IActionFilter() {
				
				@Override
				public boolean testAttribute(Object target, String name, String value) {
					if (!(target instanceof JcrNode)) {
						return false;
					}
					final JcrNode node = (JcrNode)target;
					if ("domNode".equals(name)) {
						return node.domElement!=null;	
					}
					if ("nonDomNode".equals(name)) {
						return node.domElement==null;	
					}
					if ("browseableNode".equals(name)) {
						return node.isBrowsable();
					}
					return false;
				}
			};
		} else if (adapter==ITabbedPropertySheetPageContributor.class && "christmas".equals("christmas")) {
			return new ITabbedPropertySheetPageContributor() {

				@Override
				public String getContributorId() {
					return "org.eclipse.ui.navigator.ProjectExplorer";
				}
				
			};
		} else if (adapter==IPropertySource.class) {
			return null;//properties;
		} else if (adapter == IFile.class) {
			if (resource instanceof IFile) {
				return (IFile)resource;
			} else {
				return null;
			}
		} else if (adapter == IContributorResourceAdapter.class) {
			//if (resource==null) {
			//	return null;
			//}
			return new IContributorResourceAdapter() {
				
				@Override
				public IResource getAdaptedResource(IAdaptable adaptable) {
					if (!(adaptable instanceof JcrNode)) {
						return null;
					}
					JcrNode node = (JcrNode)adaptable;
					if (node.resource!=null) {
						return node.resource;
					} else {
						return node.underlying.file;
					}
//					return null;
				}
			};
		} else if (adapter == IResource.class) {
			if (resource!=null) {
				return resource;
			} else {
				return null;//underlying.file;
			}
		} else if (adapter == ResourceMapping.class) { 
			boolean t = true;
			if (!t) {
				return null;
			}
//			if (resource==null) {
//				return null;
//			}
			return new ResourceMapping() {
				
				@Override
				public ResourceTraversal[] getTraversals(ResourceMappingContext context,
						IProgressMonitor monitor) throws CoreException {
					if (resource!=null) {
						return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, IResource.DEPTH_INFINITE, IResource.NONE) };
					} else {
						return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { underlying.file }, IResource.DEPTH_INFINITE, IResource.NONE) };
					}
				}
				
				@Override
				public IProject[] getProjects() {
					if (resource!=null) {
						return new IProject[] {resource.getProject()};
					} else {
						return new IProject[] {underlying.file.getProject()};
					}
//					return null;
				}
				
				@Override
				public String getModelProviderId() {
					return "org.apache.sling.ide.eclipse.ui.nav.model.JcrNode.ResourceMapping";
				}
				
				@Override
				public Object getModelObject() {
					if (resource!=null) {
						return resource;
					} else {
						return underlying.file;
					}
				}
			};
		}
		return null;
	}

	public ModifiableProperties getProperties() {
	    return properties;
	}
	
	protected boolean isBrowsable() {
		return true;
	}

	public IFile getFileForEditor() {
	    if ("nt:folder".equals(getPrimaryType())) {
	        // nt:folder doesn't have an underlying file for editor
	        return null;
	    }
	    
		if (resource instanceof IFile) {
			return (IFile)resource;
		}
		
		if (properties!=null && properties.getUnderlying()!=null && properties.getUnderlying().file!=null) {
		    return properties.getUnderlying().file;
		}
		
		if (underlying!=null && underlying.file!=null) {
		    return underlying.file;
		}
		
		org.apache.sling.ide.eclipse.ui.internal.Activator.getDefault().getPluginLogger().warn("No file found for editor for node="+this);
		return null;
	}

    public IResource getResourceForImportExport() {
        String path = getJcrPath();
        StringTokenizer st = new StringTokenizer(path, "/");
        JcrNode root = getParent();
        while(true) {
            JcrNode thisParent = root.getParent();
            if (thisParent==null) {
                break;
            }
            root = thisParent;
        }
        if (!(root instanceof SyncDir)) {
            return null;
        }
        IFolder folder = ((SyncDir)root).getFolder();
        while(st.hasMoreTokens()) {
            String nodeStr = st.nextToken();
            IResource child = folder.findMember(nodeStr);
            if (child==null || !(child instanceof IFolder)) {
                break;
            } else {
                folder = (IFolder) child;
            }
        }
        return folder;
    }

	public void rename(final String string) {
		if (domElement!=null && underlying!=null) {
			domElement.setName(string);
			underlying.save();
		}
		if (resource!=null) {
		    IWorkspaceRunnable r = new IWorkspaceRunnable() {

                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    final IPath fileRenamePath = resource.getParent().getFullPath().append(string);
                    resource.move(fileRenamePath, true, monitor);
                    if (dirSibling!=null) {
                        final IPath dirRenamePath = dirSibling.getResource().getParent().getFullPath().append(string+".dir");
                        dirSibling.getResource().move(dirRenamePath, true, monitor);
                    }
                }
		    };
		    try {
                ResourcesPlugin.getWorkspace().run(r, null);
            } catch (CoreException e) {
                Activator.getDefault().getPluginLogger().error("Error renaming resource ("+resource+"): "+e, e);
            }
		}
	}

	public boolean canBeRenamed() {
        if (parent==null) {
            return false;
        }
	    if (resource!=null) {
            // can be a file or a folder (project is virtually impossible)
	        return true;
	    }
		if (domElement!=null && underlying!=null) {
			return true;
		}
		return false;
	}

	public JcrNode getParent() {
		return parent;
	}

	JcrNode getChild(String name) {
		for (Iterator<JcrNode> it = children.iterator(); it.hasNext();) {
			JcrNode aChild = it.next();
			if (aChild.getName().equals(name)) {
				return aChild;
			}
		}
		return null;
	}

	IResource getResource() {
		return resource;
	}
	
	private SerializationKind getSerializationKind(String nodeType) {
        final SerializationKindManager skm = new SerializationKindManager();
        final Repository repo = ServerUtil.getDefaultRepository(getProject());
        if (repo==null) {
            return getFallbackSerializationKind(nodeType);
        }
        try {
            skm.init(repo);
            //TODO: mixins not yet supported
            return skm.getSerializationKind(nodeType, new ArrayList<String>());
        } catch (RepositoryException e) {
            e.printStackTrace();
            return getFallbackSerializationKind(nodeType);
        }
	}
	
	public void createChild(final String childNodeName, final String childNodeType) {
	    String thisNodeType = getPrimaryType();
	    final SerializationKind parentSk = getSerializationKind(thisNodeType);
        final SerializationKind childSk = getSerializationKind(childNodeType);

        final SerializationManager serializationManager = Activator.getDefault().getSerializationManager();
	    
	    if (parentSk==SerializationKind.METADATA_FULL) {
	        createDomChild(childNodeName, childNodeType);
	    } else if (parentSk==SerializationKind.FILE) {
            throw new IllegalStateException("cannot create child of nt:file");
	    } else if (childSk==SerializationKind.FOLDER) {
            IWorkspaceRunnable r = new IWorkspaceRunnable() {

                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    IFolder newFolder = prepareCreateFolderChild(childNodeName);
                    if (parentSk==SerializationKind.METADATA_PARTIAL) {
                        // when the parent is partial and we're creating a folder here,
                        // then we're running into a SLING-3639 type of problem
                        // the way around this is to make a 'pointer' in the 'root'
                        // .content.xml, and have a directory structure leaving to
                        // the new node, together with a .content.xml describing
                        // the type (unless it's a nt:folder that is)
                        
                        // 1) 'pointer' in the 'root .content.xml'
                        createDomChild(childNodeName, null);
                        
                        // 2) directory structure is created above already
                        // 3) new .content.xml is done below
                    }
                    if (!childNodeType.equals("nt:folder")) {
                        createVaultFile(newFolder, ".content.xml", childNodeType);
                    }
                }
            };
	        
	        try {
	            ResourcesPlugin.getWorkspace().run(r, null);
	            if (childNodeType.equals("nt:folder") && parentSk==SerializationKind.FOLDER) {
	                // trigger a publish, as folder creation is not propagated to 
	                // the SlingLaunchpadBehavior otherwise
	                //TODO: make configurable? Fix in Eclipse/WST?
	                ServerUtil.triggerIncrementalBuild((IFolder)resource, null);
	            }
	        } catch (CoreException e) {
	            Activator.getDefault().getPluginLogger().error("Error creating child "+childNodeName+": "+e, e);
	            e.printStackTrace();
	            MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating node", "Error creating child of "+thisNodeType+" with type "+childNodeType+": "+e);
	            return;
	        }
        } else if ((parentSk == SerializationKind.FOLDER || parentSk == SerializationKind.METADATA_PARTIAL)
                && childSk == SerializationKind.METADATA_FULL) {
            createVaultFile((IFolder) resource, serializationManager.getOsPath(childNodeName) + ".xml", childNodeType);
	    } else if (parentSk==SerializationKind.FOLDER && childSk==SerializationKind.METADATA_PARTIAL) {
//	        createVaultFile((IFolder)resource, childNodeName+".xml", childNodeType);

            IWorkspaceRunnable r = new IWorkspaceRunnable() {

                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    IFolder f = (IFolder)resource;
                    IFolder newFolder = null;
                    newFolder = f.getFolder(serializationManager.getOsPath(childNodeName));
                    newFolder.create(true, true, new NullProgressMonitor());
                    createVaultFile(newFolder, ".content.xml", childNodeType);
                }
            };
            
            try {
                ResourcesPlugin.getWorkspace().run(r, null);
            } catch (CoreException e) {
                Activator.getDefault().getPluginLogger().error("Error creating child "+childNodeName+": "+e, e);
                e.printStackTrace();
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating node", "Error creating child of "+thisNodeType+" with type "+childNodeType+": "+e);
                return;
            }
            
	    } else if (parentSk!=SerializationKind.FOLDER && childSk==SerializationKind.METADATA_PARTIAL) {
            createDomChild(childNodeName, childNodeType);
	    } else {
	        if (childNodeType.equals("nt:file")) {
	            IFolder f = (IFolder)resource;
	            createNtFile(f, childNodeName, childNodeType);
	            return;
	        }
	        //TODO: FILE not yet supported

            Activator.getDefault().getPluginLogger()
                    .error("Cannot create child node of type " + childNodeType + ", serializationKind " + childSk
                            + " under child node of type " + thisNodeType + ", serializationKind " + parentSk);

	        MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Error creating node", "Cannot create child of "+thisNodeType+" with type "+childNodeType+" (yet?)");
	        return;
	    }
	}

    void createDomChild(String childNodeName, String childNodeType) {
        boolean underlyingMatch = underlying==properties.getUnderlying();
        if (domElement==properties.getDomElement()) {
            // normal case
            try{
                createChild(childNodeName, childNodeType, domElement, underlying);
            } catch(Exception e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating new JCR node", "The following error occurred: "+e.getMessage());
            }
        } else {
            // trickier case
            try{
                createChild(childNodeName, childNodeType, properties.getDomElement(), properties.getUnderlying());
            } catch(Exception e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating new JCR node", "The following error occurred: "+e.getMessage());
            }
            
        }
    }

    private void createVaultFile(IFolder parent, String filename, String childNodeType) {
        createVaultFileWithContent(parent, filename, childNodeType, null);
    }

    private void createVaultFileWithContent(IFolder parent, String filename, String childNodeType, Element content) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<jcr:root \n    xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\"\n    xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n    jcr:primaryType=\""+childNodeType+"\"/>";
        final IFile file = parent.getFile(filename);
        try {
            if (content!=null) {
                Document document = TolerantXMLParser.parse(xml, file.getFullPath().toOSString());
                // add the attributes of content
                List<Attribute> attributes = content.getAttributes();
                for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
                    Attribute anAttribute = it.next();
                    if (anAttribute.getName().equals("jcr:primaryType")) {
                        // skip this
                        continue;
                    }
                    document.getRootElement().addAttribute(anAttribute);
                }
                // then copy all the children
                document.getRootElement().addNodes(content.getChildren());
                
                // then save the document
                xml = document.toXML();
            }
            if (file.exists()) {
                file.setContents(new ByteArrayInputStream(xml.getBytes()), true, true, new NullProgressMonitor());
            } else {
                file.create(new ByteArrayInputStream(xml.getBytes()), true, new NullProgressMonitor());
            }
        } catch (Exception e) {
            //TODO proper logging
            e.printStackTrace();
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
                    "Cannot create JCR node on a File", "Following Exception encountered: "+e);
        }
    }

    private void createNtFile(IFolder parent, String filename, String childNodeType) {
        IFile file = parent.getFile(filename);
        if (file.exists()) {
            // file already exists
            return;
        }
        try {
            file.create(new ByteArrayInputStream("".getBytes()), true, new NullProgressMonitor());
        } catch (CoreException e) {
            //TODO proper logging
            e.printStackTrace();
            MessageDialog.openWarning(Display.getDefault().getActiveShell(), 
                    "Cannot create file "+filename, "Following Exception encountered: "+e);
        }
    }

    private SerializationKind getFallbackSerializationKind(String nodeType) {
        if (nodeType.equals("nt:file")) {
            return SerializationKind.FILE;
        } else if (probablyFolderType(nodeType)) {
            return SerializationKind.FOLDER;
        } else {
            return SerializationKind.METADATA_PARTIAL;
        }
    }

	protected void createChild(String nodeName, String nodeType,
			Element domElement, GenericJcrRootFile effectiveUnderlying) {
		if (domElement==null) {
			throw new IllegalArgumentException("domNode must not be null");
		}
		if (effectiveUnderlying==null) {
			throw new IllegalArgumentException("effectiveUnderlying must not be null");
		}
		Element element = new Element(nodeName);
		if (nodeType!=null) {
		    element.addAttribute("jcr:primaryType", nodeType);
		}
		StringBuffer indent = new StringBuffer();
		Element parElement = domElement.getParentElement();
		while(parElement!=null) {
			indent.append("    ");
			parElement = parElement.getParentElement();
		}
		domElement.addNode(new Text("\n    "+indent.toString()));
		element = domElement.addNode(element);
		domElement.addNode(new Text("\n"+indent.toString()));
		JcrNode childNode = new JcrNode(this, element, null);
		effectiveUnderlying.save();
	}

	public boolean canBeDeleted() {
	    if (parent==null) {
	        return false;
	    }
	    if (resource!=null) {
	        // can be a file or a folder (project is virtually impossible)
	        return true;
	    }
        if (domElement!=null && underlying!=null) {
            return true;
        }
        return false;
    }
	
	public void delete() {
		if (parent==null) {
			// then I dont know how to delete
			return;
		}
		parent.children.remove(this);
		if (resource!=null) {
            IWorkspaceRunnable r = new IWorkspaceRunnable() {

                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    resource.delete(true, monitor);
                    if (dirSibling!=null) {
                        dirSibling.getResource().delete(true, monitor);
                    }
                }
            };
            try {
                ResourcesPlugin.getWorkspace().run(r, null);
            } catch (CoreException e) {
                Activator.getDefault().getPluginLogger().error("Error renaming resource ("+resource+"): "+e, e);
            }
		}
		if (domElement!=null) {
			Element parentNode = domElement.getParentElement();
			domElement.remove();
			if (parentNode!=null) {
				List<Node> allChildNodes = parentNode.getNodes();
				boolean nonTextChild = false;
				for (Iterator<Node> it = allChildNodes.iterator(); it
						.hasNext();) {
					Node node = it.next();
					if (node.getType()!=Type.TEXT) {
						nonTextChild = true;
					}					
				}
				if (!nonTextChild) {
					for (Iterator<Node> it = allChildNodes.iterator(); it
							.hasNext();) {
						Node node = it.next();
						it.remove();
					}
					if (!parentNode.hasNodes()) {
						parentNode.setCompactEmpty(true);
					}
				}
			}
		}
		if (underlying!=null) {
		    underlying.save();
		}
	}

	public IProject getProject() {
		if (resource!=null) {
			return resource.getProject();
		}
		if (underlying!=null) {
			return underlying.file.getProject();
		}
		return null;
	}

    public boolean isInContentXml() {
        return domElement!=null;
    }

    public boolean canCreateChild() {
        try {
            final IProject project = getProject();
            final Filter filter = ProjectUtil.loadFilter(project);
            final String relativeFilePath = getJcrPath();
//            final Repository repository = Activator.getDefault().getRepositoryFactory().newRepository(null);//ServerUtil.getRepository(null, null);
//            final RepositoryInfo repositoryInfo = repository.getRepositoryInfo();
//            if (repositoryInfo==null) {
//                return false;
//            }
            if (filter==null) {
                Activator.getDefault().getPluginLogger().error("No filter.xml found for "+project);
                return true;
            } else {
                final FilterResult result = filter.filter(relativeFilePath);
                return result==FilterResult.ALLOW;
            }
        } catch (CoreException e) {
            Logger logger = Activator.getDefault().getPluginLogger();
            logger.error("Could not verify child node allowance: "+this, e);
            return false;
        }
    }

    public String getPrimaryType() {
        final String pt = properties.getValue("jcr:primaryType");
        if (pt!=null && pt.length()!=0) {
            return pt;
        }
        if (resource!=null) {
            if (resource instanceof IContainer) {
                return "nt:folder";
            } else {
                return "nt:file";
            }
        }
        return "";
    }

    public void deleteProperty(String displayName) {
        properties.deleteProperty(displayName);
    }

    public SyncDir getSyncDir() {
        return getParent().getSyncDir();
    }

    public void setPropertyValue(Object key, Object value) {
        properties.setPropertyValue(key, value);
    }

    void changePrimaryType(String newPrimaryType) {
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        if (ntManager == null) {
            MessageDialog.openWarning(null, "Unable to change primary type", "Unable to change primary type since project "
                    + getProject().getName() + " is not associated with a server or the server is not started.");
            return;
        }
        
        try {
            if (!ntManager.isAllowedPrimaryChildNodeType(getParent().getPrimaryType(), newPrimaryType)) {
                if (!MessageDialog.openQuestion(null, "Unable to change primary type", "Parent (type '"+getParent().getPrimaryType()+"')"+
                        " does not accept child with primary type '"+newPrimaryType+"'. Change anyway?")) {
                    return;
                }
            }
        } catch (RepositoryException e1) {
            MessageDialog.openWarning(null, "Unable to change primary type", "Exception occured while trying to "+
                    "verify node types: "+e1);
            return;
        }
        String thisNodeType = getPrimaryType();
        final SerializationKind currentSk = getSerializationKind(thisNodeType);
        final SerializationKind newSk = getSerializationKind(newPrimaryType);
        
        if (currentSk.equals(newSk)) {
            if (newSk!=SerializationKind.FOLDER) {
                // easiest - we should just be able to change the type in the .content.xml
                properties.doSetPropertyValue("jcr:primaryType", newPrimaryType);
            } else {
                if (thisNodeType.equals("nt:folder")) {
                    // switching away from an nt:folder might require creating a .content.xml
                    createVaultFile((IFolder) resource, ".content.xml", newPrimaryType);
                } else if (newPrimaryType.equals("nt:folder")) {
                    // switching *to* an nt:folder also has its challenges..:
                    // 1) it is not allowed to occur within a 'default' and 'full coverage aggregate' node
                    // 2) nt:folder doesn't allow arbitrary children for one
                    // 3)  but it also doesn't have an extra .content.xml - so that one would disappear
                    
                    // 1)
                    if (domElement!=null) {
                        MessageDialog.openWarning(null, "Unable to change primaryType", "Unable to change jcr:primaryType to nt:folder"
                                + " since the node is contained in a .content.xml");
                        return;
                    }
                    
                    
                    // verify 2)
                    if (!verifyNodeTypeChange(ntManager, newPrimaryType)) {
                        return;
                    }
                    if (!(resource instanceof IFolder)) {
                        MessageDialog.openWarning(null, "Unable to change primaryType", "Unable to change jcr:primaryType to nt:folder"
                                + " as there is no underlying folder");
                        return;
                    }
                    IFolder folder = (IFolder)resource;
                    // 3) delete the .content.xml
                    IFile contentXml = folder.getFile(".content.xml");
                    if (contentXml.exists()) {
                        try {
                            contentXml.delete(true, new NullProgressMonitor());
                        } catch (CoreException e) {
                            Logger logger = Activator.getDefault().getPluginLogger();
                            logger.error("Could not delete "+contentXml.getFullPath()+", e="+e, e);
                            MessageDialog.openError(null, "Could not delete file",
                                    "Could not delete "+contentXml.getFullPath()+", "+e);
                        }
                    }
                } else {
                    properties.doSetPropertyValue("jcr:primaryType", newPrimaryType);
                }
            }
            return;
        }
        
        if (newSk==SerializationKind.FOLDER) {
            // switching to a folder
            if (currentSk==SerializationKind.FILE) {
                MessageDialog.openWarning(null, "Unable to change primary type",
                        "Changing from a file to a folder type is currently not supported");
                return;
            }
            if (newPrimaryType.equals("nt:folder")) {
                // verify
                if (!verifyNodeTypeChange(ntManager, newPrimaryType)) {
                    return;
                }
            }
            try {
                // create the new directory structure pointing to 'this'
                IFolder newFolder = getParent().prepareCreateFolderChild(getJcrPathName());
                
                if (!newPrimaryType.equals("nt:folder")) {
                    // move any children from the existing 'this' to a new vault file
                    createVaultFileWithContent(newFolder, ".content.xml", newPrimaryType, domElement);
                }
                
                // remove myself
                if (domElement!=null) {
                    domElement.remove();
                    if (underlying!=null) {
                        underlying.save();
                    }
                }

                // add a pointer in the corresponding .content.xml to point to this (folder) child
                getParent().createDomChild(getJcrPathName(), 
                        null);
                if (newPrimaryType.equals("nt:folder")) {
                    // delete the .content.xml
                    if (properties!=null && properties.getUnderlying()!=null) {
                        IFile contentXml = properties.getUnderlying().file;
                        if (contentXml!=null && contentXml.exists()) {
                            contentXml.delete(true, new NullProgressMonitor());
                        }
                    }
                }
                ServerUtil.triggerIncrementalBuild(newFolder, null);
                return;
            } catch (CoreException e) {
                MessageDialog.openWarning(null, "Unable to change primaryType", "Exception occurred: "+e);
                Logger logger = Activator.getDefault().getPluginLogger();
                logger.error("Exception occurred", e);
                return;
            }
        } else if (newSk==SerializationKind.FILE) {
            MessageDialog.openWarning(null, "Unable to change primary type",
                    "Changing to/from a file is currently not supported");
            return;
        } else {
            // otherwise we're going from a folder to partial-or-full
            if (domElement==null && (resource instanceof IFolder)) {
                createVaultFile((IFolder) resource, ".content.xml", newPrimaryType);
            } else {
                
                // set the "pointer"'s jcr:primaryType
                if (domElement.getAttributeMap().containsKey("jcr:primaryType")) {
                    domElement.setAttribute("jcr:primaryType", newPrimaryType);
                } else {
                    domElement.addAttribute("jcr:primaryType", newPrimaryType);
                }
                
                // then copy all the other attributes - plus children if there are nay
                Element propDomElement = properties.getDomElement();
                if (propDomElement!=null) {
                    List<Attribute> attributes = propDomElement.getAttributes();
                    for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
                        Attribute anAttribute = it.next();
                        if (anAttribute.getName().startsWith("xmlns:")) {
                            continue;
                        }
                        if (anAttribute.getName().equals("jcr:primaryType")) {
                            continue;
                        }
                        if (domElement.getAttributeMap().containsKey(anAttribute.getName())) {
                            domElement.setAttribute(anAttribute.getName(), anAttribute.getValue());
                        } else {
                            domElement.addAttribute(anAttribute);
                        }
                    }
                    List<Element> c2 = propDomElement.getChildren();
                    if (c2!=null && c2.size()!=0) {
                        domElement.addNodes(c2);
                    }
                }
                
                if (properties.getUnderlying()!=null && properties.getUnderlying().file!=null) {
                    try {
                        properties.getUnderlying().file.delete(true, new NullProgressMonitor());
                        // prune empty directories:
                        prune(properties.getUnderlying().file.getParent());
                    } catch (CoreException e) {
                        MessageDialog.openError(null, "Unable to change primary type",
                                "Could not delete vault file "+properties.getUnderlying().file+": "+e);
                        Activator.getDefault().getPluginLogger().error("Error changing jcr:primaryType. Could not delete vault file "+properties.getUnderlying().file+": "+e.getMessage(), e);
                        return;
                    }
                }
                
                underlying.save();
            }
        }
        
    }

    private void prune(IContainer folder) throws CoreException {
        if (folder==null || !(folder instanceof IFolder)) {
            return;
        }
        IFolder f = (IFolder)folder;
        IResource[] members = f.members();
        if (members!=null && members.length!=0) {
            return;
        }
        f.delete(true, new NullProgressMonitor());
        prune(folder.getParent());
    }

    private boolean verifyNodeTypeChange(NodeTypeRegistry ntManager,
            final String newNodeType) {
        Object[] cn = getChildren(true);
        for (int i = 0; i < cn.length; i++) {
            JcrNode node = (JcrNode) cn[i];
            try {
                if (!ntManager.isAllowedPrimaryChildNodeType(newNodeType, node.getPrimaryType())) {
                    MessageDialog.openWarning(null, "Unable to change primaryType", "Unable to change jcr:primaryType to nt:folder"
                            + " since nt:folder cannot have child of type "+node.getPrimaryType());
                    return false;
                }
            } catch (RepositoryException e) {
                Logger logger = Activator.getDefault().getPluginLogger();
                logger.error("Could not determine allowed primary child node types", e);
            }
        }
        return true;
    }

    public void addProperty(String name, String value) {
        properties.addProperty(name, value);
    }

    public NodeType getNodeType() {
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        if (ntManager==null) {
            return null;
        }
        return ntManager.getNodeType(getPrimaryType());
    }

    public int getPropertyType(String propertyName) {
        PropertyDefinition pd = getPropertyDefinition(propertyName);
        if (pd!=null) {
            return pd.getRequiredType();
        }
        
        // otherwise use the SerializationManager to read the
        // underlying vault file and derive the propertyType from there
        GenericJcrRootFile u = properties.getUnderlying();
        if (u==null) {
            // no underlying properties file, that's not good
            Activator.getDefault().getPluginLogger().warn("No underlying properties file, cannot derive propertyType ("+propertyName+") for "+this);
            return -1;
        }
        
        IFolder contentSyncRoot = ProjectUtil.getSyncDirectory(getProject());
        IFile file = (IFile) u.file;
        InputStream contents = null;
        try{
            contents = file.getContents();
            String resourceLocation = file.getFullPath().makeRelativeTo(contentSyncRoot.getFullPath())
                    .toPortableString();
            ResourceProxy resourceProxy = Activator.getDefault()
                    .getSerializationManager().readSerializationData(resourceLocation, contents);
            
            // resourceProxy could be containing a full tree
            // dive into the right position
            String rawValue = properties.getValue(propertyName);
            return PropertyTypeSupport.propertyTypeOfString(rawValue);
        } catch(Exception e) {
            Activator.getDefault().getPluginLogger().warn("Exception occurred during analyzing propertyType ("+propertyName+") for "+this, e);
        } finally {
            IOUtils.closeQuietly(contents);
        }
        return -1;
    }
    
    private Object doGetProperty(ResourceProxy resourceProxy,
            String propertyName) {
        if (resourceProxy.getPath().equals(getJcrPath())) {
            Map<String, Object> props = resourceProxy.getProperties();
            if (props.containsKey(propertyName)) {
                Object p0 = props.get(propertyName);
                return p0;
            }
        } else {
            List<ResourceProxy> resourceProxyChildren = resourceProxy.getChildren();
            for (Iterator<ResourceProxy> it = resourceProxyChildren.iterator(); it
                    .hasNext();) {
                final ResourceProxy aChild = it.next();
                final Object p1 = doGetProperty(aChild, propertyName);
                if (p1!=null) {
                    return p1;
                }
            }
        }
        return null;
    }

    public PropertyDefinition getPropertyDefinition(String propertyName) {
        NodeType nt0 = getNodeType();
        if (nt0==null) {
            return null;
        }
        List<NodeType> nodeTypes = new LinkedList<NodeType>();
        nodeTypes.add(nt0);
        // add all supertypes
        nodeTypes.addAll(Arrays.asList(nt0.getSupertypes()));
        for (Iterator<NodeType> it = nodeTypes.iterator(); it.hasNext();) {
            NodeType nt = it.next();
            PropertyDefinition[] pds = nt.getPropertyDefinitions();
            for (int i = 0; i < pds.length; i++) {
                PropertyDefinition propertyDefinition = pds[i];
                if (propertyDefinition.getName().equals(propertyName)) {
                    return propertyDefinition;
                }
            }
        }
        return null;
    }
    
    public JcrProperty getProperty(final String name) {
        if (properties==null) {
            return null;
        }
        return new JcrProperty() {

            @Override
            public String getName() {
                return name;
            }

            
            @Override
            public int getType() {
                return getPropertyType(name);
            }
            
            @Override
            public String getTypeAsString() {
                int t = getPropertyType(name);
                return PropertyType.nameFromValue(t);
            };

//            @Override
//            public Object getValue() {
//                throw new IllegalStateException("not yet implemented");
//            }
            
            @Override
            public String getValueAsString() {
                String rawValue = getProperties().getValue(name);
                if (rawValue==null) {
                    return null;
                }
                if (rawValue.startsWith("{")) {
                    int curlyEnd = rawValue.indexOf("}", 1);
                    rawValue = rawValue.substring(curlyEnd+1);
                }
                return rawValue;
            }

            @Override
            public boolean isMultiple() {
                String rawValue = getProperties().getValue(name);
                if (rawValue==null) {
                    return false;
                }
                if (rawValue.startsWith("{")) {
                    int curlyEnd = rawValue.indexOf("}", 1);
                    rawValue = rawValue.substring(curlyEnd+1);
                }
                return rawValue.startsWith("[") && rawValue.endsWith("]");
            }
            
            @Override
            public String[] getValuesAsString() {
                String rawValue = getProperties().getValue(name);
                if (rawValue.startsWith("{")) {
                    int curlyEnd = rawValue.indexOf("}", 1);
                    rawValue = rawValue.substring(curlyEnd+1);
                }
                rawValue = rawValue.substring(1, rawValue.length()-1);
                return org.apache.jackrabbit.util.Text.explode(rawValue, ',');
            }
        };
    }

    public void renameProperty(String oldKey, String newKey) {
        properties.renameProperty(oldKey, newKey);
    }

    public void changePropertyType(String key, int propertyType) {
        properties.changePropertyType(key, propertyType);
    }

    private IFolder prepareCreateFolderChild(final String childNodeName)
            throws CoreException {
        // 0) find base folder for creating new subfolders
        List<String> parentNames = new LinkedList<String>();
        JcrNode node = JcrNode.this;
        while(!(node.resource instanceof IFolder) && !(node instanceof SyncDir)) {
            parentNames.add(0, node.getJcrPathName());
            node = node.getParent();
        }
        if (!(node.resource instanceof IFolder)) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not find base folder for creating child. (1) Expected a folder at "+node.resource));
        }
        IFolder folder = (IFolder) node.resource;
        parentNames.add(childNodeName);
        for (Iterator<String> it = parentNames.iterator(); it
                .hasNext();) {
            String aParentName = it.next();
            String encodedParentName = DirNode.encode(aParentName);
            IResource member = folder.findMember(encodedParentName);
            if (member!=null && !(member instanceof IFolder)) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not find base folder for creating child. (2) Expected a folder at "+member));
            }
            if (member!=null && member.exists()) {
                folder = (IFolder) member;
                it.remove();
                continue;
            }
            folder = folder.getFolder(encodedParentName);
            if (!folder.exists()) {
                folder.create(true, true, new NullProgressMonitor());
            }
        }
        
        return folder;
    }

    public IStatus validateDrop(int operation, TransferData transferType) {
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        if (ntManager == null) {
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop element here because corresponding server is not started! (Needed to determine node types)", null);
        }
        // let's support plain files first
        try {
            if (getPrimaryType().equals("nt:file")) {
                // hard-code the most prominent case: cannot drop onto a file
                return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop element onto nt:file", null);
            }
            if (ntManager.isAllowedPrimaryChildNodeType(getPrimaryType(), "nt:file")) {
                return Status.OK_STATUS;
            } else {
                return Status.CANCEL_STATUS;
            }
        } catch (RepositoryException e) {
            Activator.getDefault().getPluginLogger().error("validateDrop: Got Exception while "
                    + "verifying nodeType: "+e, e);
            return Status.CANCEL_STATUS;
        }
    }

    public IStatus handleDrop(Object data, int detail) throws CoreException {
        IFolder folder = (IFolder)this.resource;
        if (data instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection)data;
            Object firstElem = sel.getFirstElement();
            if (firstElem instanceof IResource) {
                IResource resource = (IResource)firstElem;
                return handleDropResource(folder, resource, detail);
            } else if (firstElem instanceof JcrNode) {
                JcrNode node = (JcrNode)firstElem;
                return handleDropNode(folder, node, detail);
            } else {
                return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop on this type of element (yet) [1]", null);
            }
        } else {
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop this type of selection", null);            
        }
    }

    private IStatus handleDropNode(IFolder targetFolder, JcrNode droppedNode, int dropDetail) throws CoreException {
        if (domElement!=null && droppedNode.domElement!=null && droppedNode.resource==null) {
            // then this is the case of moving/copying a dom tree
            domElement.addNodes(droppedNode.domElement.copy());
            underlying.save();
            if (dropDetail==DND.DROP_MOVE) {
                // then delete the original
                droppedNode.delete();
            }
            return Status.OK_STATUS;
        }
        
        if (droppedNode.resource!=null && droppedNode.domElement==null) {
            // this is a pure file/folder based d'n'd
            IStatus status = handleDropResource(targetFolder, droppedNode.resource, dropDetail);
            if (!status.isOK()) {
                return status;
            }
            if (droppedNode.dirSibling!=null) {
                return handleDropResource(targetFolder, droppedNode.dirSibling.getResource(), dropDetail);
            }
            return Status.OK_STATUS;
        }
        //TODO mixed cases are more advanced and not yet supported
        return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop on this (mixed) type of element (yet) [2]", null);
    }

    private IStatus handleDropResource(IFolder targetFolder,
            IResource droppedResourceRoot, int detail) throws CoreException {
        if (targetFolder==null) {
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, 1, "Cannot drop on this type of element (yet)", null);
        }
        if (droppedResourceRoot==null) {
            throw new IllegalArgumentException("droppedResourceRoot must not be null");
        }
        IFile copyToFile = targetFolder.getFile(droppedResourceRoot.getName());
        IPath copyToPath = copyToFile.getFullPath();
        switch (detail) {
        case DND.DROP_COPY: {
            droppedResourceRoot.copy(copyToPath, true,
                    new NullProgressMonitor());
            break;
        }
        case DND.DROP_MOVE: {
            droppedResourceRoot.move(copyToPath, true,
                    new NullProgressMonitor());
            break;
        }
        default: {
            throw new IllegalStateException("Unknown drop action (detail: "
                    + detail + ")");
        }
        }
        return Status.OK_STATUS;
    }

    public IContainer getDropContainer() {
        if (resource instanceof IContainer) {
            return (IContainer) resource;
        } else {
            return null;
        }
    }

    public boolean canBeCopiedToClipboard() {
        if (getPrimaryType().equals("nt:file")) {
            return true;
        } else if (domElement!=null && resource==null) {
            // plain dom node/sub-tree - which we support via XML
            return true;
        } else {
            // everything else: not yet supported
            return false;
        }
    }
    
    public void copyToClipboard(Clipboard clipboard) {
        if (!canBeCopiedToClipboard()) {
            MessageDialog.openWarning(null, "Cannot copy", "Cannot copy this type of node (yet)");
            return;
        }
        if (getPrimaryType().equals("nt:file")) {
            copyFileToClipboard(clipboard);
        } else if (domElement!=null && resource==null) {
            copyDomToClipboard(clipboard);
        }
    }

    private void copyDomToClipboard(Clipboard clipboard) {
        String text = domElement.toXML();
        clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
    }

    private void copyFileToClipboard(Clipboard clipboard) {
        final IResource[] resources;
        final String[] fileNames;
        if (dirSibling==null) {
            resources = new IResource[] {resource};
            final IPath location = resource.getLocation();
            fileNames = (location==null) ? null : new String[] {location.toOSString()};
        } else {
            resources = new IResource[] {resource, dirSibling.getResource()};
            final IPath resLocation = resource.getLocation();
            final IPath siblLocation = dirSibling.getResource().getLocation();
            fileNames = (resLocation==null || siblLocation==null) ? null : new String[] {resLocation.toOSString(), siblLocation.toOSString()};
        }
        if (fileNames!=null) {
            clipboard.setContents(new Object[] { resources, fileNames },
                    new Transfer[] { ResourceTransfer.getInstance(),
                            FileTransfer.getInstance()});
        } else {
            clipboard.setContents(new Object[] { resources }, 
                    new Transfer[] { ResourceTransfer.getInstance() });
        }
    }

    public boolean canBePastedTo(Clipboard clipboard) {
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        
        IResource[] resourceData = (IResource[]) clipboard
                .getContents(ResourceTransfer.getInstance());
        if (resourceData!=null) {
            IContainer container = getDropContainer();
            return container!=null;
        }
        
        String[] fileData = (String[]) clipboard.getContents(FileTransfer.getInstance());
        if (fileData!=null) {
            IContainer container = getDropContainer();
            return container!=null;
        }
        
        String text = (String) clipboard.getContents(TextTransfer.getInstance());
        if (text!=null) {
            return (domElement!=null);
        }
        
        return false;
    }

    /**
     * Paste from the clipboard to this (container) node.
     * <p>
     * Copyright Note: The code of this method was ported from eclipse'
     * PasteAction, which due to visibility restrictions was not reusable.
     * <p>
     * @param clipboard
     */
    public void pasteFromClipboard(Clipboard clipboard) {
        if (!canBePastedTo(clipboard)) {
            // should not occur due to 'canBePastedTo' check done by 
            // corresponding action - checking here nevertheless
            MessageDialog.openInformation(null, "Cannot paste",
                    "No applicable node (type) for pasting found.");
            return;
        }
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        if (ntManager == null) {
            MessageDialog.openWarning(null, "Cannot paste", "Cannot paste if corresponding server is not started");
            return;
        }
        
        // try the resource transfer
        IResource[] resourceData = (IResource[]) clipboard.getContents(ResourceTransfer.getInstance());

        if (resourceData != null && resourceData.length > 0) {
            if (resourceData[0].getType() == IResource.PROJECT) {
                // do not support project pasting onto a jcr node
                MessageDialog.openInformation(null, "Cannot paste project(s)",
                        "Pasting of a project onto a (JCR) node is not possible");
                return;
            } else {
                CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(null);
                operation.copyResources(resourceData, getDropContainer());
            }
            return;
        }

        // try the file transfer
        String[] fileData = (String[]) clipboard.getContents(FileTransfer.getInstance());
        if (fileData != null) {
            CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(null);
            operation.copyFiles(fileData, getDropContainer());
            return;
        }
        
        // then try the text transfer
        String text = (String) clipboard.getContents(TextTransfer.getInstance());
        if ((text!=null) && (this.domElement!=null)) {
            try {
                Document document = TolerantXMLParser.parse(text, "pasted from clipboard");
                this.domElement.addNode(document.getRootElement());
                this.underlying.save();
            } catch (IOException e) {
                MessageDialog.openError(null, "Could not paste from clipboard",
                        "Exception encountered while pasting from clipboard: "+e);
                Activator.getDefault().getPluginLogger().error("Error pasting from clipboard: "+e, e);
            }
        }
    }

}

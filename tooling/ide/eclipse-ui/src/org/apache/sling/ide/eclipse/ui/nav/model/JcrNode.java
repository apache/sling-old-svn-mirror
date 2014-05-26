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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.core.debug.PluginLogger;
import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.wst.server.core.IServer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.pdark.decentxml.Element;
import de.pdark.decentxml.Namespace;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLTokenizer.Type;

/** WIP: model object for a jcr node shown in the content package view in project explorer **/
public class JcrNode implements IAdaptable {

    private final class JcrRootHandler extends DefaultHandler {
		boolean firstElement = true;
		boolean isVaultFile = false;

		public boolean isVaultFile() {
			return isVaultFile;
		}

		@Override
		public void startElement(String uri, String localName,
				String qName, Attributes attributes)
				throws SAXException {
			if (!firstElement) {
				return;
			}
			firstElement = false;
			if ("jcr:root".equals(qName)) {
				String ns = attributes.getValue("xmlns:jcr");
				if (ns!=null && ns.startsWith("http://www.jcp.org/jcr")) {
					// then this is a vault file with a jcr:root at the beginning
					isVaultFile = true;
				}
			}
		}
	}

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
		return "JcrNode[dom:"+domElement+", file:"+resource+", jcrPath:"+getJcrPath()+"]";
	}
	
	@Override
	public int hashCode() {
		if (underlying==null) {
			if (resource==null) {
				if (domElement==null) {
					return toString().hashCode();
				} else {
					return domElement.toString().hashCode() + parent.hashCode();
				}
			} else {
				return resource.getFullPath().hashCode();
			}
		} else {
			if (domElement==null) {
				return underlying.hashCode();
			}
			return underlying.hashCode() + domElement.toString().hashCode();
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
			return domElement.toString().equals(other.domElement.toString());
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
			for (Iterator it = children.iterator(); it.hasNext();) {
				JcrNode node = (JcrNode) it.next();
				childrenNames.add(node.getName());
			}
			
			if (resource!=null && resource instanceof IFolder) {
				IFolder folder = (IFolder)resource;
				IResource[] members = folder.members();
				List<Object> membersList = new LinkedList<Object>(Arrays.asList(members));
				outerLoop: while(membersList.size()>0) {
					for (Iterator it = membersList.iterator(); it.hasNext();) {
						IResource iResource = (IResource) it.next();
						if (isVaultFile(iResource)) {
							GenericJcrRootFile gjrf = new GenericJcrRootFile(this, (IFile)iResource);
							it.remove();
							//gjrf.getChildren();
							gjrf.pickResources(membersList);
							
							// as this might have added some new children, go through the children again and
							// add them if they're not already added
							for (Iterator it3 = children.iterator(); it3
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
					for (Iterator it = membersList.iterator(); it.hasNext();) {
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
					for (Iterator<JcrNode> it = newNodes.iterator(); it
							.hasNext();) {
						JcrNode jcrNode = it.next();
						// load the children - to make sure we get vault files loaded too
						jcrNode.initChildren();
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

	private boolean isVaultFile(IResource iResource)
			throws ParserConfigurationException, SAXException, IOException,
			CoreException {
		if (iResource.getName().endsWith(".xml")) {
			// then it could potentially be a vlt file. 
			// let's check if it contains a jcr:root element with the appropriate namespace
			
			IFile file = (IFile)iResource;
			
		    SAXParserFactory factory = SAXParserFactory.newInstance();
		    SAXParser saxParser = factory.newSAXParser();

		    JcrRootHandler h = new JcrRootHandler();
			saxParser.parse(new InputSource(file.getContents()), h);

			return h.isVaultFile();
		}
		return false;
	}

	public void setResource(IResource resource) {
		if (this.resource!=null) {
			throw new IllegalStateException("can only set resource once");
		}
		this.resource = resource;
	}

	public Image getImage() {
		boolean plainFolder = resource!=null && (resource instanceof IFolder);
		String primaryType = getProperty("jcr:primaryType");
		boolean typeFolder = primaryType!=null && ((primaryType.equals("nt:folder") || primaryType.equals("sling:Folder")));
		boolean typeFile = primaryType!=null && ((primaryType.equals("nt:file") || primaryType.equals("nt:resource") || primaryType.equals("sling:File")));
		typeFile |= (resource!=null && primaryType==null);
		boolean typeUnstructured = primaryType!=null && ((primaryType.equals("nt:unstructured")));
		
		boolean isVaultFile = false;
		try {
			isVaultFile = resource!=null && isVaultFile(resource);
		} catch (Exception e) {
			// this empty catch is okay
		}
		
		String mimeType = null;
		mimeType = getJcrContentProperty("jcr:mimeType");
		if (mimeType == null) {
			mimeType = getProperty("jcr:mimeType");
		}
		
		if (typeUnstructured) {
			return WhitelabelSupport.JCR_NODE_ICON.createImage();
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
				return WhitelabelSupport.JCR_NODE_ICON.createImage();
			}
			return workbenchLabelProvider.getImage(resource);
		} else {
			if (resource!=null && !isVaultFile) {
				return workbenchLabelProvider.getImage(resource);
			}
			return WhitelabelSupport.JCR_NODE_ICON.createImage();
		}
		
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
				return jcrNode.getProperty(propertyKey);
			}
		}
		return null;
	}

	private String getProperty(String propertyKey) {
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
					return "org.apache.sling.ide.eclipse-ui.propertyContributor1";
				}
				
			};
		} else if (adapter==IPropertySource.class) {
			return properties;
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
	
	public void createChild(String childNodeName, String childNodeType) {
	    String thisNodeType = getPrimaryType();
	    final SerializationKind parentSk = getSerializationKind(thisNodeType);
        final SerializationKind childSk = getSerializationKind(childNodeType);
	    
	    if (parentSk==SerializationKind.METADATA_FULL) {
	        createDomChild(childNodeName, childNodeType);
	    } else if (childSk==SerializationKind.FOLDER) {
	        IFolder f = (IFolder)resource;
	        IFolder newFolder = null;
	        try {
	            newFolder = f.getFolder(childNodeName);
	            newFolder.create(true, true, new NullProgressMonitor());
	        } catch (CoreException e) {
	            e.printStackTrace();
	            MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating node", "Error creating child of "+thisNodeType+" with type "+childNodeType+": "+e);
	            return;
	        }
	        
	        if (!childNodeType.equals("nt:folder")) {
	            createVaultFile(newFolder, ".content.xml", childNodeType);
	        } else {
	            // otherwise trigger a publish, as folder creation is not propagated to 
	            // the SlingLaunchpadBehavior otherwise
	            //TODO: make configurable? Fix in Eclipse/WST?
	            ServerUtil.triggerIncrementalBuild(f, null);
	        }
	    } else if (parentSk==SerializationKind.FOLDER && childSk==SerializationKind.METADATA_FULL) {
            createVaultFile((IFolder)resource, childNodeName+".xml", childNodeType);
	    } else if (parentSk==SerializationKind.FOLDER && childSk==SerializationKind.METADATA_PARTIAL) {
//	        createVaultFile((IFolder)resource, childNodeName+".xml", childNodeType);

            IFolder f = (IFolder)resource;
            IFolder newFolder = null;
            try {
                newFolder = f.getFolder(childNodeName);
                newFolder.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                e.printStackTrace();
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating node", "Error creating child of "+thisNodeType+" with type "+childNodeType+": "+e);
                return;
            }
            
            createVaultFile(newFolder, ".content.xml", childNodeType);
	    } else if (parentSk!=SerializationKind.FOLDER && childSk==SerializationKind.METADATA_PARTIAL) {
            createDomChild(childNodeName, childNodeType);
	    } else {
	        //TODO: FILE not yet supported
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
        final String minimalContentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<jcr:root \n    xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\"\n    xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n    jcr:primaryType=\""+childNodeType+"\"/>";
        IFile file = parent.getFile(filename);
        if (file.exists()) {
            System.out.println("then what?");
        }
        try {
            file.create(new ByteArrayInputStream(minimalContentXml.getBytes()), true, new NullProgressMonitor());
        } catch (CoreException e) {
            //TODO proper logging
            e.printStackTrace();
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
                    "Cannot create JCR node on a File", "Following Exception encountered: "+e);
        }
	}

    private SerializationKind getFallbackSerializationKind(String nodeType) {
        if (nodeType.equals("nt:file")) {
            return SerializationKind.FILE;
        } else if (nodeType.equals("nt:folder")) {
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
		element.addAttribute("jcr:primaryType", nodeType);
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

	public String getURLForBrowser(IServer server) {
		final String host = server.getHost();
		final int port = server.getAttribute(ISlingLaunchpadServer.PROP_PORT, 8080);
        final String url = "http://"+host+":"+port+""+getJcrPath();
		return url;
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
                final FilterResult result = filter.filter(ProjectUtil.getSyncDirectoryFile(project), relativeFilePath, null);
                return result==FilterResult.ALLOW;
            }
        } catch (CoreException e) {
            PluginLogger logger = Activator.getDefault().getPluginLogger();
            logger.error("Could not verify child node allowance: "+this, e);
            return false;
        }
    }

    public String getPrimaryType() {
        final String pt = properties.getValue("jcr:primaryType");
        if (pt!=null && pt.length()!=0) {
            return pt;
        }
        if (domElement==null) {
            if (resource!=null) {
                if (resource instanceof IContainer) {
                    return "nt:folder";
                } else {
                    return "nt:file";
                }
            }
        }
        return null;
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

    public void addProperty(String name, String value) {
        properties.addProperty(name, value);
    }

    public NodeType getNodeType() {
        Repository repository = ServerUtil.getDefaultRepository(getProject());
        if (repository==null) {
            return null;
        }
        NodeTypeRegistry ntManager = repository.getNodeTypeRegistry();
        return ntManager.getNodeType(getProperty("jcr:primaryType"));
    }

    public PropertyDefinition getPropertyDefinition(String propertyName) {
        NodeType nt = getNodeType();
        if (nt==null) {
            return null;
        }
        PropertyDefinition[] pds = nt.getPropertyDefinitions();
        for (int i = 0; i < pds.length; i++) {
            PropertyDefinition propertyDefinition = pds[i];
            if (propertyDefinition.getName().equals(propertyName)) {
                return propertyDefinition;
            }
        }
        return null;
    }

}

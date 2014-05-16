package org.apache.sling.ide.impl.vlt;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.TracingCommand;
import org.osgi.service.event.EventAdmin;

/**
 * The <tt>VltRepository</tt> is a Repository implementation backed by <tt>FileVault</tt>
 * 
 */
public class VltRepository implements Repository {

    private final RepositoryInfo repositoryInfo;
    private EventAdmin eventAdmin;
    private NodeTypeRegistry ntRegistry;

    private javax.jcr.Repository jcrRepo;
    private Credentials credentials;
    private boolean markedStopped = false;

    public VltRepository(RepositoryInfo repositoryInfo, EventAdmin eventAdmin) {
        this.repositoryInfo = repositoryInfo;
        this.eventAdmin = eventAdmin;
    }

    public synchronized void markStopped() {
        this.markedStopped = true;
    }

    public synchronized boolean isMarkedStopped() {
        return markedStopped;
    }

    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    public void init() {
        try {
            jcrRepo = RepositoryUtils.getRepository(repositoryInfo);
            credentials = RepositoryUtils.getCredentials(repositoryInfo);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        // loading nodeTypeRegistry:
        getNodeTypeRegistry();
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(FileInfo fileInfo, ResourceProxy resource) {
        return TracingCommand.wrap(new AddOrUpdateNodeCommand(jcrRepo, credentials, fileInfo, resource),
                eventAdmin);
    }

    @Override
    public Command<Void> newDeleteNodeCommand(ResourceProxy resource) {
        return TracingCommand.wrap(new DeleteNodeCommand(jcrRepo, credentials, resource), eventAdmin);
    }

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(String path) {

        return TracingCommand.wrap(new ListChildrenCommand(jcrRepo, credentials, path), eventAdmin);
    }

    @Override
    public Command<ResourceProxy> newGetNodeContentCommand(String path) {

        return TracingCommand.wrap(new GetNodeContentCommand(jcrRepo, credentials, path), eventAdmin);
    }

    @Override
    public Command<byte[]> newGetNodeCommand(String path) {

        return TracingCommand.wrap(new GetNodeCommand(jcrRepo, credentials, path), eventAdmin);
    }

    protected void bindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = eventAdmin;
    }

    protected void unbindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = null;
    }
    
    Command<ResourceProxy> newListTreeNodeCommand(String path, int levels) {

        return TracingCommand.wrap(new ListTreeCommand(jcrRepo, credentials, path, levels, eventAdmin), eventAdmin);
    }
    
    @Override
    public synchronized NodeTypeRegistry getNodeTypeRegistry() {
        if (repositoryInfo==null) {
            throw new IllegalStateException("repositoryInfo must not be null");
        }
        if (ntRegistry!=null) {
            return ntRegistry;
        }
        try {
            ntRegistry = new VltNodeTypeRegistry(this);
        } catch (org.apache.sling.ide.transport.RepositoryException e) {
            throw new RuntimeException(e);
        }
        return ntRegistry;
    }

}

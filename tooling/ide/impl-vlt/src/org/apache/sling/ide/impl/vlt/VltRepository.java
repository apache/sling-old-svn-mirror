package org.apache.sling.ide.impl.vlt;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
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

    private RepositoryInfo repositoryInfo;
    private javax.jcr.Repository jcrRepo;

    private EventAdmin eventAdmin;
    private Credentials credentials;

    @Override
    public void setRepositoryInfo(RepositoryInfo repositoryInfo) {

        this.repositoryInfo = repositoryInfo;

        initJcrRepo();
    }

    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    private void initJcrRepo() {
        try {
            jcrRepo = RepositoryUtils.getRepository(repositoryInfo);
            credentials = RepositoryUtils.getCredentials(repositoryInfo);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(FileInfo fileInfo, ResourceProxy resource) {
        // TODO implement
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

}

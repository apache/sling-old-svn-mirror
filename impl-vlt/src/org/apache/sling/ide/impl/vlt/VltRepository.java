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

    private final RepositoryInfo repositoryInfo;
    private final EventAdmin eventAdmin;

    private javax.jcr.Repository jcrRepo;
    private Credentials credentials;

    public VltRepository(RepositoryInfo repositoryInfo, EventAdmin eventAdmin) {
        this.repositoryInfo = repositoryInfo;
        this.eventAdmin = eventAdmin;
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

}

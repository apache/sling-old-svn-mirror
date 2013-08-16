package org.apache.sling.ide.impl.vlt;

import java.net.URISyntaxException;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
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

    private final RepositoryProvider rp = new RepositoryProvider();
    private Credentials credentials;

    @Override
    public void setRepositoryInfo(RepositoryInfo repositoryInfo) {

        this.repositoryInfo = repositoryInfo;

        initJcrRepo();
    }

    private void initJcrRepo() {
        try {
            // TODO proper error handling
            String url = repositoryInfo.getUrl()+ "server/-/jcr:root/";
            // TODO this should be configurable, or even better - automatically discovered
            RepositoryAddress repositoryAddress = new RepositoryAddress(url);
            jcrRepo = rp.getRepository(repositoryAddress);

            credentials = new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword()
                    .toCharArray());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Command<Void> newAddNodeCommand(FileInfo fileInfo) {
        // TODO implement
        return new NoOpCommand<Void>(jcrRepo, credentials);
    }

    @Override
    public Command<Void> newUpdateContentNodeCommand(FileInfo fileInfo, Map<String, Object> serializationData) {
        // TODO implement
        return new NoOpCommand<Void>(jcrRepo, credentials);
    }

    @Override
    public Command<Void> newDeleteNodeCommand(FileInfo fileInfo) {
        // TODO implement
        return new NoOpCommand<Void>(jcrRepo, credentials);
    }

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(String path) {

        return TracingCommand.wrap(new ListChildrenCommand(jcrRepo, credentials, path), eventAdmin);
    }

    @Override
    public Command<Map<String, Object>> newGetNodeContentCommand(String path) {

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

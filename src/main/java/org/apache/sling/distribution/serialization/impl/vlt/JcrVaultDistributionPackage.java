package org.apache.sling.distribution.serialization.impl.vlt;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.impl.AbstractDistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
/**
 * a JcrPackage based {@link org.apache.sling.distribution.packaging.DistributionPackage}
 */
public class JcrVaultDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private final String type;
    private final JcrPackage jcrPackage;
    private final Session session;

    public JcrVaultDistributionPackage(String type, JcrPackage jcrPackage, Session session) {
        this.type = type;
        this.jcrPackage = jcrPackage;
        this.session = session;
        String[] paths = new String[0];
        try {
            paths = VltUtils.getPaths(jcrPackage.getDefinition().getMetaInf());
        } catch (RepositoryException e) {
            log.error("cannot read paths", e);
        }
        this.getInfo().setPaths(paths);
        this.getInfo().setRequestType(DistributionRequestType.ADD);

    }

    @Nonnull
    public String getId() {
        try {
            return jcrPackage.getPackage().getId().getName();
        } catch (RepositoryException e) {
            log.error("Cannot obtain package id", e);
        } catch (IOException e) {
            log.error("Cannot obtain package id", e);
        }

        return null;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        try {
            return jcrPackage.getData().getBinary().getStream();
        } catch (RepositoryException e) {
            log.error("Cannot create input stream", e);
            throw new IOException();
        }
    }

    public void close() {
        jcrPackage.close();
    }

    public void delete() {
        try {
            Node node = jcrPackage.getNode();

            close();

            node.remove();
            session.save();
        } catch (Throwable e) {
            log.error("Cannot delete package", e);
        }
    }
}

package org.apache.sling.testing.tools.serversetup;

import java.util.Properties;

import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor.ExecutorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SetupPhase that uses a JarExecutor to start
 *  a runnable jar, and stop it at system shutdown
 *  if our SetupServer wants that.
 */
public class StartRunnableJarPhase implements SetupPhase {

    public static final String TEST_SERVER_HOSTNAME = "test.server.hostname";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String id;
    private final String description;
    private final JarExecutor executor;
    
    public StartRunnableJarPhase(final ServerSetup owner, String id, String description, Properties config) throws ExecutorException {
        this.id = id;
        this.description = description;
        executor = new JarExecutor(config);

        String hostname = config.getProperty(TEST_SERVER_HOSTNAME);
        if(hostname == null) {
            hostname = "localhost";
        }
        final String url = "http://" + hostname + ":" + executor.getServerPort();
        log.info("Server base URL={}", url);
        owner.getContext().put(ServerSetup.SERVER_BASE_URL, url);
    }
    
    public String toString() {
        return getClass().getSimpleName() + "(" + id + ")";
    }
    
    /** @inheritDoc */
    public void run(ServerSetup owner) throws Exception {
        executor.start();
    }

    /** @inheritDoc */
    public boolean isStartupPhase() {
        return true;
    }

    /** @inheritDoc */
    public String getDescription() {
        return description;
    }

    /** @inheritDoc */
    public String getId() {
        return id;
    }
    
    /** Return a SetupPhase that kills the process started by this phase */
    public SetupPhase getKillPhase(final String id) {
        return new SetupPhase() {
            public void run(ServerSetup owner) throws Exception {
                executor.stop();
            }

            public boolean isStartupPhase() {
                // This is not a shutdown phase, it's meant to
                // use during startup to forcibly kill an instance
                return true;
            }

            public String getDescription() {
                return "Kill the process started by " + StartRunnableJarPhase.this.getDescription();
            }

            public String getId() {
                return id;
            }
        };
    }
}

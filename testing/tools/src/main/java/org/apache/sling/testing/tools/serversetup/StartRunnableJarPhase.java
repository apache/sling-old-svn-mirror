package org.apache.sling.testing.tools.serversetup;

import java.util.Properties;

import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor.ExecutorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SetupPhase that uses a JarExecutor to start
 *  a runnable jar, and stop it at system shutdown
 *  if our SetupServer wants that.
 */
public class StartRunnableJarPhase implements SetupPhase {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String id;
    private final String description;
    private final JarExecutor executor;
    
    public StartRunnableJarPhase(final ServerSetup owner, String id, String description, Properties config) throws ExecutorException {
        this.id = id;
        this.description = description;
        
        /** Our JarExecutor uses a ProcessDestroyer which does noting
         *  if our ServerSetup owner would not run
         *  a shutdown task with our name + SHUTDOWN_ID_SUFFIX
         */
        final String shutdownId = id + ServerSetup.SHUTDOWN_ID_SUFFIX;
        final ProcessDestroyer destroyer = new ShutdownHookProcessDestroyer() {
            @Override
            public void run() {
                if(owner.getPhasesToRun().contains(shutdownId)) {
                    log.info(
                            "{}: {} allows {} phase to run, shutting down runnable jar", 
                            new Object[] { this, owner, shutdownId } );
                    super.run();
                } else {
                    log.info(
                            "{}: {} does not allow {} phase to run, doing nothing", 
                            new Object[] { this, owner, shutdownId } );
                }
            }
            
        };
        
        executor = new JarExecutor(config) {
            @Override
            protected ProcessDestroyer getProcessDestroyer() {
                return destroyer;
            }
            
        };
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
}

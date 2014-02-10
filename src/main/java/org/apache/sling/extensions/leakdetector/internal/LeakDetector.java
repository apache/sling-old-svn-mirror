package org.apache.sling.extensions.leakdetector.internal;

import java.io.PrintWriter;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeakDetector implements Runnable, BundleActivator {
    /**
     * Set of PhantomReferences such that PhantomReference itself is not GC.
     * While analyzing the Heap Dump it might appear that GC roots of such classloaders (suspected)
     * points to LeakDetector. This happens because they are held here through PhantomReference
     * and there normal GC has not been done. So consider that as false positive
     */
    private final Set<Reference<?>> refs = Collections.synchronizedSet(new HashSet<Reference<?>>());

    /**
     * Lock to control concurrent access to internal data structures
     */
    private final Object leakDetectorLock = new Object();

    private final ReferenceQueue<ClassLoader> queue = new ReferenceQueue<ClassLoader>();

    private final ConcurrentMap<Long, BundleInfo> bundleInfos = new ConcurrentHashMap<Long, BundleInfo>();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Thread referencePoller;

    private BundleContext context;

    private BundleTracker bundleTracker;

    public void start(BundleContext context) {
        this.context = context;
        this.bundleTracker = new LeakDetectorBundleTracker(context);

        referencePoller = new Thread(this, "Bundle Leak Detector Thread");
        referencePoller.setDaemon(true);
        referencePoller.start();

        Dictionary<String,Object> printerProps = new Hashtable<String, Object>();
        printerProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Configuration Printer");
        printerProps.put("felix.webconsole.label", "leakdetector");
        printerProps.put("felix.webconsole.title", "Classloader Leak Detector");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        context.registerService(LeakDetector.class.getName(), this, printerProps);
    }

    public void stop(BundleContext context) {
        this.bundleTracker.close();
        referencePoller.interrupt();
    }

    private class LeakDetectorBundleTracker extends BundleTracker {
        public LeakDetectorBundleTracker(BundleContext context) {
            //Only listen for started
            super(context, Bundle.ACTIVE, null);
            this.open();
        }

        @Override
        public Object addingBundle(Bundle bundle, BundleEvent event) {
            synchronized (leakDetectorLock) {
                registerBundle(bundle);
            }
            return bundle;
        }
    }

    private void registerBundle(Bundle bundle) {
        ClassLoader cl = getClassloader(bundle);
        //cl would be null for Fragment bundle
        if (cl != null) {
            BundleReference ref = new BundleReference(bundle, cl);
            refs.add(ref);

            //Note that a bundle can be started multiple times
            //for e.g. when refreshed So we need to account for that also
            BundleInfo bi = bundleInfos.get(bundle.getBundleId());
            if (bi == null) {
                bi = new BundleInfo(bundle);
                bundleInfos.put(bundle.getBundleId(), bi);
            }
            bi.incrementUsageCount(ref);
            log.info("Registered bundle [{}] with Classloader [{}]", bi, ref.classloaderInfo);
        }
    }

    //~----------------------------------------<GC Callback>

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                BundleReference ref = (BundleReference) queue.remove();
                if (ref != null) {
                    removeBundle(ref);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        log.info("Shutting down reference collector for Classloader LeakDetector");
        //Drain out the queue
        BundleReference ref = null;
        while ((ref = (BundleReference)queue.poll()) != null){
            removeBundle(ref);
        }
    }

    private void removeBundle(BundleReference ref) {
        BundleInfo bi = bundleInfos.get(ref.bundleId);

        synchronized (leakDetectorLock){
            //bi cannot be null
            bi.decrementUsageCount(ref);
            refs.remove(ref);
            ref.clear();
        }

        log.info("Detected garbage collection of bundle [{}] - Classloader [{}]", bi, ref.classloaderInfo);
    }



    //~---------------------------------------<Configuration Printer>

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    @SuppressWarnings("UnusedDeclaration")
    public void printConfiguration(PrintWriter pw) {
        //Try to force GC
        //TODO Should we do by default or let user do it explicitly via
        //Felix Web Console
        //System.gc();

        Set<Long> activeBundleIds = new HashSet<Long>();
        for (Bundle b : context.getBundles()) {
            activeBundleIds.add(b.getBundleId());
        }

        List<BundleInfo> suspiciousBundles = new ArrayList<BundleInfo>(bundleInfos.values());
        Iterator<BundleInfo> itr = suspiciousBundles.iterator();
        while (itr.hasNext()) {
            BundleInfo bi = itr.next();

            //Filter out bundles which are active and have
            //only one classloader created for them
            if (bi.hasSingleInstance()
                    && activeBundleIds.contains(bi.bundleId)) {
                itr.remove();
            }
        }

        if (suspiciousBundles.isEmpty()) {
            pw.println("No classloader leak detected");
        } else {
            pw.println("Possible classloader leak detected");
            pw.printf("Number of suspicious bundles - %d %n", suspiciousBundles.size());
            pw.println();

            final String tab = "    ";

            for(BundleInfo bi : suspiciousBundles){
                pw.printf("* %s %n", bi);
                pw.printf("%s - Bundle Id - %d %n", tab, bi.bundleId);
                pw.printf("%s - Leaked classloaders %n", tab);
                for(ClassloaderInfo ci : bi.leakedClassloaders()){
                    pw.printf("%s%s - %s %n", tab, tab, ci);
                }
            }
        }
    }

    //~---------------------------------------<Data Model>

    private static class BundleInfo {
        final String symbolicName;
        final String version;
        final long bundleId;
        private final Set<ClassloaderInfo> classloaderInfos =
                Collections.synchronizedSet(new HashSet<ClassloaderInfo>());

        public BundleInfo(Bundle b) {
            this.symbolicName = b.getSymbolicName();
            this.version = b.getVersion().toString();
            this.bundleId = b.getBundleId();
        }

        public synchronized void incrementUsageCount(BundleReference ref) {
            classloaderInfos.add(ref.classloaderInfo);
        }

        public synchronized void decrementUsageCount(BundleReference ref) {
            classloaderInfos.remove(ref.classloaderInfo);
        }

        public synchronized boolean hasSingleInstance() {
            return classloaderInfos.size() == 1;
        }

        public synchronized List<ClassloaderInfo> leakedClassloaders(){
            if(hasSingleInstance()){
                return new ArrayList<ClassloaderInfo>(classloaderInfos);
            }else{
                List<ClassloaderInfo> cis = new ArrayList<ClassloaderInfo>(classloaderInfos);
                Collections.sort(cis);

                //Leave out the latest classloader entry as that is
                //associated with running bundle
                return cis.subList(0, cis.size() - 1);
            }
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - Classloader Count [%s]", symbolicName,
                    version, classloaderInfos.size());
        }
    }

    private static class ClassloaderInfo implements Comparable<ClassloaderInfo> {
        final Long creationTime = System.currentTimeMillis();
        /**
         * The hashCode might collide for two different classloaders but then
         * we cannot keep a hard reference to Classloader reference. So at best
         * we keep the systemHashCode and *assume* it is unqiue at least wrt
         * classloader instances
         */
        final long systemHashCode;

        private ClassloaderInfo(ClassLoader cl) {
            this.systemHashCode = System.identityHashCode(cl);
        }

        public int compareTo(ClassloaderInfo o) {
            return creationTime.compareTo(o.creationTime);
        }

        public String getAddress(){
            return Long.toHexString(systemHashCode);
        }

        public String getCreationDate(){
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
            return dateFormat.format(new Date(creationTime));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClassloaderInfo that = (ClassloaderInfo) o;

            if (systemHashCode != that.systemHashCode) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (systemHashCode ^ (systemHashCode >>> 32));
        }

        @Override
        public String toString() {
            return String.format("Identity HashCode - %s, Creation time %s", getAddress(), getCreationDate());
        }
    }

    private class BundleReference extends PhantomReference<ClassLoader> {
        final Long bundleId;
        final ClassloaderInfo classloaderInfo;

        public BundleReference(Bundle bundle, ClassLoader cl) {
            super(cl, queue);
            this.bundleId = bundle.getBundleId();
            this.classloaderInfo = new ClassloaderInfo(cl);
        }
    }

    private static ClassLoader getClassloader(Bundle b) {
        //Somehow it fails to compile on JDK 7. Explicit cast helps
        BundleWiring bw = (BundleWiring) b.adapt(BundleWiring.class);
        if(bw != null){
            return bw.getClassLoader();
        }
        return null;
    }
}

package org.apache.sling.rewriter.impl;

import java.util.Arrays;

import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.rewriter.impl.FactoryCache.ServiceReferenceComparator;
import org.apache.sling.rewriter.impl.FactoryCache.TransformerFactoryEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

final class TransformerFactoryServiceTracker<T> extends HashingServiceTrackerCustomizer<T> {

    private String getMode(final ServiceReference ref) {
        final String mode = (String) ref.getProperty(FactoryCache.PROPERTY_MODE);
        return mode;
    }

    private boolean isGlobal(final ServiceReference ref) {
        return FactoryCache.MODE_GLOBAL.equalsIgnoreCase(this.getMode(ref));
    }

    public static final TransformerFactoryEntry[] EMPTY_ENTRY_ARRAY = new TransformerFactoryEntry[0];
    public static final TransformerFactoryEntry[][] EMPTY_DOUBLE_ENTRY_ARRAY = new TransformerFactoryEntry[][] {EMPTY_ENTRY_ARRAY, EMPTY_ENTRY_ARRAY};

    public static final TransformerFactory[] EMPTY_FACTORY_ARRAY = new TransformerFactory[0];
    public static final TransformerFactory[][] EMPTY_DOUBLE_FACTORY_ARRAY = new TransformerFactory[][] {EMPTY_FACTORY_ARRAY, EMPTY_FACTORY_ARRAY};

    private TransformerFactoryEntry[][] cached = EMPTY_DOUBLE_ENTRY_ARRAY;

    /** flag for cache. */
    private boolean cacheIsValid = true;

    public TransformerFactoryServiceTracker(final BundleContext bc, final String serviceClassName) {
        super(bc, serviceClassName);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(ServiceReference reference) {
        final boolean isGlobal = isGlobal(reference);
        if ( isGlobal ) {
            this.cacheIsValid = false;
        }
        Object obj = super.addingService(reference);
        if ( obj == null && isGlobal ) {
            obj = this.context.getService(reference);
        }
        return obj;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void removedService(ServiceReference reference, Object service) {
        if ( isGlobal(reference) ) {
            this.cacheIsValid = false;
        }
        super.removedService(reference, service);
    }

    /**
     * Get all global transformer factories.
     * @return Two arrays of transformer factories
     */
    public TransformerFactoryEntry[][] getGlobalTransformerFactoryEntries() {
        if ( !this.cacheIsValid ) {
            synchronized ( this ) {
                if ( !this.cacheIsValid ) {
                    final ServiceReference[] refs = this.getServiceReferences();
                    if ( refs == null || refs.length == 0 ) {
                        this.cached = EMPTY_DOUBLE_ENTRY_ARRAY;
                    } else {
                        Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);

                        int preCount = 0;
                        int postCount = 0;
                        for(final ServiceReference ref : refs) {
                            if ( isGlobal(ref) ) {
                                final Object r = ref.getProperty(Constants.SERVICE_RANKING);
                                int ranking = (r instanceof Integer ? (Integer)r : 0);
                                if ( ranking < 0 ) {
                                    preCount++;
                                } else {
                                    postCount++;
                                }
                            }
                        }
                        final TransformerFactoryEntry[][] globalFactories = new TransformerFactoryEntry[2][];
                        if ( preCount == 0 ) {
                            globalFactories[0] = EMPTY_ENTRY_ARRAY;
                        } else {
                            globalFactories[0] = new TransformerFactoryEntry[preCount];
                        }
                        if ( postCount == 0) {
                            globalFactories[1] = EMPTY_ENTRY_ARRAY;
                        } else {
                            globalFactories[1] = new TransformerFactoryEntry[postCount];
                        }
                        int index = 0;
                        for(final ServiceReference ref : refs) {
                            if ( isGlobal(ref) ) {
                                if ( index < preCount ) {
                                    globalFactories[0][index] = new TransformerFactoryEntry((TransformerFactory) this.getService(ref), ref);
                                } else {
                                    globalFactories[1][index - preCount] = new TransformerFactoryEntry((TransformerFactory) this.getService(ref), ref);
                                }
                                index++;
                            }
                        }
                        this.cached = globalFactories;
                    }
                }
                this.cacheIsValid = true;
            }
        }

        return this.cached;
    }

    /**
     * Get all global transformer factories that apply to the current request.
     * @param context The current processing context.
     * @return Two arrays containing the transformer factories.
     */
    public TransformerFactory[][] getGlobalTransformerFactories(final ProcessingContext context) {
        final TransformerFactoryEntry[][] globalFactoryEntries = this.getGlobalTransformerFactoryEntries();
        // quick check
        if ( globalFactoryEntries == EMPTY_DOUBLE_ENTRY_ARRAY ) {
            return EMPTY_DOUBLE_FACTORY_ARRAY;
        }
        final TransformerFactory[][] factories = new TransformerFactory[2][];
        for(int i=0; i<2; i++) {
            if ( globalFactoryEntries[i] == EMPTY_ENTRY_ARRAY ) {
                factories[i] = EMPTY_FACTORY_ARRAY;
            } else {
                factories[i] = new TransformerFactory[globalFactoryEntries[i].length];
                for(int m=0; m<globalFactoryEntries[i].length; m++) {
                    final TransformerFactoryEntry entry = globalFactoryEntries[i][m];
                    if ( entry.match(context) ) {
                        factories[i][m] = entry.factory;
                    }
                }
            }
        }
        return factories;
    }
}
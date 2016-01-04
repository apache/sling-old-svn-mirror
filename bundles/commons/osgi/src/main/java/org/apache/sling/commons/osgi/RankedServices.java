/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import aQute.bnd.annotation.ConsumerType;
import aQute.bnd.annotation.ProviderType;

/**
 * Helper class that collects all services registered via OSGi bind/unbind methods.
 * The services are ordered by service ranking and can be iterated directly using this object instance.
 * Implementation is thread-safe.<br><br>
 * <i>With Declarative Services 1.3 supporting field injection with multiple cardinality (leveraging Collections),
 * this class should only be used if DS 1.3 cannot be used for some reason.
 * DS 1.3 is using the same ordering as {@link ServiceReference#compareTo(Object)}</i>
 * <br><br>
 * <p>Usage example:</p>
 * <p>1. Define a dynamic reference with cardinality OPTIONAL_MULTIPLE in your service:
 * <pre>
 * &#64;Reference(name = "myService", referenceInterface = MyService.class,
 *     cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
 * private final RankedServices&lt;MyService&gt; myServices = new RankedServices&lt;MyService&gt;(Order.DESCENDING);
 * </pre>
 * <p>2. Define bind/unbind methods that delegate to the RankedServices instance:</p>
 * <pre>
 * void bindMyService(MyService service, Map&lt;String, Object&gt; props) {
 *   myServices.bind(service, props);
 * }
 * void unbindMyService(MyService service, Map&lt;String, Object&gt; props) {
 *   myServices.unbind(service, props);
 * }
 * </pre>
 * <p>To access the list of referenced services you can access them in a thread-safe manner:</p>
 * <pre>
 * for (MyService service : myServices) {
 *   // your code...
 * }
 * </pre>
 * <p>Optionally you can pass in a {@link ChangeListener} instance to get notified when the list
 * of referenced services has changed.</p>
 * @param <T> Service type
 * @since 2.3
 * @see "OSGi Compendium 6.0, Declarative Services 1.3, Reference Field Option, $112.3.8.1"
 */
@ProviderType
public final class RankedServices<T> implements Iterable<T> {

  private final ChangeListener changeListener;
  private final SortedMap<Comparable<Object>, T> serviceMap = new TreeMap<Comparable<Object>, T>();
  private volatile List<T> sortedServices = Collections.emptyList();
  private final Order order;

  /**
   * Instantiate without change listener in ascending order (lowest service ranking first).
   * @deprecated Use {@link #RankedService(Order)} to explicitly give the order.
   */
  @Deprecated
  public RankedServices() {
    this(Order.ASCENDING, null);
  }
  
  /**
   * Instantiate with change listener in ascending order (lowest service ranking first).
   * @param changeListener Change listener
   * @deprecated Use {@link #RankedServices(Order order, ChangeListener changeListener)} instead 
   */
  @Deprecated
  public RankedServices(ChangeListener changeListener) {
    this(Order.ASCENDING, changeListener);
  }
  
  /**
   * Instantiate without change listener but with a given order.
   * @param order the order in which the services should be returned in {@link #iterator()} and {@link #get()}. 
   *              Either {@link Order#ASCENDING} or {@link Order#DESCENDING}.
   *              Use {@link Order#DESCENDING} if you want to have the service with the highest ranking returned first
   *              (this is the service which would also be chosen by {@link BundleContext#getServiceReference(String)}).
   * @since 2.4
   */
  public RankedServices(Order order) {
    this(order, null);
  }

  /**
   * Instantiate with change listener.
   * @param order the order in which the services should be returned in {@link #iterator()} and {@link #get()}. 
   *              Either {@link Order#ASCENDING} or {@link Order#DESCENDING}.
   *              Use {@link Order#DESCENDING} if you want to have the service with the highest ranking returned first
   *              (this is the service which would also be chosen by {@link BundleContext#getServiceReference(String)}).
   * @param changeListener Change listener
   * @since 2.4
   */
  public RankedServices(Order order, ChangeListener changeListener) {
    this.order = order;
    this.changeListener = changeListener;
  }

  /**
   * Handle bind service event.
   * @param service Service instance
   * @param props Service reference properties
   */
  public void bind(T service, Map<String, Object> props) {
    synchronized (serviceMap) {
      serviceMap.put(ServiceUtil.getComparableForServiceRanking(props, order), service);
      updateSortedServices();
    }
  }

  /**
   * Handle unbind service event.
   * @param service Service instance
   * @param props Service reference properties
   */
  public void unbind(T service, Map<String, Object> props) {
    synchronized (serviceMap) {
      serviceMap.remove(ServiceUtil.getComparableForServiceRanking(props, order));
      updateSortedServices();
    }
  }

  /**
   * Update list of sorted services by copying it from the array and making it unmodifiable.
   */
  private void updateSortedServices() {
    List<T> copiedList = new ArrayList<T>(serviceMap.values());
    sortedServices = Collections.unmodifiableList(copiedList);
    if (changeListener != null) {
      changeListener.changed();
    }
  }

  /**
   * Lists all services registered in OSGi, sorted by service ranking
   * (either ascending or descending depending on the order given in the constructor).
   * @return Collection of service instances
   * @deprecated Use {@link #getList()} instead
   */
  public Collection<T> get() {
    return sortedServices;
  }
  
  /**
   * Lists all services registered in OSGi, sorted by service ranking
   * (either ascending or descending depending on the order given in the constructor).
   * @return List of service instances
   */
  public List<T> getList() {
    return sortedServices;
  }

  /**
   * Iterates all services registered in OSGi, sorted by service ranking
   * (either ascending or descending depending on the order given in the constructor).
   * @return Iterator with service instances.
   */
  public Iterator<T> iterator() {
    return sortedServices.iterator();
  }

  /**
   * Notification for changes on services list.
   */
  @ConsumerType
  public interface ChangeListener {

    /**
     * Is called when the list of ranked services was changed due to bundle bindings/unbindings.
     * This method is called within a synchronized block, so it's code should be kept as efficient as possible.
     */
    void changed();

  }

}

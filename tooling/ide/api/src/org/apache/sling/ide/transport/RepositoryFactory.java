/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.transport;

/**
 * The <tt>RepositoryFactory</tt> creates new <tt>Repository</tt> instances
 * 
 * <p>
 * Implementations of this interface must be thread-safe.
 * </p>
 *
 */
public interface RepositoryFactory {

    public Repository getRepository(RepositoryInfo repositoryInfo, boolean acceptsDisconnectedRepository) throws RepositoryException;

    /**
     * Returns a <tt>Repository</tt> instance for the specified <tt>repositoryInfo</tt>
     * 
     * <p>
     * As an optimisation, implementations may choose to return the same instance for equivalent repositoryInfo data.
     * </p>
     * 
     * @param repositoryInfo
     * @return
     * @throws RepositoryException
     */
    Repository connectRepository(RepositoryInfo repositoryInfo) throws RepositoryException;

    void disconnectRepository(RepositoryInfo repositoryInfo);
}

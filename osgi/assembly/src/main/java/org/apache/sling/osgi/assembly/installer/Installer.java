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
package org.apache.sling.osgi.assembly.installer;

import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * The <code>Installer</code> interface defines the API provided to install
 * (or update) bundles in the framework either from a defined location (such as
 * an <code>InputStrean</code> or <code>URL</code>) or from a bundle
 * repository.
 */
public interface Installer {

    /**
     * Returns a temporary OSGi Bundle Repository to the system. This bundle
     * repository will only be used for this installer while installing bundles
     * from the OSGi Bundle Repository.
     *
     * @param url The URL to use. This must be an URL to a repository
     *            specification file as defined in the <a
     *            href="http://www2.osgi.org/div/rfc-0112_BundleRepository.pdf">OSGi
     *            RFC 112 Bundle Repository<a/>.
     */
    void addTemporaryRepository(URL url);

    /**
     * The default start level to set on bundles to be installed without any
     * specific startlevel setting.
     *
     * @param startLevel The default start level to set. This must be a positive
     *            number otherwise no explicit start level is set for the
     *            bundle(s) thus defaulting the start level to the default
     *            initial bundle start level of the StartLevel service.
     */
    void setDefaultStartLevel(int startLevel);

    /**
     * Adds a Bundle for installation reading the bundle package from the given
     * <code>source</code> URL.
     *
     * @param location
     * @param source The <code>URL</code> providing access to the bundle to
     *            install.
     * @param startLevel The start level to assign the bundle. If this is zero
     *            or a negative number, the default start level as set by the
     *            {@link #setDefaultStartLevel(int)} method is used (or the
     *            StartLevel service default value).
     */
    void addBundle(String location, URL source, int startLevel);

    /**
     * Adds a Bundle for installation reading the bundle package from the given
     * <code>source</code> InputStream.
     * <p>
     * This method is just for added convenience. The
     * {@link #addBundle(String, URL, int)} method is preferred for installation
     * from a defined URL location.
     *
     * @param location
     * @param source
     * @param startLevel The start level to assign the bundle. If this is zero
     *            or a negative number, the default start level as set by the
     *            {@link #setDefaultStartLevel(int)} method is used (or the
     *            StartLevel service default value).
     */
    void addBundle(String location, InputStream source, int startLevel);

    /**
     * Adds a Bundle for installation to be retrieved from an OSGi Bundle
     * Repository.
     * <p>
     * If the installation of the bundle results in further bundles to be
     * retrieved and installed from the OSGi bundle repository, those bundles
     * will be assigned the default start level as set in the
     * {@link #setDefaultStartLevel(int)} method.
     * <p>
     * This method allows a range of versions to be specified for the selected
     * (primary) bundle to install. This version range is handled as follows:
     * <ul>
     * <li>If the range is <code>null</code>, the most recent version of the
     *      bundle is installed.</li>
     * <li>If the range has both a lower and an upper version limit, the most
     *      recent version within the limits is returned.</li>
     * <li>If the range only has a lower version limit, the most recent version
     *      of the bundle is returned whose major, minor and micro parts of the
     *      bundle version equals the lower version limit.</li>
     * </ul>
     *
     * @param symbolicName The symbolic name of the bundle to install.
     * @param versionRange The range of versions acceptable for the bundle to
     *      install.
     * @param startLevel The start level to assign the bundle. If this is zero
     *            or a negative number, the default start level as set by the
     *            {@link #setDefaultStartLevel(int)} method is used (or the
     *            StartLevel service default value).
     */
    void addBundle(String symbolicName, VersionRange versionRange, int startLevel);

    /**
     * Install the bundles added to this installer via the
     * {@link #addBundle(String, InputStream, int)} and
     * {@link #addBundle(String, URL, int)} methods. Bundles from
     * InputStreams are installed before bundles retrieved from an OSGi Bundle
     * Repository. For each bundle installed, the respective start level is set
     * and the bundles are started (or persistently marked started if the
     * current system start level is lower than a bundle's start level) if the
     * <code>start</code> flag is <code>true</code>.
     *
     * @param start <code>true</code> if the bundles should be started after
     *            the installation.
     * @retrun An array of bundles installed. This is the complete list of
     *         bundles installed, which may exceed the bundles added through the
     *         {@link #addBundle(String, InputStream, int)} and
     *         {@link #addBundle(String, URL, int)} method as some bundles
     *         might have been added to resolve dependencies. If no bundles have
     *         been added, this method returns <code>null</code>.
     * @throws InstallerException
     */
    Bundle[] install(boolean start) throws InstallerException;

    /**
     * Releases any resources held by this installer object. That is, internal
     * caches will be cleared, any open <code>InputStream</code>s received by
     * the {@link #addBundle(String, InputStream, int)} method and not used
     * during an installation are closed and any locks still held are released.
     * <p>
     * Users of this class are strongly encouraged to call this method when done
     * with the installer.
     */
    void dispose();
}

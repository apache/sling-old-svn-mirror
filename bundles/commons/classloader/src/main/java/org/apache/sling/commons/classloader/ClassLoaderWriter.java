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
package org.apache.sling.commons.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The class loader writer allows to modify the resources loaded by a
 * {@link DynamicClassLoaderProvider}. For example a class loader writer
 * could write generated class files into the repository or the temporary
 * file system.
 */
public interface ClassLoaderWriter {

    OutputStream getOutputStream(String name);

    InputStream getInputStream(String name) throws IOException;

    long getLastModified(String name);

    boolean delete(String name);

    boolean rename(String oldName, String newName);
}

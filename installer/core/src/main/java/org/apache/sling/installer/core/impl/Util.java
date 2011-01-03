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
package org.apache.sling.installer.core.impl;

import java.io.IOException;
import java.lang.reflect.Field;

public class Util {

    /** Set a (final) field during deserialization. */
    public static void setField(final Object obj, final String name, final Object value)
    throws IOException {
        try {
            final Field field = obj.getClass().getDeclaredField(name);
            if ( field == null ) {
                throw new IOException("Field " + name + " not found in class " + obj.getClass());
            }
            field.setAccessible(true);
            field.set(obj, value);
        } catch (final SecurityException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final NoSuchFieldException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final IllegalArgumentException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final IllegalAccessException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }
}

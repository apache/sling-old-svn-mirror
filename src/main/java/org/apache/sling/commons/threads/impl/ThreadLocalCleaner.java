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
package org.apache.sling.commons.threads.impl;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.sling.commons.threads.impl.ThreadLocalChangeListener.Mode;

/** Notifies a {@link ThreadLocalChangeListener} about changes on a thread local storage. In addition it removes all references to variables
 * being added to the thread local storage while the cleaner was running with its {@link cleanup} method.
 * 
 * @see <a href="http://www.javaspecialists.eu/archive/Issue229.html">JavaSpecialist.eu - Cleaning ThreadLocals</a> */
public class ThreadLocalCleaner {
    private final ThreadLocalChangeListener listener;

    /* Reflection fields */
    /** this field is in class {@link ThreadLocal} and is of type {@code ThreadLocal.ThreadLocalMap} */
    private static Field threadLocalsField;
    /** this field is in class {@link ThreadLocal} and is of type {@code ThreadLocal.ThreadLocalMap} */
    private static Field inheritableThreadLocalsField;
    private static Class<?> threadLocalMapClass;
    /** this field is in class {@code ThreadLocal.ThreadLocalMap} and contains an array of {@code ThreadLocal.ThreadLocalMap.Entry's} */
    private static Field tableField;
    private static Class<?> threadLocalMapEntryClass;
    /** this field is in class {@code ThreadLocal.ThreadLocalMap.Entry} and contains an object referencing the actual thread local
     * variable */
    private static Field threadLocalEntryValueField;
    private static IllegalStateException reflectionException;

    public ThreadLocalCleaner(ThreadLocalChangeListener listener) {
        if (threadLocalsField == null) {
            initReflectionFields();
        }
        this.listener = listener;
        saveOldThreadLocals();
    }

    private static synchronized void initReflectionFields() throws IllegalStateException {
        // check if previous initialization lead to an exception
        if (reflectionException != null) {
            throw reflectionException;
        }
        // check if initialized
        if (threadLocalsField == null) {
            try {
                threadLocalsField = field(Thread.class, "threadLocals");
                inheritableThreadLocalsField = field(Thread.class, "inheritableThreadLocals");
                threadLocalMapClass = inner(ThreadLocal.class, "ThreadLocalMap");
                tableField = field(threadLocalMapClass, "table");
                threadLocalMapEntryClass = inner(threadLocalMapClass, "Entry");
                threadLocalEntryValueField = field(threadLocalMapEntryClass, "value");
            } catch (NoSuchFieldException e) {
                reflectionException = new IllegalStateException(
                        "Could not locate threadLocals field in class Thread.  " +
                                "Will not be able to clear thread locals: " + e);
                throw reflectionException;
            }
        }
    }

    public void cleanup() {
        // the first two diff calls are only to notify the listener, the actual cleanup is done by restoreOldThreadLocals
        diff(threadLocalsField, copyOfThreadLocals.get());
        diff(inheritableThreadLocalsField, copyOfInheritableThreadLocals.get());
        restoreOldThreadLocals();
    }

    /** Notifies the {@link ThreadLocalChangeListener} about changes on thread local variables for the current thread.
     * 
     * @param field
     * @param backup */
    private void diff(Field field, Reference<?>[] backup) {
        try {
            Thread thread = Thread.currentThread();
            Object threadLocals = field.get(thread);
            if (threadLocals == null) {
                if (backup != null) {
                    for (Reference<?> reference : backup) {
                        changed(thread, reference, Mode.REMOVED);
                    }
                }
                return;
            }

            Reference<?>[] current = (Reference<?>[]) tableField.get(threadLocals);
            if (backup == null) {
                for (Reference<?> reference : current) {
                    changed(thread, reference, Mode.ADDED);
                }
            } else {
                // nested loop - both arrays *should* be relatively small
                next: for (Reference<?> curRef : current) {
                    if (curRef != null) {
                        if (curRef.get() == copyOfThreadLocals ||
                                curRef.get() == copyOfInheritableThreadLocals) {
                            continue next;
                        }
                        for (Reference<?> backupRef : backup) {
                            if (curRef == backupRef)
                                continue next;
                        }
                        // could not find it in backup - added
                        changed(thread, curRef, Mode.ADDED);
                    }
                }
                next: for (Reference<?> backupRef : backup) {
                    for (Reference<?> curRef : current) {
                        if (curRef == backupRef)
                            continue next;
                    }
                    // could not find it in current - removed
                    changed(thread, backupRef, Mode.REMOVED);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access denied", e);
        }
    }

    private void changed(Thread thread, Reference<?> reference,
            ThreadLocalChangeListener.Mode mode)
            throws IllegalAccessException {
        listener.changed(mode,
                thread, (ThreadLocal<?>) reference.get(),
                threadLocalEntryValueField.get(reference));
    }

    /** @param c the class containing the field
     * @param name the name of the field
     * @return the field from the given class with the given name (made accessible)
     * @throws NoSuchFieldException */
    private static Field field(Class<?> c, String name)
            throws NoSuchFieldException {
        Field field = c.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /** @param clazz the class containing the inner class
     * @param name the name of the inner class
     * @return the class with the given name, declared as inner class of the given class */
    private static Class<?> inner(Class<?> clazz, String name) {
        for (Class<?> c : clazz.getDeclaredClasses()) {
            if (c.getSimpleName().equals(name)) {
                return c;
            }
        }
        throw new IllegalStateException(
                "Could not find inner class " + name + " in " + clazz);
    }

    private static final ThreadLocal<Reference<?>[]> copyOfThreadLocals = new ThreadLocal<>();

    private static final ThreadLocal<Reference<?>[]> copyOfInheritableThreadLocals = new ThreadLocal<>();

    private static void saveOldThreadLocals() {
        copyOfThreadLocals.set(copy(threadLocalsField));
        copyOfInheritableThreadLocals.set(copy(inheritableThreadLocalsField));
    }

    private static Reference<?>[] copy(Field field) {
        try {
            Thread thread = Thread.currentThread();
            Object threadLocals = field.get(thread);
            if (threadLocals == null)
                return null;
            Reference<?>[] table = (Reference<?>[]) tableField.get(threadLocals);
            return Arrays.copyOf(table, table.length);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access denied", e);
        }
    }

    private static void restoreOldThreadLocals() {
        try {
            restore(inheritableThreadLocalsField, copyOfInheritableThreadLocals.get());
            restore(threadLocalsField, copyOfThreadLocals.get());
        } finally {
            copyOfThreadLocals.remove();
            copyOfInheritableThreadLocals.remove();
        }
    }

    private static void restore(Field field, Object value) {
        try {
            Thread thread = Thread.currentThread();
            if (value == null) {
                field.set(thread, null);
            } else {
                tableField.set(field.get(thread), value);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access denied", e);
        }
    }

    static {
        // TODO: move to a place where the exception can be caught!

    }
}
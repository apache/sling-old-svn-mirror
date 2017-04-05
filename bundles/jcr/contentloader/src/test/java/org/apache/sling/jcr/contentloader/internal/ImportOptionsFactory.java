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
package org.apache.sling.jcr.contentloader.internal;

import org.apache.sling.jcr.contentloader.ContentReader;
import org.apache.sling.jcr.contentloader.ImportOptions;

import java.util.Map;

public final class ImportOptionsFactory {
    public static ImportOptions createImportOptions(final boolean isOverwrite, final boolean isPropertyOverwrite,
            final boolean isAutoCheckout, final boolean isCheckin, final boolean isIgnoredImportProvider){
        return new ImportOptions() {
            @Override
            public boolean isOverwrite() {
                return isOverwrite;
            }

            @Override
            public boolean isPropertyOverwrite() {
                return isPropertyOverwrite;
            }

            @Override
            public boolean isAutoCheckout() {
                return isAutoCheckout;
            }

            @Override
            public boolean isCheckin() {
                return isCheckin;
            }

            @Override
            public boolean isIgnoredImportProvider(String extension) {
                return isIgnoredImportProvider;
            }
        };
    }

    public static ImportOptions createImportOptsWithReaders(final boolean isOverwrite, final boolean isPropertyOverwrite,
            final boolean isAutoCheckout, final boolean isCheckin, final Map<String, ContentReader> defaultContentReaders){
        return new ImportOptions() {
            @Override
            public boolean isOverwrite() {
                return isOverwrite;
            }

            @Override
            public boolean isPropertyOverwrite() {
                return isPropertyOverwrite;
            }

            @Override
            public boolean isAutoCheckout() {
                return isAutoCheckout;
            }

            @Override
            public boolean isCheckin() {
                return isCheckin;
            }

            @Override
            public boolean isIgnoredImportProvider(String extension) {
                return defaultContentReaders.containsKey(extension);
            }
        };
    }
}

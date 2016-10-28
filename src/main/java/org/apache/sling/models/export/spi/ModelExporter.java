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
package org.apache.sling.models.export.spi;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.models.factory.ExportException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;

/**
 * SPI interface for model exporters.
 */
@ConsumerType
public interface ModelExporter {

    /**
     * Check if the result class is supported by this exporter.
     *
     * @param clazz the result class
     * @return true if the result class is supported
     */
    boolean isSupported(@Nonnull Class<?> clazz);

    /**
     * Export the provided model to the defined class using the options.
     *
     * @param model the model class
     * @param clazz the export type
     * @param options export options
     * @param <T> the export type
     * @return an exported object
     * @throws ExportException if the export is not successful
     */
    @CheckForNull <T> T export(@Nonnull Object model, @Nonnull Class<T> clazz, @Nonnull Map<String, String> options) throws ExportException;

    /**
     * The name of the exporter.
     * @return the name of the exporter
     */
    @Nonnull String getName();

}

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
package org.apache.sling.models.jacksonexporter.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import com.fasterxml.jackson.databind.MapperFeature;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.models.export.spi.ModelExporter;
import org.apache.sling.models.factory.ExportException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class JacksonExporter implements ModelExporter {

    private static final Logger log = LoggerFactory.getLogger(JacksonExporter.class);

    private static final String SERIALIZATION_FEATURE_PREFIX = SerializationFeature.class.getSimpleName() + ".";

    private static final int SERIALIZATION_FEATURE_PREFIX_LENGTH = SERIALIZATION_FEATURE_PREFIX.length();

    private static final String MAPPER_FEATURE_PREFIX = MapperFeature.class.getSimpleName() + ".";

    private static final int MAPPER_FEATURE_PREFIX_LENGTH = MAPPER_FEATURE_PREFIX.length();

    @Override
    public boolean isSupported(Class<?> clazz) {
        return clazz.equals(String.class) || clazz.equals(Map.class);
    }

    @Override
    public <T> T export(Object model, Class<T> clazz, Map<String, String> options)
            throws ExportException {
        ObjectMapper mapper = new ObjectMapper();
        for (Map.Entry<String, String> optionEntry : options.entrySet()) {
            String key = optionEntry.getKey();
            if (key.startsWith(SERIALIZATION_FEATURE_PREFIX)) {
                String enumName = key.substring(SERIALIZATION_FEATURE_PREFIX_LENGTH);
                try {
                    SerializationFeature feature = SerializationFeature.valueOf(enumName);
                    mapper.configure(feature, Boolean.valueOf(optionEntry.getValue()));
                } catch (IllegalArgumentException e) {
                    log.warn("Bad SerializationFeature option");
                }
            } else if (key.startsWith(MAPPER_FEATURE_PREFIX)) {
                String enumName = key.substring(MAPPER_FEATURE_PREFIX_LENGTH);
                try {
                    MapperFeature feature = MapperFeature.valueOf(enumName);
                    mapper.configure(feature, Boolean.valueOf(optionEntry.getValue()));
                } catch (IllegalArgumentException e) {
                    log.warn("Bad SerializationFeature option");
                }
            }
        }

        if (clazz.equals(Map.class)) {
            return (T) mapper.convertValue(model, Map.class);
        } else if (clazz.equals(String.class)) {
            final JsonFactory f = new JsonFactory();
            f.setCharacterEscapes(new EscapeCloseScriptBlocks());
            StringWriter writer = new StringWriter();
            JsonGenerator jgen;
            final boolean printTidy;
            if (options.containsKey("tidy")) {
                printTidy = Boolean.valueOf(options.get("tidy"));
            } else {
                printTidy = false;
            }
            try {
                jgen = f.createGenerator(writer);
                if (printTidy) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(jgen, model);
                } else {
                    mapper.writeValue(jgen, model);
                }
            } catch (final IOException e) {
                throw new ExportException(e);
            }
            return (T) writer.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return "jackson";
    }

    private static class EscapeCloseScriptBlocks extends CharacterEscapes {
        private final int[] escapes;

        public EscapeCloseScriptBlocks() {
            int[] baseEscapes = standardAsciiEscapesForJSON();
            baseEscapes['<'] = CharacterEscapes.ESCAPE_STANDARD;
            baseEscapes['>'] = CharacterEscapes.ESCAPE_STANDARD;
            escapes = baseEscapes;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapes;
        }

        @Override
        public SerializableString getEscapeSequence(final int arg0) {
            return null;
        }
    }
}

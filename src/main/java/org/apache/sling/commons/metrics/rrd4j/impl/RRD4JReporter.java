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
package org.apache.sling.commons.metrics.rrd4j.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static java.lang.String.join;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class RRD4JReporter extends ScheduledReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RRD4JReporter.class);

    private static final String PROPERTIES_SUFFIX = ".properties";
    static final int DEFAULT_STEP = 5;

    private final Map<String, Integer> dictionary = new HashMap<>();
    private final RrdDb rrdDB;

    static Builder forRegistry(MetricRegistry metricRegistry) {
        return new Builder(metricRegistry);
    }

    static class Builder {
        private MetricRegistry metricRegistry;
        private TimeUnit ratesUnit;
        private TimeUnit durationUnit;
        private File path = new File(".");
        private final List<String> indexedDS = new ArrayList<>();
        private final Map<String, Integer> dictionary = new HashMap<>();
        private final List<String> archives = new ArrayList<>();
        private int step = DEFAULT_STEP;

        Builder(MetricRegistry metricRegistry ) {
            this.metricRegistry = metricRegistry;
        }

        Builder withPath(File path) {
            this.path = path;
            return this;
        }

        Builder withDatasources(String[] datasources) {
            this.indexedDS.clear();
            this.dictionary.clear();

            int i = 0;
            for (String ds : datasources) {
                String[] tokens = ds.split(":");
                if (tokens.length != 6) {
                    throw new IllegalArgumentException("Invalid data source definition: " + ds);
                }
                dictionary.put(normalize(tokens[1]), i);
                tokens[1] = String.valueOf(i);
                this.indexedDS.add(checkDataSource(join(":", tokens)));
                i++;
            }
            return this;
        }

        Builder withArchives(String[] archives) {
            this.archives.clear();
            this.archives.addAll(Arrays.asList(archives));
            return this;
        }

        Builder withStep(int step) {
            this.step = step;
            return this;
        }

        Builder convertRatesTo(TimeUnit ratesUnit) {
            this.ratesUnit = ratesUnit;
            return this;
        }

        Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        ScheduledReporter build() throws IOException {
            return new RRD4JReporter(metricRegistry, "RRD4JReporter",
                    MetricFilter.ALL, ratesUnit, durationUnit, dictionary, createDef());
        }

        private String checkDataSource(String ds)
                throws IllegalArgumentException {
            new RrdDef("path").addDatasource(ds);
            return ds;
        }

        private RrdDef createDef() {
            RrdDef def = new RrdDef(path.getPath());
            def.setStep(step);
            for (String ds : indexedDS) {
                def.addDatasource(ds);
            }
            for (String rra : archives) {
                def.addArchive(rra);
            }
            return def;
        }
    }

    RRD4JReporter(MetricRegistry registry,
                  String name,
                  MetricFilter filter,
                  TimeUnit rateUnit,
                  TimeUnit durationUnit,
                  Map<String, Integer> dictionary,
                  RrdDef rrdDef) throws IOException {
        super(registry, name, filter, rateUnit, durationUnit);
        this.dictionary.putAll(dictionary);
        this.rrdDB = createDB(rrdDef);
        storeDictionary(rrdDef.getPath() + PROPERTIES_SUFFIX);
    }

    @Override
    public void close() {
        try {
            rrdDB.close();
        } catch (IOException e) {
            LOGGER.warn("Closing RRD failed", e);
        }
        super.close();
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        long time = System.nanoTime();
        int total = gauges.size() + counters.size() + histograms.size() + meters.size() + timers.size();
        int reported = 0;
        try {
            Sample sample = rrdDB.createSample(System.currentTimeMillis() / 1000);
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                reported += update(sample, indexForName(entry.getKey()), entry.getValue());
            }

            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                reported += update(sample, indexForName(entry.getKey()), entry.getValue());
            }

            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                reported += update(sample, indexForName(entry.getKey()), entry.getValue());
            }

            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                reported += update(sample, indexForName(entry.getKey()), entry.getValue());
            }

            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                reported += update(sample, indexForName(entry.getKey()), entry.getValue());
            }
            sample.update();
        } catch (IOException e) {
            LOGGER.warn("Unable to write sample to RRD", e);
        } finally {
            time = System.nanoTime() - time;
            LOGGER.debug("{} out of {} metrics reported in {} \u03bcs",
                    reported, total, TimeUnit.NANOSECONDS.toMicros(time));
        }
    }

    private int indexForName(String name) {
        Integer idx = dictionary.get(normalize(name));
        return idx != null ? idx : -1;
    }

    private static String normalize(String name) {
        return name.replaceAll(":", "_");
    }

    private int update(Sample sample, int nameIdx, Gauge g) {
        if (nameIdx < 0) {
            return 0;
        }
        Object value = g.getValue();
        if (value instanceof Number) {
            sample.setValue(nameIdx, ((Number) value).doubleValue());
            return 1;
        }
        return 0;
    }

    private int update(Sample sample, int nameIdx, Counter c) {
        if (nameIdx < 0) {
            return 0;
        }
        sample.setValue(nameIdx, c.getCount());
        return 1;
    }

    private int update(Sample sample, int nameIdx, Histogram h) {
        if (nameIdx < 0) {
            return 0;
        }
        sample.setValue(nameIdx, h.getCount());
        return 1;
    }

    private int update(Sample sample, int nameIdx, Timer t) {
        if (nameIdx < 0) {
            return 0;
        }
        sample.setValue(nameIdx, t.getCount());
        return 1;
    }


    private int update(Sample sample, int nameIdx, Meter m) {
        if (nameIdx < 0) {
            return 0;
        }
        sample.setValue(nameIdx, m.getCount());
        return 1;
    }

    private void storeDictionary(String path) throws IOException {
        File dictFile = new File(path);
        if (dictFile.exists() && ! dictFile.delete()) {
            throw new IOException("Unable to delete dictionary file: " + dictFile.getPath());
        }
        Properties dict = new Properties();
        for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
            dict.put(String.valueOf(entry.getValue()), entry.getKey());
        }
        try (FileOutputStream out = new FileOutputStream(dictFile)) {
            dict.store(out, "RRD4JReporter dictionary");
        }
    }

    private RrdDb createDB(RrdDef definition) throws IOException {
        File dbFile = new File(definition.getPath());
        if (!dbFile.getParentFile().exists()) {
            if (!dbFile.getParentFile().mkdirs()) {
                throw new IOException("Unable to create directory for RRD file: " + dbFile.getParent());
            }
        }
        RrdDb db = null;
        if (dbFile.exists()) {
            db = new RrdDb(definition.getPath());
            if (!db.getRrdDef().equals(definition)) {
                // definition changed -> re-create DB
                db.close();
                File renamed = renameDB(dbFile);
                LOGGER.info("Configuration changed, renamed existing RRD file to: {}",
                        renamed.getPath());
                db = null;
            }
        }
        if (db == null) {
            db = new RrdDb(definition);
        }
        return db;
    }

    private File renameDB(File dbFile) throws IOException {
        // find a suitable suffix
        int idx = 0;
        while (new File(dbFile.getPath() + suffix(idx)).exists()) {
            idx++;
        }
        // rename rrd file
        rename(dbFile.toPath(), dbFile.getName() + suffix(idx));
        // rename properties file
        rename(dbFile.toPath().resolveSibling(dbFile.getName() + PROPERTIES_SUFFIX),
                dbFile.getName() + suffix(idx) + PROPERTIES_SUFFIX);

        return new File(dbFile.getParentFile(), dbFile.getName() + suffix(idx));
    }

    private static String suffix(int idx) {
        return "." + idx;
    }

    private void rename(Path path, String newName) throws IOException {
        if (!Files.exists(path)) {
            // nothing to rename
            return;
        }
        Path target = path.resolveSibling(newName);
        Files.move(path, target, REPLACE_EXISTING);
    }
}

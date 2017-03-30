/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.validation.impl.util.ValidatorTypeUtil;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class which encapsulates a map of {@link Validator}s with their meta information.
 *
 */
public class ValidatorMap {

    final static class ValidatorMetadata implements Comparable<ValidatorMetadata> {
        protected final @Nonnull Validator<?> validator;
        // default severity of the validator
        protected final Integer severity;
        protected final @Nonnull Class<?> type;
        /** used for comparison, to sort by service ranking and id */
        protected final @Nonnull ServiceReference<Validator<?>> serviceReference;

        public ValidatorMetadata(Validator<?> validator, ServiceReference<Validator<?>> serviceReference, Integer severity) {
            this.validator = validator;
            this.severity = severity;
            this.serviceReference = serviceReference;
            // cache validator's type (as this is using reflection)
            type = ValidatorTypeUtil.getValidatorType(validator);
        }

        @Override
        public int compareTo(ValidatorMetadata o) {
            return serviceReference.compareTo(o.serviceReference);
        }

        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((serviceReference == null) ? 0 : serviceReference.hashCode());
            result = prime * result + ((severity == null) ? 0 : severity.hashCode());
            result = prime * result + ((validator == null) ? 0 : validator.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ValidatorMetadata other = (ValidatorMetadata) obj;
            if (serviceReference == null) {
                if (other.serviceReference != null)
                    return false;
            } else if (!serviceReference.equals(other.serviceReference))
                return false;
            if (severity == null) {
                if (other.severity != null)
                    return false;
            } else if (!severity.equals(other.severity))
                return false;
            if (validator == null) {
                if (other.validator != null)
                    return false;
            } else if (!validator.equals(other.validator))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Entry [validator=" + validator + ", severity=" + severity + ", type=" + type + ", from bundle '" + serviceReference.getBundle().getSymbolicName() + "'"
                    + "]";
        }

        public @Nonnull Validator<?> getValidator() {
            return validator;
        }

        public @CheckForNull Integer getSeverity() {
            return severity;
        }

        public @Nonnull Class<?> getType() {
            return type;
        }
        
    }

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorMap.class);
    private final Map<String, ValidatorMetadata> validatorMap;
    
    public ValidatorMap() {
        validatorMap = new ConcurrentHashMap<>();
    }

    private String getValidatorIdFromServiceProperties(Map<String, Object> properties, @SuppressWarnings("rawtypes") Class<? extends Validator> validatorClass,
            ServiceReference<Validator<?>> serviceReference) {
        Object id = properties.get(Validator.PROPERTY_VALIDATOR_ID);
        if (id == null) {
            throw new IllegalArgumentException("Validator '" + validatorClass.getName() + "' provided from bundle "
                    + serviceReference.getBundle().getBundleId() +
                    " is lacking the mandatory service property " + Validator.PROPERTY_VALIDATOR_ID);
        }
        if (!(id instanceof String)) {
            throw new IllegalArgumentException("Validator '" + validatorClass.getName() + "' provided from bundle "
                    + serviceReference.getBundle().getBundleId() +
                    " is providing the mandatory service property " + Validator.PROPERTY_VALIDATOR_ID + " with the wrong type "
                    + id.getClass() + " (must be of type String)");
        }
        return (String) id;
    }
    
    private Integer getValidatorSeverityFromServiceProperties(Map<String, Object> properties, Validator<?> validator,
            ServiceReference<Validator<?>> serviceReference) {
        Object severity = properties.get(Validator.PROPERTY_VALIDATOR_SEVERITY);
        if (severity == null) {
            LOG.debug("Validator '{}' is not setting an explicit severity via the OSGi service property {}", validator.getClass().getName(), Validator.PROPERTY_VALIDATOR_SEVERITY);
            return null;
        }
        if (!(severity instanceof Integer)) {
            throw new IllegalArgumentException("Validator '" + validator.getClass().getName() + "' provided from bundle "
                    + serviceReference.getBundle().getBundleId() +
                    " is providing the optional service property " + Validator.PROPERTY_VALIDATOR_SEVERITY + " with the wrong type "
                    + severity.getClass() + " (must be of type Integer)");
        }
        return (Integer) severity;
    }

    public void put(@Nonnull Map<String, Object> properties, @Nonnull Validator<?> validator, ServiceReference<Validator<?>> serviceReference) {
        String validatorId = getValidatorIdFromServiceProperties(properties, validator.getClass(), serviceReference);
        Integer severity = getValidatorSeverityFromServiceProperties(properties, validator, serviceReference);
        put(validatorId, validator, serviceReference, severity);
    }

    void put(@Nonnull String id, @Nonnull Validator<?> validator, ServiceReference<Validator<?>> serviceReference, Integer severity) {
        // create new entry
        ValidatorMetadata entry = new ValidatorMetadata(validator, serviceReference, severity);
        if (validatorMap.containsKey(id)) {
            ValidatorMetadata existingEntry = validatorMap.get(id);
            if (entry.compareTo(existingEntry) > 0) {
                LOG.info("Overwriting already existing validator {} with {}, because it has the same id '{}' and a higher service ranking",
                        existingEntry, entry, id);
            } else {
                LOG.info(
                        "A Validator for the same id '{}' is already registered {} and it has a higher service ranking, therefore ignoring {}",
                        id, existingEntry, entry);
                return;
            }
        } else {
            LOG.debug("New validator with id '{}' added: {}", id, entry);
        }
        validatorMap.put(id, entry);
    }

    public void update(@Nonnull Map<String, Object> properties, @Nonnull Validator<?> validator, ServiceReference<Validator<?>> serviceReference) {
        String validatorId = getValidatorIdFromServiceProperties(properties, validator.getClass(), serviceReference);
        Integer severity = getValidatorSeverityFromServiceProperties(properties, validator, serviceReference);
        update(validatorId, validator, serviceReference, severity);
    }

    void update(@Nonnull String id, @Nonnull Validator<?> validator, ServiceReference<Validator<?>> serviceReference, Integer severity) {
        LOG.info("Updating validator with id '{}'", id);
        // the id might have been changed, therefore remove old entry by looking up the service reference!
        remove(serviceReference);
        put(id, validator, serviceReference, severity);
    }

    private boolean remove(ServiceReference<Validator<?>> serviceReference) {
        for (java.util.Iterator<ValidatorMetadata> iterator = validatorMap.values().iterator(); iterator.hasNext();) {
            ValidatorMetadata value = iterator.next();
            if (value.serviceReference.equals(serviceReference)) {
               iterator.remove();
               return true;
            }
        }
        return false;
    }

    public boolean remove(@Nonnull Map<String, Object> properties, @Nonnull Validator<?> validator, ServiceReference<Validator<?>> serviceReference) {
        String validatorId = getValidatorIdFromServiceProperties(properties, validator.getClass(), serviceReference);
        return remove(validatorId, serviceReference);
    }
    
    public boolean remove(String id, ServiceReference<Validator<?>> serviceReference) {
        // only actually remove if the service reference is equal
        if (id == null) {
            // find by service reference
        }
        ValidatorMetadata entry = validatorMap.get(id);
        if (entry == null) {
            LOG.warn("Could not remove validator with id '{}' from map, because it is not there!", id);
            return false;
        } else {
            // only actually remove if the service reference is equal
            if (entry.serviceReference.equals(serviceReference)) {
                return true;
            } else {
                LOG.warn("Could not remove validator with id '{}' from map because it is only contained with a different service reference!", id);
                return false;
            }
        }
    }

    public ValidatorMetadata get(String id) {
        return validatorMap.get(id);
    }
}

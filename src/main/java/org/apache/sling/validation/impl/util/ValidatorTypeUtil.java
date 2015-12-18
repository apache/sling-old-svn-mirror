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
package org.apache.sling.validation.impl.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.sling.validation.spi.Validator;

public class ValidatorTypeUtil {
    
    /**
     * 
     * @param validator
     * @return the type parametrization value on the {@link Validator} interface
     */
    public static @Nonnull Class<?> getValidatorType(Validator<?> validator) {
        // get all type arguments from the current validator class up to the Validator interface
        Map<TypeVariable<?>, java.lang.reflect.Type> typeMap = TypeUtils.getTypeArguments(validator.getClass(), Validator.class);
        java.lang.reflect.Type type = null;
        for (Entry<TypeVariable<?>, java.lang.reflect.Type> entry : typeMap.entrySet()) {
            type = entry.getValue();
            // check if this is really the type argument defined on the interface {@link Validator}
            if (entry.getKey().getGenericDeclaration() instanceof Class<?>) {
                Class<?> clazz = (Class<?>)entry.getKey().getGenericDeclaration();
                if (clazz.equals(Validator.class)) {
                	// Java6 doesn't return the class for array types due to this bug: http://bugs.java.com/view_bug.do?bug_id=5041784
                	if (type instanceof GenericArrayType) {
                		// as a workaround make a new array class out of the generic component type encapsulated in the generic array type
                    	type = Array.newInstance((Class<?>) ((GenericArrayType)type).getGenericComponentType(), 0).getClass();
                    }
                    if (type instanceof Class<?>) {
                        return (Class<?>)type;
                    }
                    // type may also be a parameterized type (e.g. for Collection<String>), this is not allowed!
                    else {
                        throw new IllegalArgumentException("Validators may not use parameterized types as type parameter. Only simple class types and arrays of class types are allowed.");
                    }
                }
            }
           
        }
        throw new IllegalArgumentException("Validator '" + validator +"' has not valid type parameter!");
}
}

/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.api.ObjectModel;

/**
 * Extends the object model by providing implementations for all Sightly
 * operators.
 */
public class Dynamic implements ObjectModel {

    private final ObjectModel model;

    public Dynamic(ObjectModel model) {
        this.model = model;
    }

    @Override
    public Object resolveProperty(Object target, Object property) {
        return model.resolveProperty(target, property);
    }

    @Override
    public String coerceToString(Object target) {
        return model.coerceToString(target);
    }

    @Override
    public boolean coerceToBoolean(Object object) {
        return model.coerceToBoolean(object);
    }

    @Override
    public Collection<Object> coerceToCollection(Object object) {
        return model.coerceToCollection(object);
    }

    @Override
    public Number coerceNumeric(Object object) {
        if (object instanceof Number) {
            return (Number) object;
        }
        return 0;
    }

    @Override
    public Map coerceToMap(Object object) {
        return model.coerceToMap(object);
    }

    @Override
    public boolean strictEq(Object left, Object right) {
        return model.strictEq(left, right);
    }

    public boolean eq(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    @Override
    public boolean lt(Object left, Object right) {
        return model.lt(left, right);
    }

    @Override
    public boolean leq(Object left, Object right) {
        return model.leq(left, right);
    }

    public Object and(Object left, Object right) {
        if (coerceToBoolean(left)) {
            return right;
        }
        return left;
    }


    public Object or(Object left, Object right) {
        if (coerceToBoolean(left)) {
            return left;
        }
        return right;
    }


    public Object not(Object obj) {
        return !coerceToBoolean(obj);
    }


    public Object concatenate(Object left, Object right) {
        return coerceToString(left) + coerceToString(right);
    }


    public Object isWhiteSpace(Object object) {
        return StringUtils.isWhitespace(coerceToString(object));
    }


    public int length(Object object) {
        return coerceToCollection(object).size();
    }




    public Number add(Object left, Object right) {
        return adjust(coerceDouble(left) + coerceDouble(right));
    }


    public Number sub(Object left, Object right) {
        return adjust(coerceDouble(left) - coerceDouble(right));
    }


    public Number mult(Object left, Object right) {
        return adjust(coerceDouble(left) * coerceDouble(right));
    }


    public int iDiv(Object left, Object right) {
        return coerceInt(left) / coerceInt(right);
    }


    public int rem(Object left, Object right) {
        return coerceInt(left) % coerceInt(right);
    }


    public Number div(Object left, Object right) {
        return adjust(coerceDouble(left) / coerceDouble(right));
    }


    public boolean neq(Object left, Object right) {
        return !eq(left, right);
    }

    public boolean strictNeq(Object left, Object right) {
        return !strictEq(left, right);
    }

    public boolean gt(Object left, Object right) {
        return !leq(left, right);
    }


    public boolean geq(Object left, Object right) {
        return !lt(left, right);
    }

    public boolean isCollection(Object obj) {
        return (obj instanceof Collection) || (obj instanceof Object[])
                || (obj instanceof Iterable)
                || (obj instanceof Iterator);
    }

    private double coerceDouble(Object object) {
        if (object instanceof Number) {
            return ((Number) object).doubleValue();
        }
        return 0;
    }

    private int coerceInt(Object object) {
        return coerceNumeric(object).intValue();
    }

    private Number adjust(double x) {
        if (Math.floor(x) == x) {
            return (int)x;
        }
        return x;
    }
}

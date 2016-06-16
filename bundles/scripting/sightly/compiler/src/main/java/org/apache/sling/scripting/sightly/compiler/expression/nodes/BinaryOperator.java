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
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.impl.compiler.CompileTimeObjectModel;

/**
 * Binary operators used in expressions.
 */
public enum BinaryOperator {
    /**
     * Logical conjunction.
     */
    AND {
        @Override
        public Object eval(Object left, Object right) {
            return (CompileTimeObjectModel.toBoolean(left)) ? right : left;
        }
    },
    /**
     * Logical disjunction.
     */
    OR {
        @Override
        public Object eval(Object left, Object right) {
            return (CompileTimeObjectModel.toBoolean(left)) ? left : right;
        }
    },
    /**
     * String concatenation.
     */
    CONCATENATE {
        @Override
        public Object eval(Object left, Object right) {
            return CompileTimeObjectModel.toString(left).concat(CompileTimeObjectModel.toString(right));
        }
    },
    /**
     * Less than.
     */
    LT {
        @Override
        public Object eval(Object left, Object right) {
            return lt(left, right);
        }
    },
    /**
     * Less or equal.
     */
    LEQ {
        @Override
        public Object eval(Object left, Object right) {
            return leq(left, right);
        }
    },
    /**
     * Greater than.
     */
    GT {
        @Override
        public Object eval(Object left, Object right) {
            return !leq(left, right);
        }
    },
    /**
     * Greater or equal.
     */
    GEQ {
        @Override
        public Object eval(Object left, Object right) {
            return !lt(left, right);
        }
    },
    /**
     * Equal.
     */
    EQ {
        @Override
        public Object eval(Object left, Object right) {
            return eq(left, right);
        }
    },
    /**
     * Not equal.
     */
    NEQ {
        @Override
        public Object eval(Object left, Object right) {
            return !eq(left, right);
        }

    },
    /**
     * Strict version of equality, restricted to just some types.
     */
    STRICT_EQ {
        @Override
        public Object eval(Object left, Object right) {
            return strictEq(left, right);
        }
    },
    /**
     * Strict version of the not-equal operator.
     */
    STRICT_NEQ {
        @Override
        public Object eval(Object left, Object right) {
            return !strictEq(left, right);
        }
    },
    /**
     * Addition.
     */
    ADD {
        @Override
        public Object eval(Object left, Object right) {
            return adjust(CompileTimeObjectModel.toNumber(left).doubleValue() + CompileTimeObjectModel.toNumber(right).doubleValue());
        }
    },

    /**
     * Difference.
     */
    SUB {
        @Override
        public Object eval(Object left, Object right) {
            return adjust(CompileTimeObjectModel.toNumber(left).doubleValue() - CompileTimeObjectModel.toNumber(right).doubleValue());
        }
    },
    /**
     * Multiplication.
     */
    MUL {
        @Override
        public Object eval(Object left, Object right) {
            return adjust(CompileTimeObjectModel.toNumber(left).doubleValue() * CompileTimeObjectModel.toNumber(right).doubleValue());
        }
    },
    /**
     * Floating point division.
     */
    DIV {
        @Override
        public Object eval(Object left, Object right) {
            return adjust(CompileTimeObjectModel.toNumber(left).doubleValue() / CompileTimeObjectModel.toNumber(right).doubleValue());
        }
    },
    /**
     * Integer division.
     */
    I_DIV {
        @Override
        public Object eval(Object left, Object right) {
            return CompileTimeObjectModel.toNumber(left).intValue() / CompileTimeObjectModel.toNumber(right).intValue();
        }
    },

    /**
     * Reminder.
     */
    REM {
        @Override
        public Object eval(Object left, Object right) {
            return adjust(CompileTimeObjectModel.toNumber(left).intValue()
                    % CompileTimeObjectModel.toNumber(right).intValue());
        }

    };

    public static boolean eq(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    public static boolean lt(final Object left, final Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() < ((Number) right).doubleValue();
        }
        throw new SightlyCompilerException("Operands are not of the same type: comparison is supported for Number types only.");
    }

    public static boolean leq(final Object left, final Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
        }
        throw new SightlyCompilerException("Operands are not of the same type: comparison is supported for Number types only.");
    }


    public static boolean strictEq(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        if (left instanceof String && right instanceof String) {
            return left.equals(right);
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            return left.equals(right);
        }
        if (left == null && right == null) {
            return true;
        }
        if ((left instanceof Enum && right instanceof String) || (left instanceof String && right instanceof Enum)) {
            String constantName = left instanceof String ? (String) left : (String) right;
            Enum enumObject = left instanceof Enum ? (Enum) left : (Enum) right;
            try {
                Enum enumComparisonObject = Enum.valueOf(enumObject.getClass(), constantName);
                return enumComparisonObject == enumObject;
            } catch (Exception e) {
                return false;
            }
        }
        if (left == null || right == null) {
            Object notNull = (left != null) ? left : right;
            if (notNull instanceof String || notNull instanceof Boolean || notNull instanceof Number) {
                return false;
            }
        }
        throw new SightlyCompilerException("Operands are not of the same type: the equality operator can only be applied to String, Number" +
                " and Boolean types.");
    }

    private static Number adjust(double x) {
        if (Math.floor(x) == x) {
            return (int) x;
        }
        return x;
    }

    public abstract Object eval(Object left, Object right);
}

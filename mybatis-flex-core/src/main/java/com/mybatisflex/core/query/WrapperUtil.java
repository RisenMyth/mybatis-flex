/**
 * Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mybatisflex.core.query;


import com.mybatisflex.core.FlexConsts;
import com.mybatisflex.core.dialect.IDialect;
import com.mybatisflex.core.util.ClassUtil;
import com.mybatisflex.core.util.EnumWrapper;
import com.mybatisflex.core.util.StringUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class WrapperUtil {


    static String buildAsAlias(String alias, IDialect dialect) {
        return StringUtil.isBlank(alias) ? "" : " AS " + dialect.wrap(alias);
    }

    static List<QueryWrapper> getChildQueryWrapper(QueryCondition condition) {
        List<QueryWrapper> list = null;
        while (condition != null) {
            if (condition.checkEffective()) {
                if (condition instanceof Brackets) {
                    List<QueryWrapper> childQueryWrapper = getChildQueryWrapper(((Brackets) condition).getChildCondition());
                    if (!childQueryWrapper.isEmpty()) {
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.addAll(childQueryWrapper);
                    }
                }
                // not Brackets
                else {
                    Object value = condition.getValue();
                    if (value instanceof QueryWrapper) {
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.add((QueryWrapper) value);
                        list.addAll(((QueryWrapper) value).getChildSelect());
                    } else if (value != null && value.getClass().isArray()) {
                        for (int i = 0; i < Array.getLength(value); i++) {
                            Object arrayValue = Array.get(value, i);
                            if (arrayValue instanceof QueryWrapper) {
                                if (list == null) {
                                    list = new ArrayList<>();
                                }
                                list.add((QueryWrapper) arrayValue);
                                list.addAll(((QueryWrapper) arrayValue).getChildSelect());
                            }
                        }
                    }
                }
            }
            condition = condition.next;
        }
        return list == null ? Collections.emptyList() : list;
    }


    static Object[] getValues(QueryCondition condition) {
        if (condition == null) {
            return FlexConsts.EMPTY_ARRAY;
        }

        List<Object> params = new ArrayList<>();
        getValues(condition, params);

        return params.isEmpty() ? FlexConsts.EMPTY_ARRAY : params.toArray();
    }


    private static void getValues(QueryCondition condition, List<Object> params) {
        if (condition == null) {
            return;
        }

        Object value = condition.getValue();
        if (value == null
                || value instanceof QueryColumn
                || value instanceof RawFragment) {
            getValues(condition.next, params);
            return;
        }

        addParam(params, value);
        getValues(condition.next, params);
    }

    private static void addParam(List<Object> paras, Object value) {
        if (value == null) {
            paras.add(null);
        } else if (ClassUtil.isArray(value.getClass())) {
            for (int i = 0; i < Array.getLength(value); i++) {
                addParam(paras, Array.get(value, i));
            }
        } else if (value instanceof QueryWrapper) {
            Object[] valueArray = ((QueryWrapper) value).getValueArray();
            paras.addAll(Arrays.asList(valueArray));
        } else if (value.getClass().isEnum()) {
            EnumWrapper enumWrapper = EnumWrapper.of(value.getClass());
            if (enumWrapper.hasEnumValueAnnotation()) {
                paras.add(enumWrapper.getEnumValue((Enum) value));
            } else {
                paras.add(value);
            }
        } else {
            paras.add(value);
        }

    }





}

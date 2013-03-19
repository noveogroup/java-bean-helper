/*
 * Copyright (c) 2013 Noveo Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Except as contained in this notice, the name(s) of the above copyright holders
 * shall not be used in advertising or otherwise to promote the sale, use or
 * other dealings in this Software without prior written authorization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.noveo.android.bean;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class BeanRegistry {

    private static BeanRegistry instance = new BeanRegistry();

    public static BeanRegistry getInstance() {
        return instance;
    }

    private final Map<Class<?>, WeakReference<Bean<?>>> map = new HashMap<Class<?>, WeakReference<Bean<?>>>();

    private BeanRegistry() {
    }

    @SuppressWarnings("unchecked")
    public <T> Bean<T> get(Class<T> beanClass) {
        synchronized (map) {
            WeakReference<Bean<?>> reference = map.get(beanClass);
            if (reference != null) {
                Bean<?> bean = reference.get();
                if (bean != null) {
                    return (Bean<T>) bean;
                }
            }

            Bean<T> bean = load(beanClass);
            map.put(beanClass, new WeakReference<Bean<?>>(bean));
            return bean;
        }
    }

    private static final String IS_PREFIX = "is";
    private static final String GET_PREFIX = "get";
    private static final String SET_PREFIX = "set";

    private static Map<String, Field> getFields(Class<?> beanClass) {
        Map<String, Field> map = new HashMap<String, Field>();

        while (beanClass != null && beanClass != Object.class) {
            for (Field field : beanClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers)) {
                    map.put(field.getName(), field);
                }
            }
            beanClass = beanClass.getSuperclass();
        }

        return map;
    }

    private static Map<String, Method> getMethods(Class<?> beanClass) {
        Map<String, Method> map = new HashMap<String, Method>();

        while (beanClass != null && beanClass != Object.class) {
            for (Method method : beanClass.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                String name = method.getName();
                if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
                        && (name.startsWith(IS_PREFIX) || name.startsWith(GET_PREFIX) || name.startsWith(SET_PREFIX))) {
                    map.put(method.getName(), method);
                }
            }
            beanClass = beanClass.getSuperclass();
        }

        return map;
    }

    private static Set<String> getFieldPropertyNames(Collection<Field> fields) {
        Set<String> set = new HashSet<String>(fields.size());
        for (Field field : fields) {
            set.add(field.getName());
        }
        return set;
    }

    private static Set<String> getMethodPropertyNames(Collection<Method> methods) {
        Set<String> set = new HashSet<String>(methods.size());
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(IS_PREFIX) && name.length() > IS_PREFIX.length()) {
                name = name.substring(IS_PREFIX.length());
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                set.add(name);
            }
            if (name.startsWith(GET_PREFIX) && name.length() > GET_PREFIX.length()) {
                name = name.substring(GET_PREFIX.length());
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                set.add(name);
            }
            if (name.startsWith(SET_PREFIX) && name.length() > SET_PREFIX.length()) {
                name = name.substring(SET_PREFIX.length());
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                set.add(name);
            }
        }
        return set;
    }

    private static <T> Bean<T> load(Class<T> beanClass) {
        // get class members
        Map<String, Field> fields = getFields(beanClass);
        Map<String, Method> methods = getMethods(beanClass);

        // get property names
        Set<String> propertyNames = new HashSet<String>();
        propertyNames.addAll(getFieldPropertyNames(fields.values()));
        propertyNames.addAll(getMethodPropertyNames(methods.values()));

        // load properties
        List<Property<T>> properties = new ArrayList<Property<T>>(propertyNames.size());
        for (String propertyName : propertyNames) {
            Field field = fields.get(propertyName);

            String getter1Name = IS_PREFIX + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method getter1 = methods.get(getter1Name);

            String getter2Name = GET_PREFIX + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method getter2 = methods.get(getter2Name);

            String setterName = SET_PREFIX + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method setter = methods.get(setterName);

            Class<?> type = Object.class;
            if (field != null) {
                type = field.getType();
            } else if (getter1 != null) {
                type = getter1.getReturnType();
            } else if (getter2 != null) {
                type = getter2.getReturnType();
            } else if (setter != null) {
                if (setter.getParameterTypes().length > 0) {
                    type = setter.getParameterTypes()[0];
                }
            }

            // choose the getter
            Method getter;
            if (type == Boolean.class || type == boolean.class) {
                getter = getter1 != null ? getter1 : getter2;
            } else {
                getter = getter2;
            }

            try {
                properties.add(new DefaultProperty<T>(beanClass, propertyName, type, field, getter, setter));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new DefaultBean<T>(beanClass, beanClass.getName(), properties);
    }

}

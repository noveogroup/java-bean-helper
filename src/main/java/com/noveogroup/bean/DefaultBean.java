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

package com.noveogroup.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultBean<T> implements Bean<T> {

    private final Class<T> beanClass;
    private final String name;
    private final Map<String, Property<T>> properties;

    public DefaultBean(Class<T> beanClass, String name, Collection<Property<T>> properties) {
        this.beanClass = beanClass;
        this.name = name;

        HashMap<String, Property<T>> map = new HashMap<String, Property<T>>(properties.size());
        for (Property<T> property : properties) {
            map.put(property.getName(), property);
        }
        this.properties = Collections.unmodifiableMap(map);
    }

    @Override
    public Class<T> getBeanClass() {
        return beanClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T newBean() throws InstantiationException, IllegalAccessException {
        return beanClass.newInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] newBeanArray(int length) {
        return (T[]) Array.newInstance(beanClass, length);
    }

    @Override
    public Property<T> getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<Property<T>> getProperties() {
        return properties.values();
    }

    @Override
    public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationClass) {
        return beanClass.isAnnotationPresent(annotationClass);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return beanClass.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return beanClass.getAnnotations();
    }

}

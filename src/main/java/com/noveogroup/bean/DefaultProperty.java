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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DefaultProperty<T> implements Property<T> {

    private final Class<T> beanClass;
    private final String name;
    private final Field field;

    private final Method getter;
    private final Method setter;
    private final Class<?> type;

    private final boolean isReadable;
    private final boolean isWritable;

    public DefaultProperty(Class<T> beanClass, String name, Class<?> type, Field field, Method getter, Method setter) {
        this.beanClass = beanClass;
        this.name = name;
        this.type = type;

        this.field = field;
        if (field != null) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("field should not be static");
            }
            if (!type.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("field type is not assignable from property type");
            }
        }

        this.getter = getter;
        if (getter != null) {
            int modifiers = getter.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("getter should not be static");
            }
            if (!type.isAssignableFrom(getter.getReturnType())) {
                throw new IllegalArgumentException("getter return type is not assignable from the property type");
            }
            if (getter.getParameterTypes().length != 0) {
                throw new IllegalArgumentException("getter should not have parameters");
            }
        }

        this.setter = setter;
        if (setter != null) {
            int modifiers = setter.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("setter should not be static");
            }
            if (setter.getReturnType() != void.class) {
                throw new IllegalArgumentException("setter should not return a value");
            }
            if (setter.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("setter should have only one parameter");
            }
            if (!setter.getParameterTypes()[0].isAssignableFrom(type)) {
                throw new IllegalArgumentException("setter parameter type should be assignable from the property type");
            }
        }

        boolean isReadable = false;
        boolean isWritable = false;
        if (field != null) {
            int modifiers = field.getModifiers();
            isReadable = Modifier.isPublic(modifiers);
            isWritable = Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers);
        }
        if (getter != null) {
            int modifiers = getter.getModifiers();
            isReadable = isReadable || Modifier.isPublic(modifiers);
        }
        if (setter != null) {
            int modifiers = setter.getModifiers();
            isWritable = isWritable || Modifier.isPublic(modifiers);
        }
        this.isReadable = isReadable;
        this.isWritable = isWritable;
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
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean isReadable() {
        return isReadable;
    }

    @Override
    public Object getValue(T bean) throws IllegalAccessException, InvocationTargetException {
        if (getter != null) {
            return getter.invoke(bean);
        } else if (field != null) {
            return field.get(bean);
        } else {
            throw new IllegalAccessError("property is not readable");
        }
    }

    @Override
    public boolean isWritable() {
        return isWritable;
    }

    @Override
    public void setValue(T bean, Object value) throws IllegalAccessException, InvocationTargetException {
        if (setter != null) {
            setter.invoke(bean, value);
        } else if (field != null) {
            field.set(bean, value);
        } else {
            throw new IllegalAccessError("property is not writable");
        }
    }

    @Override
    public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationClass) {
        if (field != null && field.isAnnotationPresent(annotationClass)) {
            return true;
        }
        if (getter != null && getter.isAnnotationPresent(annotationClass)) {
            return true;
        }
        if (setter != null && setter.isAnnotationPresent(annotationClass)) {
            return true;
        }
        return false;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        A annotation;
        if (field != null && (annotation = field.getAnnotation(annotationClass)) != null) {
            return annotation;
        }
        if (getter != null && (annotation = getter.getAnnotation(annotationClass)) != null) {
            return annotation;
        }
        if (setter != null && (annotation = setter.getAnnotation(annotationClass)) != null) {
            return annotation;
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        Annotation[] fieldAnnotations = field == null ? new Annotation[0] : field.getAnnotations();
        Annotation[] getterAnnotations = getter == null ? new Annotation[0] : getter.getAnnotations();
        Annotation[] setterAnnotations = setter == null ? new Annotation[0] : setter.getAnnotations();

        Annotation[] annotations = new Annotation[fieldAnnotations.length + getterAnnotations.length + setterAnnotations.length];

        int position = 0;
        System.arraycopy(fieldAnnotations, 0, annotations, position, fieldAnnotations.length);
        position += fieldAnnotations.length;
        System.arraycopy(getterAnnotations, 0, annotations, position, getterAnnotations.length);
        position += getterAnnotations.length;
        System.arraycopy(setterAnnotations, 0, annotations, position, setterAnnotations.length);

        return annotations;
    }

}

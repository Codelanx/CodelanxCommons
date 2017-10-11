/*
 * Copyright (C) 2016 Codelanx, All Rights Reserved
 *
 * This work is licensed under a Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 *
 * This program is protected software: You are free to distrubute your
 * own use of this software under the terms of the Creative Commons BY-NC-ND
 * license as published by Creative Commons in the year 2015 or as published
 * by a later date. You may not provide the source files or provide a means
 * of running the software outside of those licensed to use it.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the Creative Commons BY-NC-ND license
 * long with this program. If not, see <https://creativecommons.org/licenses/>.
 */
package com.codelanx.commons.util;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides utility methods for simplifying lambda operations
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public class Lambdas {

    /**
     * Returns {@code true} if the passed object is not {@code null}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param o The object to check
     * @deprecated JDK provides a method
     * @see java.util.Objects#nonNull(Object) 
     * @return {@code true} if not {@code null}
     */
    @Deprecated
    public static boolean notNull(Object o) {
        return o != null;
    }

    /**
     * Returns {@code true} if the passed object is {@code null}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param o The object to check
     * @deprecated JDK provides a method
     * @see java.util.Objects#isNull(Object)
     * @return {@code true} if {@code null}
     */
    @Deprecated
    public static boolean isNull(Object o) {
        return !Lambdas.notNull(o);
    }

    /**
     * Useful in {@link java.util.stream.Stream#filter(Predicate)}.
     *
     * <p>Maps a stream element to a new identity {@code U}, and tests it
     * against the passed {@link Predicate Predicate<U>}.
     *
     * @param mapper The mapping function of the stream elements to a new testable identity
     * @param predicate The predicate for our new identity
     * @param <T> The original stream element type
     * @param <U> Our new identity type
     * @return A {@link Predicate Predicate&lt;T&gt;} which will map and test against stream elements of {@code &lt;T&gt;}
     */
    public static <T, U> Predicate<T> mapToPredicate(Function<T, U> mapper, Predicate<U> predicate) {
        return o -> predicate.test(mapper.apply(o));
    }
}

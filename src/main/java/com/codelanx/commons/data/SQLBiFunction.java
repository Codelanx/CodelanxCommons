package com.codelanx.commons.data;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents an SQL function that accepts two arguments and produces a result.
 *
 * @since 0.3.2
 * @author 1Rogue
 * @version 0.3.2
 *
 * @see Function
 * @param <T> the type of the input to the function
 * @param <U> the type of the second argument
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface SQLBiFunction<T, U, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     * @throws SQLException If an {@link SQLException} is thrown in the lambda
     *                      body
     */
    R apply(T t, U u) throws SQLException;

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> SQLBiFunction<T, U, V> andThen(SQLFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}

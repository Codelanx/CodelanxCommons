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
package com.codelanx.commons.data;

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Represents the response from the sql server, whether that be in
 * the form of an {@link SQLException} or just the returned and
 * selected value from a query
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.3.2
 * 
 * @param <T> The response content type
 */
public class SQLResponse<T> implements Cloneable {

    public static SQLResponse<?> EMPTY = new SQLResponse<>((Object) null);
    private SQLException ex;
    private T response;
    private int update;
    
    public SQLResponse() {

    }

    public SQLResponse(T response) {
        this.response = response;
    }

    public SQLResponse(SQLException ex) {
        this.ex = ex;
    }

    public SQLResponse(int update) {
        this.update = update;
    }

    public SQLException getException() {
        return this.ex;
    }

    public SQLResponse<T> onException(Consumer<SQLException> consumer) {
        if (this.ex != null) {
            consumer.accept(this.ex);
        }
        return this;
    }
    
    public T getResponse() {
        return this.response;
    }
    
    public int getUpdatedRows() {
        return this.update;
    }

    void setException(SQLException ex) {
        if (this == EMPTY) {
            throw new UnsupportedOperationException("Cannot call setters on public constant");
        }
        this.ex = ex;
    }
    
    void setResponse(T response) {
        if (this == EMPTY) {
            throw new UnsupportedOperationException("Cannot call setters on public constant");
        }
        this.response = response;
    }
    
    void setUpdatedRows(int update) {
        if (this == EMPTY) {
            throw new UnsupportedOperationException("Cannot call setters on public constant");
        }
        this.update = update;
    }

    @Override
    protected SQLResponse<T> clone() {
        try {
            return (SQLResponse<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new SQLResponse<>();
    }
}

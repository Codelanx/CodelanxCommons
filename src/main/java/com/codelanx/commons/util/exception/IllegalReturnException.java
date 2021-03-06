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
package com.codelanx.commons.util.exception;

/**
 * Thrown when the return type is not acceptable and the program logic cannot
 * continue
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public class IllegalReturnException extends RuntimeException {

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param message {@inheritDoc}
     */
    public IllegalReturnException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    public IllegalReturnException() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param message {@inheritDoc}
     * @param cause {@inheritDoc}
     */
    public IllegalReturnException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param cause {@inheritDoc}
     */
    public IllegalReturnException(Throwable cause) {
        super(cause);
    }

}

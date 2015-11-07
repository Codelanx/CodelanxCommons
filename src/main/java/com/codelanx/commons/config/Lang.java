/*
 * Copyright (C) 2015 Codelanx, All Rights Reserved
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
package com.codelanx.commons.config;

import com.codelanx.commons.data.FileDataType;
import com.codelanx.commons.lost.internal.InternalLang;

/**
 * Represents a single value that is dynamically retrieved from a
 * {@link FileDataType}. This value should be usable with a Formatter and
 * is typically implemented through an enum
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public interface Lang extends ConfigFile {

    /**
     * Returns the format specifier for this {@link Lang} file. This should be
     * a constant in the enum and represent a value that is used for all
     * messages universally.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The format specifier for this {@link Lang}
     */
    public Lang getFormat();

    /**
     * The default value of this {@link Lang} string
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @return The default value
     */
    @Override
    public String getDefault();

    /**
     * Returns the string value used for this {@link Lang}. Color codes
     * will not be automatically converted
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The formatting string to be used for sending a message
     */
    @Override
    default public String get() {
        if (this.getClass().isAnonymousClass()) {
            return this.getDefault();
        }
        return String.valueOf(ConfigFile.super.get());
    }

    /**
     * Formats a {@link Lang} enum constant with the supplied arguments
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param args The arguments to supply
     * @return The formatted string
     */
    default public String format(Object... args) {
        return String.format(this.get(), args);
    }

    /**
     * Will format a string with "PLURAL" or "PLURALA" tokens in them.
     * <br><br><ul>
     * <li> <em>PLURALA</em>: Token that will evaluate gramatically. An int
     * value of 1 will return "is &lt;amount&gt; 'word'", otherwise it will be
     * "are &lt;amount&gt; 'word'".
     * </li><li> <em>PLURAL</em>: Token that will evaluate the word. An int
     * value of 1 will return the first word, value of 2 the second word.
     * </ul>
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param amount The amount representative of the data token
     * @param args The arguments to replace any other tokens with.
     * @return The formatting string value for plurals
     */
    default public String pluralFormat(int amount, Object... args) {
        String repl = this.get();
        repl = repl.replaceAll("\\{PLURALA (.*)\\|(.*)\\}", amount == 1 ? "is " + amount + " $1" : "are " + amount + " $2");
        repl = repl.replaceAll("\\{PLURAL (.*)\\|(.*)\\}", amount == 1 ? "$1" : "$2");
        return String.format(repl, args);
    }

    /**
     * Returns a raw Lang object that can be used for dynamic creation of Lang
     * variables
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param format The string to wrap in a {@link Lang} object
     * @return A {@link Lang} object that will 
     */
    public static Lang createLang(String format) {
        return new Lang() {

            @Override
            public Lang getFormat() {
                return InternalLang.FORMAT.getFormat();
            }

            @Override
            public String getDefault() {
                return format;
            }

            @Override
            public String getPath() {
                return null;
            }

            @Override
            public FileDataType getConfig() {
                throw new UnsupportedOperationException("An anonymous Lang does not have a FileDataType associated with it");
            }

            @Override
            public DataHolder<FileDataType> getData() {
                throw new UnsupportedOperationException("Anonymous ConfigFile classes do not have DataHolders");
            }

        };
    }

    /**
     * Returns a Lang object representing the default formatting for all
     * CodelanxLib plugins. The {@code option} parameter can be supplemented
     * with {@code null} which will default to "CL-Lib". The returned format is:
     * <br> <br>
     * {@code "&amp;f[&amp;9" + option == null ? "CL-Lib" : option + "&amp;f] %amp;s"}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param option The optional "title" string for the format
     * @return A {@link Lang} object representing the default format
     */
    public static Lang defaultFormat(String option) {
        return Lang.createLang("&f[&9" + (option == null ? "CL-Lib" : option) + "&f] %s");
    }

    /**
     * Formats a string into either a proper word or sentence (First letter
     * capitalized, if spaces are included a period is added)
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @see Lang#proper(String, char)
     * @param raw The raw string to format
     * @return The formatted string
     */
    public static String proper(String raw) {
        return Lang.proper(raw, '.');
    }

    /**
     * Formats a string into either a proper word or sentence (First letter
     * capitalized, if spaces are included the supplied punctuation is added)
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param raw The raw string to format
     * @param punctuation Punctuation to append to anything containing spaces
     * @return The formatted string
     */
    public static String proper(String raw, char punctuation) {
        raw = raw.trim();
        return raw.isEmpty() ? "" : (Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase() + (raw.contains(" ") ? punctuation : ""));
    }

}

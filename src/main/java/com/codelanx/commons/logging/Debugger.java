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
package com.codelanx.commons.logging;

import com.codelanx.commons.util.exception.Exceptions;
import com.codelanx.commons.util.Scheduler;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

/**
 * Provides toggleable logging supporting for debug statements and error
 * reporting to a webservice for easy bugfixing. Will find unreported errors as
 * well and submit them. Note that this class is provided as a utility, it
 * should not be used as a substitute for general logging statements that are
 * <i>always</i> available. Use the {@link Logging} class instead.
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public class Debugger {

    private final static Logger logger = Logger.getLogger(Debugger.class.getName());

    private Debugger() {}
    /**
     * Sets the URL to send a JSON payload of server information to as well as
     * any other relevant information for when a stack trace occurs. This allows
     * for a simple way of setting up error reporting. The default value for
     * this is {@code null}, and as a result will not send any information upon
     * errors occurring unless a target URL is set.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param url The URL to send JSON payloads to
     */
    public static void setReportingURL(String url) {
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts != null) {
            opts.setUrl(url);
        }
    }

    /**
     * Sets whether or not to actually output any calls from your plugin to the
     * Debugger. This defaults to {@code false}.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param output {@code true} if calls to the Debugger should print out
     */
    public static void toggleOutput(boolean output) {
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts != null) {
            opts.toggleOutput(output);
        }
    }

    /**
     * Sets whether or not calls to
     * {@link Debugger#error(Throwable, String, Object...)} will print errors.
     * This is distinct from {@link Debugger#toggleOutput(boolean)} and errors
     * will not depend on its value. This defaults to {@code false}.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param hide {@code true} if errors should be printed
     */
    public static void hideErrors(boolean hide) {
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts != null) {
            opts.hideErrors(hide);
        }
    }

    /**
     * Prints to the Debugging {@link Logger} if
     * {@link Debugger#toggleOutput(boolean)} is set to {@code true}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param level The {@link Level} to print at
     * @param format The formatting string
     * @param args The printf arguments
     */
    public static void print(Level level, String format, Object... args) {
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts == null || !opts.doOutput()) {
            return;
        }
        Debugger.logger.log(level, String.format("[%s]=> %s",
                opts.getPrefix(), String.format(format, args)));
    }

    /**
     * Prints to the Debugging {@link Logger} at {@link Level#INFO} if
     * {@link Debugger#toggleOutput(boolean)} is set to {@code true}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param format The formatting string
     * @param args The printf arguments
     */
    public static void print(String format, Object... args) {
        //Note, do not overload methods. getOpts() depends on stack location
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts == null || !opts.doOutput()) {
            return;
        }
        Debugger.logger.log(Level.INFO, String.format("[%s]=> %s",
                opts.getPrefix(), String.format(format, args)));
    }

    /**
     * Prints to the Debugging {@link Logger} at {@link Level#SEVERE} if
     * {@link Debugger#toggleOutput(boolean)} is set to {@code true}. It will
     * also report the error with the URL set via
     * {@link Debugger#setReportingURL(String)}. If that is {@code null},
     * nothing will be reported
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param error The {@link Throwable} to be printed
     * @param message The formatting string
     * @param args The formatting arguments
     */
    public static void error(Throwable error, String message, Object... args) {
        DebugOpts opts = Debugger.DebugUtil.getOpts();
        if (opts == null) {
            return;
        }
        if (!opts.doHideErrors()) {
            Debugger.logger.log(Level.SEVERE, String.format(message, args), error);
        }
        //Send JSON payload
        Debugger.DebugUtil.report(opts, error, String.format(message, args));
    }


    /**
     * Returns a JSON payload containing as much relevant server information as
     * possible (barring anything identifiable) and the error itself
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param opts The {@link DebugOpts} relevant to the current plugin context
     * @param error The {@link Throwable} to report
     * @param message The message relevant to the error
     * @return A new {@link JSONObject} payload
     */
    private static JSONObject getPayload(DebugOpts opts, Throwable error, String message) {
        JSONObject back = new JSONObject();
        Map<String, Object> additional = opts.attachInfo();
        back.put("project-type", "standalone");
        if (!additional.isEmpty()) {
            additional.forEach(back::put);
        }
        JSONObject system = new JSONObject();
        system.put("name", System.getProperty("os.name"));
        system.put("version", System.getProperty("os.version"));
        system.put("arch", System.getProperty("os.arch"));
        back.put("system", system);
        JSONObject java = new JSONObject();
        java.put("version", System.getProperty("java.version"));
        java.put("vendor", System.getProperty("java.vendor"));
        java.put("vendor-url", System.getProperty("java.vendor.url"));
        java.put("bit", System.getProperty("sun.arch.data.model"));
        back.put("java", java);
        back.put("message", message);
        back.put("error", Exceptions.readableStackTrace(error));
        return back;
    }

    /**
     * Sends a JSON payload to a URL specified by the string parameter
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param url The URL to report to
     * @param payload The JSON payload to send via POST
     * @throws IOException If the sending failed
     */
    private static void send(String url, JSONObject payload) throws IOException {
        URL loc = new URL(url);
        HttpURLConnection http = (HttpURLConnection) loc.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");
        http.setUseCaches(false);
        http.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(http.getOutputStream())) {
            wr.writeBytes(payload.toJSONString());
            wr.flush();
        }
    }

    /**
     * Represents internally stored debugging options for specific plugins
     * 
     * @since 0.1.0
     * @author 1Rogue
     * @version 0.1.0
     */
    public static class DebugOpts {

        protected String prefix;
        protected boolean output;
        protected boolean hideErrors;
        protected String url;

        /**
         * Constructor. Determines the logging prefix and initializes fields
         * 
         * @since 0.1.0
         * @version 0.1.0
         */
        public DebugOpts() {
            this.hideErrors = false;
            this.output = true;
            this.prefix = "Debug";
            this.url = null;
        }

        /**
         * Returns {@code true} if output is printed to the debug {@link Logger}
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @return {@code true} if output is printed
         */
        public boolean doOutput() {
            return this.output;
        }

        /**
         * Toggles whether or not to print information to the debugger
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @param output {@code true} to enable output
         */
        public void toggleOutput(boolean output) {
            this.output = output;
        }

        /**
         * Toggles whether or not to print errors to the debugger
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @param hide {@code true} to not print stack traces to console
         */
        public void hideErrors(boolean hide) {
            this.hideErrors = hide;
        }

        /**
         * Returns {@code true} if errors should not be printed to the debug
         * {@link Logger}
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @return {@code true} if errors are hidden
         */
        public boolean doHideErrors() {
            return this.hideErrors;
        }

        /**
         * Returns the URL that errors are reported to
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @return The error reporting URL
         */
        public String getUrl() {
            return this.url;
        }

        /**
         * Sets the URL to report errors to
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @param url The error reporting URL
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Returns the logging prefix used for debug output. This is typically
         * the plugin's name unless a prefix is specified in the plugin's
         * {@code plugin.yml} file.
         * 
         * @since 0.1.0
         * @version 0.1.0
         * 
         * @return The prefix used for debug output
         */
        public String getPrefix() {
            return this.prefix;
        }

        public JSONObject attachInfo() {
            return new JSONObject();
        }

    }

    public static final class DebugUtil {

        private static final DebugOpts OPTS = new DebugOpts();
        private static Supplier<DebugOpts> getOpts = () -> OPTS;

        /**
         * Reports an error to a specific reporting URL
         *
         * @since 0.1.0
         * @version 0.1.0
         *
         * @param opts The {@link DebugOpts} relevant to the current plugin context
         * @param error The {@link Throwable} to report
         * @param message The message relevant to the error
         */
        public static void report(DebugOpts opts, Throwable error, String message) {
            if (opts == null || opts.getUrl() == null) {
                return;
            }
            Scheduler.runAsyncTask(() -> {
                JSONObject out = Debugger.getPayload(opts, error, message);
                try {
                    Debugger.send(opts.getUrl(), out);
                } catch (IOException ex) {
                    Debugger.logger.log(Level.WARNING, "Unable to report error");
                    //Logger-generated errors should not be re-reported, and
                    //no ErrorManager is present for this instance
                }
            }, 0);
        }

        public static DebugOpts getOpts() {
            return getOpts.get();
        }

        public static void setOps(Supplier<DebugOpts> getOpts) {
            DebugUtil.getOpts = getOpts;
        }

    }

}

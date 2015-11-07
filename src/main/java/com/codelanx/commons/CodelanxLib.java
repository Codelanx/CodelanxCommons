/*
 * Copyright (C) 2015 CodeLanx , All Rights Reserved
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
 * along with this program. If not, see <https://creativecommons.org/licenses/>.
 */
package com.codelanx.commons;

import com.codelanx.commons.econ.VaultProxyListener;
import com.codelanx.commons.listener.ListenerManager;
import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.serialize.SerializationFactory;
import com.codelanx.commons.util.Reflections;
import com.codelanx.commons.util.Scheduler;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

/**
 * Main class. Ensures any services are initialized properly
 *
 * @since 0.0.1
 * @author 1Rogue
 * @version 0.1.0
 */
public class CodelanxLib extends JavaPlugin {

    static {
        SerializationFactory.registerClasses(SerializationFactory.getNativeSerializables());
    }

    /**
     * Reports metrics to <a href="http://mcstats.org/">MCStats</a>, and hooks
     * the plugin loggers for {@link Debugger}
     * <br><br>
     * {@inheritDoc}
     * 
     * @since 0.0.1
     * @version 0.1.0
     */
    @Override
    public void onEnable() {
        Debugger.hookBukkit();
        if (Reflections.findPluginJarfile("Vault") != null) {
            new VaultProxyListener(this).register();
        }
        try {
            new Metrics(this).start();
        } catch (IOException ex) {
            Debugger.error(ex, "Error reporting metrics");
        }
    }

    /**
     * Releases all currently registered listeners and cancels all Scheduler
     * tasks
     * <br><br>
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    @Override
    public void onDisable() {
        ListenerManager.release();
        Scheduler.cancelAllTasks();
        Scheduler.getService().shutdown();
    }

    /**
     * Returns the instance in use of this class as held interally by Bukkit
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The relevant {@link CodelanxLib} instance
     */
    public static CodelanxLib get() {
        return JavaPlugin.getPlugin(CodelanxLib.class);
    }

}

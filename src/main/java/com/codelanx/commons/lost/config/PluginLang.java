package com.codelanx.commons.lost.config;

import com.codelanx.commons.config.Lang;
import com.codelanx.commons.lost.implementers.Formatted;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Created by Rogue on 11/6/2015.
 */
public interface PluginLang extends Lang {


    /**
     * Returns an acceptable message format for the plugin at hand
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param plugin A {@link Plugin} that needs a format
     * @return A relevant {@link Lang} format
     */
    public static Lang getFormat(Plugin plugin) {
        return plugin instanceof Formatted
                ? ((Formatted) plugin).getFormat()
                : Lang.defaultFormat(plugin.getName());
    }

    /**
     * Automatically translates strings that contain color codes using the
     * '{@code &}' symbol
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param color String with un-converted color codes
     * @return string with correct chat colors included
     */
    public static String color(String color) {
        return ChatColor.translateAlternateColorCodes('&', color);
    }

    /**
     * Sends a formatted string and prepends the {@link Lang#getFormat} to
     * it.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The target to send to
     * @param message The message to colorize and send
     * @param args Arguments to supply to the {@link Lang} message
     */
    public static void sendMessage(CommandSender target, Lang message, Object... args) {
        Lang.sendMessage(target, message.getFormat(), message, args);
    }

    /**
     * Sends a message but does not include {@link Lang#getFormat}, instead it
     * uses a supplied format
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The target to send to
     * @param format The format provided
     * @param message The message to colorize and send
     * @param args Arguments to supply to the {@link Lang} message
     */
    public static void sendMessage(CommandSender target, Lang format, Lang message, Object... args) {
        if (target == null || format == null || message == null) {
            return;
        }
        String s = format.formatAndColor(message.format(args));
        if (!s.isEmpty()) {
            target.sendMessage(s);
        }
    }

    /**
     * Sends a raw message without additional formatting aside from translating
     * color codes
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The target to send to
     * @param message The message to colorize and send
     * @param args Arguments to supply to the {@link Lang} message
     */
    public static void sendRawMessage(CommandSender target, Lang message, Object... args) {
        if (message == null || target == null) {
            return;
        }
        String s = message.formatAndColor(args);
        if (!s.isEmpty()) {
            target.sendMessage(s);
        }
    }

    /**
     * Issues a "tellRaw" to a {@link Player} target with the supplied Lang
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The {@link Player} to send the JSON message to
     * @param message A {@link Lang} representing a JSON payload
     * @param args The arguments for the passed {@link Lang}
     */
    public static void tellRaw(Player target, Lang message, Object... args) {
        if (message == null || target == null) {
            return;
        }
        String s = message.format(args);
        if (!s.isEmpty()) {
            //Use command until proper api is in place
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellRaw " + target.getName() + " " + s);
        }
    }

    /**
     * Issues a "title" command to a {@link Player} target with the passed Lang
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The {@link Player} to send the JSON title to
     * @param message A {@link Lang} representing a JSON payload for the title
     * @param args The arguments for the passed {@link Lang}
     */
    public static void sendTitle(Player target, Lang message, Object... args) {
        if (message == null || target == null) {
            return;
        }
        String s = message.format(args);
        if (!s.isEmpty()) {
            //Use command until proper api is in place
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + target.getName() + " " + s); //inb4 "bukkit injection"
        }
    }

    /**
     * Formats a {@link Lang} enum constant with the supplied arguments, and
     * colors it
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param args The arguments to supply
     * @return The formatted string
     */
    default public String formatAndColor(Object... args) {
        return PluginLang.color(this.format(args));
    }

    //Convenience methods

    public static void sendMessage(OfflinePlayer target, Lang format, Lang message, Object... args) {
        if (target.isOnline()) {
            Lang.sendMessage((CommandSender) target.getPlayer(), format, message, args);
        }
    }
    public static void sendMessage(OfflinePlayer target, Lang message, Object... args) {
        if (target.isOnline()) {
            Lang.sendMessage((CommandSender) target.getPlayer(), message.getFormat(), message, args);
        }
    }

    public static void sendRawMessage(OfflinePlayer target, Lang message, Object... args) {
        if (target.isOnline()) {
            Lang.sendRawMessage((CommandSender) target.getPlayer(), message, args);
        }
    }

    public static void sendMessage(Player target, Lang format, Lang message, Object... args) {
        Lang.sendMessage((CommandSender) target, format, message, args);
    }

    public static void sendMessage(Player target, Lang message, Object... args) {
        Lang.sendMessage((CommandSender) target, message.getFormat(), message, args);
    }

    public static void sendRawMessage(Player target, Lang message, Object... args) {
        Lang.sendRawMessage((CommandSender) target, message, args);
    }
}

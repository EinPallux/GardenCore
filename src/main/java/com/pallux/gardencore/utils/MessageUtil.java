package com.pallux.gardencore.utils;

import com.pallux.gardencore.GardenCore;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageUtil {

    private MessageUtil() {}

    public static String format(String message, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ColorUtil.translate(message);
    }

    public static void send(CommandSender sender, String messagePath, Map<String, String> replacements) {
        GardenCore plugin = GardenCore.getInstance();
        String prefix = ColorUtil.translate(plugin.getConfigManager().getPrefix()) + " ";
        String msg = plugin.getConfigManager().getMessage(messagePath);
        msg = format(msg, replacements);
        sender.sendMessage(prefix + msg);
    }

    public static void send(CommandSender sender, String messagePath) {
        send(sender, messagePath, Map.of());
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.translate(message));
    }

    public static void broadcast(String message) {
        GardenCore.getInstance().getServer().broadcastMessage(ColorUtil.translate(message));
    }
}

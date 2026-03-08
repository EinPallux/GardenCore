package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.commands.AliasCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.PluginCommand;

import java.util.HashMap;
import java.util.Map;

public class AliasManager {

    private final GardenCore plugin;
    private final Map<String, String> aliases = new HashMap<>();

    public AliasManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public void registerAliases() {
        if (!plugin.getConfigManager().isFeatureEnabled("alias-commands")) return;

        aliases.clear();
        ConfigurationSection section = plugin.getConfigManager().getAliases().getConfigurationSection("aliases");
        if (section == null) return;

        for (String alias : section.getKeys(false)) {
            String command = section.getString(alias + ".command", "");
            if (command.isEmpty()) continue;

            aliases.put(alias.toLowerCase(), command);

            try {
                AliasCommand executor = new AliasCommand(plugin, command);
                plugin.getServer().getCommandMap().register(alias, "gardencore", new org.bukkit.command.Command(alias) {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                        return executor.onCommand(sender, this, commandLabel, args);
                    }

                    @Override
                    public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                        return executor.onTabComplete(sender, this, alias, args);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Could not register alias command '" + alias + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Registered " + aliases.size() + " alias commands.");
    }

    public Map<String, String> getAliases() {
        return Map.copyOf(aliases);
    }
}
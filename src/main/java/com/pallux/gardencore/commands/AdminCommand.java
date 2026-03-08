package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.ConfirmGui;
import com.pallux.gardencore.managers.UpgradeManager;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final GardenCore plugin;

    public AdminCommand(GardenCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gc.admin")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "give" -> handleGive(sender, args);
            case "take" -> handleTake(sender, args);
            case "upgrades" -> handleUpgrades(sender, args);
            case "reset" -> handleReset(sender, args);
            case "multi" -> handleMulti(sender, args);
            case "event" -> handleEvent(sender, args);
            case "reload" -> handleReload(sender);
            case "item" -> handleItem(sender, args);
            case "afkzone" -> handleAfkZone(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) { sendUsage(sender); return; }

        String type = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!hasData(sender, target)) return;

        switch (type) {
            case "fiber" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getFiberManager().setFiber(target.getUniqueId(), amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.send(sender, "fiber.set", Map.of("player", nameOf(target, args[2]), "amount", String.valueOf(amount)));
            }
            case "xp" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getDataManager().getPlayerData(target.getUniqueId()).setXp(amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7XP for &e" + nameOf(target, args[2]) + " &7set to &a" + amount);
            }
            case "level" -> {
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getDataManager().getPlayerData(target.getUniqueId()).setLevel(Math.max(1, amount));
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Level for &e" + nameOf(target, args[2]) + " &7set to &a" + amount);
            }
            default -> sendUsage(sender);
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) { sendUsage(sender); return; }

        String type = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!hasData(sender, target)) return;

        switch (type) {
            case "fiber" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getFiberManager().giveFiber(target.getUniqueId(), amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.send(sender, "fiber.give", Map.of("player", nameOf(target, args[2]), "amount", String.valueOf(amount)));
            }
            case "xp" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getDataManager().getPlayerData(target.getUniqueId()).addXp(amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Given &a" + amount + " &7XP to &e" + nameOf(target, args[2]));
            }
            case "level" -> {
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                var data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                data.setLevel(data.getLevel() + amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Given &a" + amount + " &7level(s) to &e" + nameOf(target, args[2]));
            }
            default -> sendUsage(sender);
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 4) { sendUsage(sender); return; }

        String type = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!hasData(sender, target)) return;

        switch (type) {
            case "fiber" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                plugin.getFiberManager().takeFiber(target.getUniqueId(), amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.send(sender, "fiber.take", Map.of("player", nameOf(target, args[2]), "amount", String.valueOf(amount)));
            }
            case "xp" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                var data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                data.setXp(Math.max(0, data.getXp() - amount));
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Taken &a" + amount + " &7XP from &e" + nameOf(target, args[2]));
            }
            case "level" -> {
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                var data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                data.setLevel(Math.max(1, data.getLevel() - amount));
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Taken &a" + amount + " &7level(s) from &e" + nameOf(target, args[2]));
            }
            default -> sendUsage(sender);
        }
    }

    private void handleUpgrades(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("set")) { sendUsage(sender); return; }

        UpgradeManager.UpgradeType type = UpgradeManager.fromString(args[2]);
        if (type == null) { sendUsage(sender); return; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (!hasData(sender, target)) return;

        int level;
        try { level = Integer.parseInt(args[4]); } catch (NumberFormatException e) {
            MessageUtil.send(sender, "invalid-number");
            return;
        }

        level = Math.max(0, Math.min(level, plugin.getUpgradeManager().getMaxLevel(type)));
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        switch (type) {
            case FIBER_AMOUNT -> data.setFiberAmountUpgrade(level);
            case MATERIAL_AMOUNT -> data.setMaterialAmountUpgrade(level);
            case MATERIAL_CHANCE -> data.setMaterialChanceUpgrade(level);
            case CROP_COOLDOWN -> data.setCropCooldownUpgrade(level);
        }

        plugin.getDataManager().saveAsync();
        MessageUtil.send(sender, "upgrades.set", Map.of(
                "upgrade", plugin.getUpgradeManager().getDisplayName(type),
                "player", nameOf(target, args[3]),
                "level", String.valueOf(level)
        ));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) { sendUsage(sender); return; }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "command-console-only");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!hasData(player, target)) return;

        String type = args[1].toLowerCase();
        String title = plugin.getConfigManager().getMessage("admin.reset-confirm-title");

        Runnable action = switch (type) {
            case "upgrades" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetUpgrades();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "upgrades.reset-upgrades", Map.of("player", nameOf(target, args[2])));
            };
            case "fiber" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetFiber();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "upgrades.reset-fiber", Map.of("player", nameOf(target, args[2])));
            };
            case "all" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetAll();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "upgrades.reset-all", Map.of("player", nameOf(target, args[2])));
            };
            default -> null;
        };

        if (action == null) { sendUsage(player); return; }

        new ConfirmGui(plugin, player, title, action).open();
    }

    private void handleMulti(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("add")) { sendUsage(sender); return; }

        UpgradeManager.UpgradeType type = UpgradeManager.fromString(args[2]);
        if (type == null) { sendUsage(sender); return; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (!hasData(sender, target)) return;

        double percent;
        try { percent = Double.parseDouble(args[4]); } catch (NumberFormatException e) {
            MessageUtil.send(sender, "invalid-number");
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        switch (type) {
            case FIBER_AMOUNT -> data.addBonusFiberMultiplier(percent);
            case MATERIAL_AMOUNT -> data.addBonusMaterialAmountMultiplier(percent);
            case MATERIAL_CHANCE -> data.addBonusMaterialChanceMultiplier(percent);
        }

        plugin.getDataManager().saveAsync();
        MessageUtil.sendRaw(sender, "&7Added &a+" + percent + "% &7" + plugin.getUpgradeManager().getDisplayName(type) + " multiplier to &e" + nameOf(target, args[3]));
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) { sendUsage(sender); return; }

        String action = args[1].toLowerCase();

        switch (action) {
            case "start" -> {
                if (args.length < 3) { sendUsage(sender); return; }
                String key = args[2];
                boolean started = plugin.getEventManager().startEventByKey(key);
                if (!started) {
                    MessageUtil.sendRaw(sender, "&cUnknown event key: &e" + key + "&c. Check events.yml.");
                } else {
                    MessageUtil.sendRaw(sender, "&7Event &e" + key + " &7has been started.");
                }
            }
            case "stop" -> {
                boolean stopped = plugin.getEventManager().stopCurrentEvent();
                if (!stopped) {
                    MessageUtil.sendRaw(sender, "&cThere is no active event to stop.");
                } else {
                    MessageUtil.sendRaw(sender, "&7The current event has been stopped.");
                }
            }
            default -> sendUsage(sender);
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadAll();
        MessageUtil.sendRaw(sender, "&aAll GardenCore config files have been reloaded.");
    }

    private void handleAfkZone(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) { sendUsage(sender); return; }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "command-console-only");
            return;
        }

        try {
            com.sk89q.worldedit.bukkit.WorldEditPlugin we =
                    (com.sk89q.worldedit.bukkit.WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (we == null) {
                we = (com.sk89q.worldedit.bukkit.WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
            }
            if (we == null) {
                MessageUtil.sendRaw(sender, "&cWorldEdit / FastAsyncWorldEdit is not installed or enabled.");
                return;
            }

            com.sk89q.worldedit.regions.Region region =
                    we.getSession(player).getSelection(we.getSession(player).getSelectionWorld());

            if (region == null) {
                MessageUtil.sendRaw(sender, "&cYou don't have a WorldEdit selection. Make one with the WE wand first.");
                return;
            }

            com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
            String worldName = player.getWorld().getName();

            plugin.getAfkZoneManager().setZone(worldName,
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ());

            MessageUtil.sendRaw(sender, "&aAFK Zone set successfully in world &e" + worldName
                    + " &afrom &e(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ")"
                    + " &ato &e(" + max.getX() + ", " + max.getY() + ", " + max.getZ() + ")");

        } catch (Exception e) {
            MessageUtil.sendRaw(sender, "&cFailed to get selection: &7" + e.getMessage());
        }
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) { sendUsage(sender); return; }

        String key = args[2];
        if (plugin.getItemManager().getItem(key).isEmpty()) {
            MessageUtil.sendRaw(sender, "&cUnknown item key: &e" + key + "&c. Check items.yml.");
            return;
        }

        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            MessageUtil.send(sender, "player-not-found", Map.of("player", args[3]));
            return;
        }

        int amount = 1;
        if (args.length >= 5) {
            try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
        }

        plugin.getItemManager().giveItem(target, key, amount);
        MessageUtil.sendRaw(sender, "&7Given &a" + amount + "x &e" + key + " &7to &a" + target.getName());
    }

    private String nameOf(OfflinePlayer p, String fallback) {
        return p.getName() != null ? p.getName() : fallback;
    }

    private boolean hasData(CommandSender sender, OfflinePlayer target) {
        if (target.getName() == null) {
            MessageUtil.send(sender, "player-not-found", Map.of("player", target.getUniqueId().toString()));
            return false;
        }
        plugin.getDataManager().getPlayerData(target.getUniqueId());
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(new String[]{
                com.pallux.gardencore.utils.ColorUtil.translate("&8&m                                &r"),
                com.pallux.gardencore.utils.ColorUtil.translate("&#a8ff78&lGardenCore Admin Commands"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca set <fiber|xp|level> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca give <fiber|xp|level> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca take <fiber|xp|level> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca upgrades set <type> <player> <level>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca reset <upgrades|fiber|all> <player>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca multi add <type> <player> <percent>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca event start <event>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca event stop"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca item give <item> <player> [amount]"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca afkzone set"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca reload"),
                com.pallux.gardencore.utils.ColorUtil.translate("&8&m                                &r")
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gc.admin")) return List.of();

        if (args.length == 1) {
            return List.of("set", "give", "take", "upgrades", "reset", "multi", "event", "item", "afkzone", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "set", "give", "take" -> List.of("fiber", "xp", "level").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "upgrades" -> List.of("set").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "reset" -> List.of("upgrades", "fiber", "all").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "multi" -> List.of("add").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "event" -> List.of("start", "stop").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "item" -> List.of("give").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                case "afkzone" -> List.of("set").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "set", "give", "take", "reset" -> getOnlinePlayers(args[2]);
                case "upgrades", "multi" -> List.of("fiber_amount", "material_amount", "material_chance", "crop_cooldown").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
                case "event" -> {
                    if (args[1].equalsIgnoreCase("start")) {
                        yield plugin.getEventManager().getAvailableEventKeys().stream()
                                .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
                    }
                    yield List.of();
                }
                case "item" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        yield plugin.getItemManager().getItemKeys().stream()
                                .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
                    }
                    yield List.of();
                }
                default -> List.of();
            };
        }

        if (args.length == 4) {
            return switch (args[0].toLowerCase()) {
                case "upgrades", "multi" -> getOnlinePlayers(args[3]);
                case "item" -> {
                    if (args[1].equalsIgnoreCase("give")) yield getOnlinePlayers(args[3]);
                    yield List.of();
                }
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> getOnlinePlayers(String prefix) {
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(p.getName());
            }
        }
        return result;
    }
}
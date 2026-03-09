package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.ConfirmGui;
import com.pallux.gardencore.managers.ElderManager;
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

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> MATERIAL_TYPES = List.of("driftwood", "moss", "reed", "clover");
    private static final List<String> SET_GIVE_TAKE_TYPES = List.of("fiber", "xp", "level", "driftwood", "moss", "reed", "clover");
    private static final List<String> ELDER_PERK_TYPES = List.of("fiber_amount", "material_amount", "xp_gain", "material_chance");

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

        switch (args[0].toLowerCase()) {
            case "set"      -> handleSet(sender, args);
            case "give"     -> handleGive(sender, args);
            case "take"     -> handleTake(sender, args);
            case "upgrades" -> handleUpgrades(sender, args);
            case "elder"    -> handleElder(sender, args);
            case "reset"    -> handleReset(sender, args);
            case "multi"    -> handleMulti(sender, args);
            case "event"    -> handleEvent(sender, args);
            case "reload"   -> handleReload(sender);
            case "item"     -> handleItem(sender, args);
            case "afkzone"  -> handleAfkZone(sender, args);
            default         -> sendUsage(sender);
        }

        return true;
    }

    // ── /gca set ──────────────────────────────────────────────

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
            case "driftwood", "moss", "reed", "clover" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                amount = Math.max(0, amount);
                setMaterial(plugin.getDataManager().getPlayerData(target.getUniqueId()), type, amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7" + capitalize(type) + " for &e" + nameOf(target, args[2]) + " &7set to &a" + amount);
            }
            default -> sendUsage(sender);
        }
    }

    // ── /gca give ─────────────────────────────────────────────

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
            case "driftwood", "moss", "reed", "clover" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                if (amount < 0) { MessageUtil.send(sender, "invalid-number"); return; }
                addMaterial(plugin.getDataManager().getPlayerData(target.getUniqueId()), type, amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Given &a" + amount + " &7" + capitalize(type) + " to &e" + nameOf(target, args[2]));
            }
            default -> sendUsage(sender);
        }
    }

    // ── /gca take ─────────────────────────────────────────────

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
            case "driftwood", "moss", "reed", "clover" -> {
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return; }
                if (amount < 0) { MessageUtil.send(sender, "invalid-number"); return; }
                var data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                takeMaterial(data, type, amount);
                plugin.getDataManager().saveAsync();
                MessageUtil.sendRaw(sender, "&7Taken &a" + amount + " &7" + capitalize(type) + " from &e" + nameOf(target, args[2]));
            }
            default -> sendUsage(sender);
        }
    }

    // ── /gca upgrades set ─────────────────────────────────────

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
            case FIBER_AMOUNT    -> data.setFiberAmountUpgrade(level);
            case MATERIAL_AMOUNT -> data.setMaterialAmountUpgrade(level);
            case MATERIAL_CHANCE -> data.setMaterialChanceUpgrade(level);
            case CROP_COOLDOWN   -> data.setCropCooldownUpgrade(level);
        }

        plugin.getDataManager().saveAsync();
        MessageUtil.send(sender, "upgrades.set", Map.of(
                "upgrade", plugin.getUpgradeManager().getDisplayName(type),
                "player", nameOf(target, args[3]),
                "level", String.valueOf(level)
        ));
    }

    // ── /gca elder set ────────────────────────────────────────

    private void handleElder(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("set")) { sendUsage(sender); return; }

        ElderManager.ElderPerkType type = ElderManager.fromString(args[2]);
        if (type == null) {
            MessageUtil.sendRaw(sender, "&cUnknown Elder perk type: &e" + args[2]
                    + "&c. Valid types: fiber_amount, material_amount, xp_gain, material_chance");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (!hasData(sender, target)) return;

        int level;
        try { level = Integer.parseInt(args[4]); } catch (NumberFormatException e) {
            MessageUtil.send(sender, "invalid-number");
            return;
        }

        int max = plugin.getElderManager().getMaxLevel(type);
        level = Math.max(0, Math.min(level, max));
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        switch (type) {
            case FIBER_AMOUNT    -> data.setElderFiberLevel(level);
            case MATERIAL_AMOUNT -> data.setElderMaterialAmountLevel(level);
            case XP_GAIN         -> data.setElderXpGainLevel(level);
            case MATERIAL_CHANCE -> data.setElderMaterialChanceLevel(level);
        }

        plugin.getDataManager().saveAsync();
        MessageUtil.sendRaw(sender, "&7Elder perk &e" + plugin.getElderManager().getDisplayName(type)
                + " &7for &e" + nameOf(target, args[3]) + " &7set to level &a" + level);
    }

    // ── /gca reset ────────────────────────────────────────────

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
            case "material" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetMaterials();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "admin.reset-material", Map.of("player", nameOf(target, args[2])));
            };
            case "research" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetResearch();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "admin.reset-research", Map.of("player", nameOf(target, args[2])));
            };
            case "elder" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetElder();
                plugin.getDataManager().saveAsync();
                MessageUtil.send(player, "admin.reset-elder", Map.of("player", nameOf(target, args[2])));
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

    // ── /gca multi ────────────────────────────────────────────

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
            case FIBER_AMOUNT    -> data.addBonusFiberMultiplier(percent);
            case MATERIAL_AMOUNT -> data.addBonusMaterialAmountMultiplier(percent);
            case MATERIAL_CHANCE -> data.addBonusMaterialChanceMultiplier(percent);
            case CROP_COOLDOWN   -> { /* no bonus multiplier for cooldown */ }
        }

        plugin.getDataManager().saveAsync();
        MessageUtil.sendRaw(sender, "&7Added &a+" + percent + "% &7"
                + plugin.getUpgradeManager().getDisplayName(type) + " multiplier to &e" + nameOf(target, args[3]));
    }

    // ── /gca event ────────────────────────────────────────────

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) { sendUsage(sender); return; }

        switch (args[1].toLowerCase()) {
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

    // ── /gca reload ───────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadAll();
        MessageUtil.sendRaw(sender, "&aAll GardenCore config files have been reloaded.");
    }

    // ── /gca afkzone ──────────────────────────────────────────

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

    // ── /gca item ─────────────────────────────────────────────

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

    // ── Material helpers ──────────────────────────────────────

    private void setMaterial(PlayerData data, String type, double amount) {
        switch (type) {
            case "driftwood" -> data.setDriftwood(amount);
            case "moss"      -> data.setMoss(amount);
            case "reed"      -> data.setReed(amount);
            case "clover"    -> data.setClover(amount);
        }
    }

    private void addMaterial(PlayerData data, String type, double amount) {
        switch (type) {
            case "driftwood" -> data.addDriftwood(amount);
            case "moss"      -> data.addMoss(amount);
            case "reed"      -> data.addReed(amount);
            case "clover"    -> data.addClover(amount);
        }
    }

    private void takeMaterial(PlayerData data, String type, double amount) {
        switch (type) {
            case "driftwood" -> data.setDriftwood(Math.max(0, data.getDriftwood() - amount));
            case "moss"      -> data.setMoss(Math.max(0, data.getMoss() - amount));
            case "reed"      -> data.setReed(Math.max(0, data.getReed() - amount));
            case "clover"    -> data.setClover(Math.max(0, data.getClover() - amount));
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Misc helpers ──────────────────────────────────────────

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
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca set <fiber|xp|level|driftwood|moss|reed|clover> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca give <fiber|xp|level|driftwood|moss|reed|clover> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca take <fiber|xp|level|driftwood|moss|reed|clover> <player> <amount>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca upgrades set <type> <player> <level>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca elder set <type> <player> <level>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca reset <upgrades|fiber|material|research|elder|all> <player>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca multi add <type> <player> <percent>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca event start <event>"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca event stop"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca item give <item> <player> [amount]"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca afkzone set"),
                com.pallux.gardencore.utils.ColorUtil.translate("&e/gca reload"),
                com.pallux.gardencore.utils.ColorUtil.translate("&8&m                                &r")
        });
    }

    // ── Tab completion ────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gc.admin")) return List.of();

        if (args.length == 1) {
            return filter(List.of("set", "give", "take", "upgrades", "elder", "reset", "multi",
                    "event", "item", "afkzone", "reload"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "set", "give", "take" -> filter(SET_GIVE_TAKE_TYPES, args[1]);
                case "upgrades"            -> filter(List.of("set"), args[1]);
                case "elder"               -> filter(List.of("set"), args[1]);
                case "reset"               -> filter(List.of("upgrades", "fiber", "material", "research", "elder", "all"), args[1]);
                case "multi"               -> filter(List.of("add"), args[1]);
                case "event"               -> filter(List.of("start", "stop"), args[1]);
                case "item"                -> filter(List.of("give"), args[1]);
                case "afkzone"             -> filter(List.of("set"), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "set", "give", "take", "reset" -> getOnlinePlayers(args[2]);
                case "upgrades", "multi" -> filter(List.of("fiber_amount", "material_amount", "material_chance", "crop_cooldown"), args[2]);
                case "elder" -> {
                    if (args[1].equalsIgnoreCase("set")) yield filter(ELDER_PERK_TYPES, args[2]);
                    yield List.of();
                }
                case "event" -> {
                    if (args[1].equalsIgnoreCase("start")) {
                        yield filter(plugin.getEventManager().getAvailableEventKeys(), args[2]);
                    }
                    yield List.of();
                }
                case "item" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        yield filter(new ArrayList<>(plugin.getItemManager().getItemKeys()), args[2]);
                    }
                    yield List.of();
                }
                default -> List.of();
            };
        }

        if (args.length == 4) {
            return switch (args[0].toLowerCase()) {
                case "upgrades", "multi" -> getOnlinePlayers(args[3]);
                case "elder" -> {
                    if (args[1].equalsIgnoreCase("set")) yield getOnlinePlayers(args[3]);
                    yield List.of();
                }
                case "item" -> {
                    if (args[1].equalsIgnoreCase("give")) yield getOnlinePlayers(args[3]);
                    yield List.of();
                }
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(s -> s.startsWith(prefix.toLowerCase())).toList();
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
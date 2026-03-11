package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.ConfirmGui;
import com.pallux.gardencore.managers.ElderManager;
import com.pallux.gardencore.managers.UpgradeManager;
import com.pallux.gardencore.models.BossData;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
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

    private static final List<String> CURRENCY_TYPES = List.of(
            "fiber", "xp", "level", "driftwood", "moss", "reed", "clover", "pet"
    );
    private static final List<String> RESET_TYPES = List.of(
            "upgrades", "fiber", "materials", "research", "elder", "all"
    );
    private static final List<String> UPGRADE_TYPES = List.of(
            "fiber_amount", "material_amount", "material_chance", "crop_cooldown"
    );
    private static final List<String> ELDER_TYPES = List.of(
            "fiber_amount", "material_amount", "xp_gain", "material_chance"
    );
    private static final List<String> PET_RARITIES = List.of(
            "none", "common", "uncommon", "rare", "epic", "legendary", "mythic", "divine"
    );

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
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "player"  -> handlePlayer(sender, args);
            case "event"   -> handleEvent(sender, args);
            case "item"    -> handleItem(sender, args);
            case "afkzone" -> handleAfkZone(sender, args);
            case "boss"    -> handleBoss(sender, args);
            case "reload"  -> handleReload(sender);
            default        -> sendHelp(sender);
        }
        return true;
    }

    private void handleBoss(CommandSender sender, String[] args) {
        if (args.length < 2) { sendBossHelp(sender); return; }
        switch (args[1].toLowerCase()) {
            case "set"     -> handleBossSet(sender, args);
            case "spawn"   -> handleBossSpawn(sender, args);
            case "despawn" -> handleBossDespawn(sender, args);
            default        -> sendBossHelp(sender);
        }
    }

    private void handleBossSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "command-console-only");
            return;
        }
        if (args.length < 3) { sendBossHelp(sender); return; }

        String key = args[2].toLowerCase();
        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null) {
            err(sender, "Unknown boss key &e" + key + "&c. Check bosses.yml.");
            return;
        }

        try {
            com.sk89q.worldedit.bukkit.WorldEditPlugin we =
                    (com.sk89q.worldedit.bukkit.WorldEditPlugin)
                            plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (we == null)
                we = (com.sk89q.worldedit.bukkit.WorldEditPlugin)
                        plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (we == null) {
                err(sender, "WorldEdit / FastAsyncWorldEdit is not installed.");
                return;
            }

            com.sk89q.worldedit.regions.Region region =
                    we.getSession(player).getSelection(we.getSession(player).getSelectionWorld());
            if (region == null) {
                err(sender, "You don't have a WorldEdit selection. Use the wand first.");
                return;
            }

            com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
            String worldName = player.getWorld().getName();

            boss.setZone(worldName,
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ());

            plugin.getBossManager().saveZone(boss);

            ok(sender, "Boss zone for &e" + key + " &7set in &e" + worldName
                    + " &7from &e(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ")"
                    + " &7to &e(" + max.getX() + ", " + max.getY() + ", " + max.getZ() + ")");

        } catch (Exception e) {
            err(sender, "Failed to get selection: " + e.getMessage());
        }
    }

    private void handleBossSpawn(CommandSender sender, String[] args) {
        if (args.length < 3) { sendBossHelp(sender); return; }
        String key = args[2].toLowerCase();
        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null) {
            err(sender, "Unknown boss key &e" + key + "&c. Check bosses.yml.");
            return;
        }
        if (boss.isActive()) {
            err(sender, "Boss &e" + key + " &cis already active.");
            return;
        }
        if (!boss.isZoneSet()) {
            err(sender, "Boss &e" + key + " &chas no zone set. Use &e/gca boss set " + key + " &cfirst.");
            return;
        }
        boolean spawned = plugin.getBossManager().forceSpawn(key);
        if (spawned) {
            ok(sender, "Boss &e" + key + " &7spawned.");
        } else {
            err(sender, "Failed to spawn boss &e" + key + "&c.");
        }
    }

    private void handleBossDespawn(CommandSender sender, String[] args) {
        if (args.length < 3) { sendBossHelp(sender); return; }
        String key = args[2].toLowerCase();
        boolean despawned = plugin.getBossManager().forceDespawn(key);
        if (despawned) {
            ok(sender, "Boss &e" + key + " &7despawned.");
        } else {
            err(sender, "Boss &e" + key + " &cis not currently active or does not exist.");
        }
    }

    private void handlePlayer(CommandSender sender, String[] args) {
        if (args.length < 3) { sendPlayerHelp(sender); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!ensureData(sender, target, args[1])) return;
        String action = args[2].toLowerCase();
        switch (action) {
            case "set"     -> handlePlayerSet(sender, target, args);
            case "give"    -> handlePlayerGive(sender, target, args);
            case "take"    -> handlePlayerTake(sender, target, args);
            case "reset"   -> handlePlayerReset(sender, target, args);
            case "upgrade" -> handlePlayerUpgrade(sender, target, args);
            case "elder"   -> handlePlayerElder(sender, target, args);
            case "bonus"   -> handlePlayerBonus(sender, target, args);
            default        -> sendPlayerHelp(sender);
        }
    }

    private void handlePlayerSet(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        String type = args[3].toLowerCase();
        String name = nameOf(target, args[1]);
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case "fiber" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                plugin.getFiberManager().setFiber(target.getUniqueId(), amount);
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(sender, "fiber.set", Map.of("player", name, "amount", args[4]));
            }
            case "xp" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                data.setXp(amount); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "XP for &e" + name + " &7set to &a" + args[4]);
            }
            case "level" -> {
                int amount = parseInt(sender, args[4]); if (amount == Integer.MIN_VALUE) return;
                data.setLevel(Math.max(1, amount)); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Level for &e" + name + " &7set to &a" + amount);
            }
            case "driftwood", "moss", "reed", "clover" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                setMaterial(data, type, Math.max(0, amount)); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, capitalize(type) + " for &e" + name + " &7set to &a" + amount);
            }
            case "pet" -> {
                com.pallux.gardencore.models.PetRarity rarity = parsePetRarity(sender, args[4]);
                if (rarity == null) return;
                data.setPetRarity(rarity);
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                if (onlineTarget != null) {
                    if (rarity == com.pallux.gardencore.models.PetRarity.NONE) {
                        plugin.getPetCosmeticManager().despawn(target.getUniqueId());
                    } else {
                        plugin.getPetCosmeticManager().refresh(onlineTarget);
                    }
                }
                ok(sender, "Pet for &e" + name + " &7set to &a" + rarity.name());
            }
            default -> sendPlayerHelp(sender);
        }
    }

    private void handlePlayerGive(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        String type = args[3].toLowerCase();
        String name = nameOf(target, args[1]);
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case "fiber" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                plugin.getFiberManager().giveFiber(target.getUniqueId(), amount);
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(sender, "fiber.give", Map.of("player", name, "amount", args[4]));
            }
            case "xp" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                data.addXp(amount); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Given &a" + args[4] + " &7XP to &e" + name);
            }
            case "level" -> {
                int amount = parseInt(sender, args[4]); if (amount == Integer.MIN_VALUE) return;
                data.setLevel(data.getLevel() + amount); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Given &a" + amount + " &7level(s) to &e" + name);
            }
            case "driftwood", "moss", "reed", "clover" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                if (amount < 0) { MessageUtil.send(sender, "invalid-number"); return; }
                addMaterial(data, type, amount); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Given &a" + args[4] + " &7" + capitalize(type) + " to &e" + name);
            }
            default -> sendPlayerHelp(sender);
        }
    }

    private void handlePlayerTake(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        String type = args[3].toLowerCase();
        String name = nameOf(target, args[1]);
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case "fiber" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                plugin.getFiberManager().takeFiber(target.getUniqueId(), amount);
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(sender, "fiber.take", Map.of("player", name, "amount", args[4]));
            }
            case "xp" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                data.setXp(Math.max(0, data.getXp() - amount)); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Taken &a" + args[4] + " &7XP from &e" + name);
            }
            case "level" -> {
                int amount = parseInt(sender, args[4]); if (amount == Integer.MIN_VALUE) return;
                data.setLevel(Math.max(1, data.getLevel() - amount)); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Taken &a" + amount + " &7level(s) from &e" + name);
            }
            case "driftwood", "moss", "reed", "clover" -> {
                double amount = parseDouble(sender, args[4]); if (Double.isNaN(amount)) return;
                if (amount < 0) { MessageUtil.send(sender, "invalid-number"); return; }
                takeMaterial(data, type, amount); plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                ok(sender, "Taken &a" + args[4] + " &7" + capitalize(type) + " from &e" + name);
            }
            default -> sendPlayerHelp(sender);
        }
    }

    private void handlePlayerReset(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 4) { sendPlayerHelp(sender); return; }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "command-console-only");
            return;
        }
        String type = args[3].toLowerCase();
        String name = nameOf(target, args[1]);
        String title = plugin.getConfigManager().getMessage("admin.reset-confirm-title");

        Runnable action = switch (type) {
            case "upgrades" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetUpgrades();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "upgrades.reset-upgrades", Map.of("player", name));
            };
            case "fiber" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetFiber();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "upgrades.reset-fiber", Map.of("player", name));
            };
            case "materials" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetMaterials();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "admin.reset-material", Map.of("player", name));
            };
            case "research" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetResearch();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "admin.reset-research", Map.of("player", name));
            };
            case "elder" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetElder();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "admin.reset-elder", Map.of("player", name));
            };
            case "all" -> () -> {
                plugin.getDataManager().getPlayerData(target.getUniqueId()).resetAll();
                plugin.getDataManager().savePlayerAsync(target.getUniqueId());
                MessageUtil.send(player, "upgrades.reset-all", Map.of("player", name));
                Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                if (onlineTarget != null) {
                    plugin.getPetCosmeticManager().despawn(target.getUniqueId());
                }
            };
            default -> null;
        };

        if (action == null) { sendPlayerHelp(sender); return; }
        new ConfirmGui(plugin, player, title, action).open();
    }

    private void handlePlayerUpgrade(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        UpgradeManager.UpgradeType type = UpgradeManager.fromString(args[3]);
        if (type == null) { sendPlayerHelp(sender); return; }
        int level = parseInt(sender, args[4]); if (level == Integer.MIN_VALUE) return;
        level = Math.max(0, Math.min(level, plugin.getUpgradeManager().getMaxLevel(type)));
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case FIBER_AMOUNT    -> data.setFiberAmountUpgrade(level);
            case MATERIAL_AMOUNT -> data.setMaterialAmountUpgrade(level);
            case MATERIAL_CHANCE -> data.setMaterialChanceUpgrade(level);
            case CROP_COOLDOWN   -> data.setCropCooldownUpgrade(level);
        }
        plugin.getDataManager().savePlayerAsync(target.getUniqueId());
        MessageUtil.send(sender, "upgrades.set", Map.of(
                "upgrade", plugin.getUpgradeManager().getDisplayName(type),
                "player",  nameOf(target, args[1]),
                "level",   String.valueOf(level)
        ));
    }

    private void handlePlayerElder(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        ElderManager.ElderPerkType type = ElderManager.fromString(args[3]);
        if (type == null) {
            err(sender, "Unknown Elder perk. Valid: fiber_amount, material_amount, xp_gain, material_chance");
            return;
        }
        int level = parseInt(sender, args[4]); if (level == Integer.MIN_VALUE) return;
        level = Math.max(0, Math.min(level, plugin.getElderManager().getMaxLevel(type)));
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case FIBER_AMOUNT    -> data.setElderFiberLevel(level);
            case MATERIAL_AMOUNT -> data.setElderMaterialAmountLevel(level);
            case XP_GAIN         -> data.setElderXpGainLevel(level);
            case MATERIAL_CHANCE -> data.setElderMaterialChanceLevel(level);
        }
        plugin.getDataManager().savePlayerAsync(target.getUniqueId());
        ok(sender, "Elder perk &e" + plugin.getElderManager().getDisplayName(type)
                + " &7for &e" + nameOf(target, args[1]) + " &7set to level &a" + level);
    }

    private void handlePlayerBonus(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) { sendPlayerHelp(sender); return; }
        UpgradeManager.UpgradeType type = UpgradeManager.fromString(args[3]);
        if (type == null) { sendPlayerHelp(sender); return; }
        double percent = parseDouble(sender, args[4]); if (Double.isNaN(percent)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (type) {
            case FIBER_AMOUNT    -> data.addBonusFiberMultiplier(percent);
            case MATERIAL_AMOUNT -> data.addBonusMaterialAmountMultiplier(percent);
            case MATERIAL_CHANCE -> data.addBonusMaterialChanceMultiplier(percent);
            case CROP_COOLDOWN   -> { err(sender, "Bonus multiplier is not supported for Crop Cooldown."); return; }
        }
        plugin.getDataManager().savePlayerAsync(target.getUniqueId());
        ok(sender, "Added &a+" + percent + "% &7"
                + plugin.getUpgradeManager().getDisplayName(type)
                + " bonus to &e" + nameOf(target, args[1]));
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }
        switch (args[1].toLowerCase()) {
            case "start" -> {
                if (args.length < 3) { sendHelp(sender); return; }
                String key = args[2];
                if (!plugin.getEventManager().startEventByKey(key)) {
                    err(sender, "Unknown event key &e" + key + "&c. Check events.yml.");
                } else { ok(sender, "Event &e" + key + " &7started."); }
            }
            case "stop" -> {
                if (!plugin.getEventManager().stopCurrentEvent()) {
                    err(sender, "No scheduled event is currently active.");
                } else { ok(sender, "Current event stopped."); }
            }
            default -> sendHelp(sender);
        }
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 3) { sendHelp(sender); return; }
        String key = args[1];
        if (plugin.getItemManager().getItem(key).isEmpty()) {
            err(sender, "Unknown item key &e" + key + "&c. Check items.yml.");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "player-not-found", Map.of("player", args[2]));
            return;
        }
        int amount = 1;
        if (args.length >= 4) { amount = parseInt(sender, args[3]); if (amount == Integer.MIN_VALUE) return; }
        plugin.getItemManager().giveItem(target, key, amount);
        ok(sender, "Given &a" + amount + "x &e" + key + " &7to &a" + target.getName());
    }

    private void handleAfkZone(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) { sendHelp(sender); return; }
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "command-console-only"); return; }
        try {
            com.sk89q.worldedit.bukkit.WorldEditPlugin we =
                    (com.sk89q.worldedit.bukkit.WorldEditPlugin)
                            plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (we == null)
                we = (com.sk89q.worldedit.bukkit.WorldEditPlugin)
                        plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (we == null) { err(sender, "WorldEdit / FastAsyncWorldEdit is not installed."); return; }
            com.sk89q.worldedit.regions.Region region =
                    we.getSession(player).getSelection(we.getSession(player).getSelectionWorld());
            if (region == null) { err(sender, "You don't have a WorldEdit selection. Use the wand first."); return; }
            com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
            String worldName = player.getWorld().getName();
            plugin.getAfkZoneManager().setZone(worldName,
                    min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
            ok(sender, "AFK Zone set in &e" + worldName
                    + " &7from &e(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ")"
                    + " &7to &e(" + max.getX() + ", " + max.getY() + ", " + max.getZ() + ")");
        } catch (Exception e) { err(sender, "Failed to get selection: " + e.getMessage()); }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadAll();
        plugin.getPetCosmeticManager().refreshAll();
        plugin.getBossManager().loadBosses();
        ok(sender, "All config files reloaded.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lines(
                "&8&m                                          ",
                "&#a8ff78&lGardenCore &7Admin Commands &8— &7/gca",
                "",
                "&#FFD700Player Management",
                "&e/gca player <player> set    &7<type> <amount>",
                "&e/gca player <player> give   &7<type> <amount>",
                "&e/gca player <player> take   &7<type> <amount>",
                "&e/gca player <player> reset  &7<upgrades|fiber|materials|research|elder|all>",
                "&e/gca player <player> upgrade &7<type> <level>",
                "&e/gca player <player> elder  &7<type> <level>",
                "&e/gca player <player> bonus  &7<type> <percent>",
                "&8  types: fiber, xp, level, driftwood, moss, reed, clover",
                "",
                "&#FFD700Events",
                "&e/gca event start &7<key>",
                "&e/gca event stop",
                "",
                "&#FFD700Bosses",
                "&e/gca boss set     &7<key>  &8— set zone from WorldEdit selection",
                "&e/gca boss spawn   &7<key>  &8— force-spawn a boss",
                "&e/gca boss despawn &7<key>  &8— force-despawn a boss",
                "",
                "&#FFD700Utility",
                "&e/gca item <key> <player> &7[amount]",
                "&e/gca afkzone set",
                "&e/gca reload",
                "&8&m                                          "
        ));
    }

    private void sendPlayerHelp(CommandSender sender) {
        sender.sendMessage(lines(
                "&8&m                                          ",
                "&#a8ff78&lGardenCore &7— &7/gca player <player> <action>",
                "",
                "&e set    &7<fiber|xp|level|driftwood|moss|reed|clover> <amount>",
                "&e set    &7pet <none|common|uncommon|rare|epic|legendary|mythic|divine>",
                "&e give   &7<fiber|xp|level|driftwood|moss|reed|clover> <amount>",
                "&e take   &7<fiber|xp|level|driftwood|moss|reed|clover> <amount>",
                "&e reset  &7<upgrades|fiber|materials|research|elder|all>",
                "&e upgrade &7<fiber_amount|material_amount|material_chance|crop_cooldown> <level>",
                "&e elder  &7<fiber_amount|material_amount|xp_gain|material_chance> <level>",
                "&e bonus  &7<fiber_amount|material_amount|material_chance> <percent>",
                "&8&m                                          "
        ));
    }

    private void sendBossHelp(CommandSender sender) {
        sender.sendMessage(lines(
                "&8&m                                          ",
                "&#a8ff78&lGardenCore &7— &7/gca boss <action> <key>",
                "",
                "&e set     &7<key>  &8— Set boss zone from WorldEdit selection",
                "&e spawn   &7<key>  &8— Force-spawn a boss immediately",
                "&e despawn &7<key>  &8— Force-despawn the active boss",
                "&8&m                                          "
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gc.admin")) return List.of();
        if (args.length == 1)
            return filter(List.of("player", "event", "item", "afkzone", "boss", "reload"), args[0]);
        return switch (args[0].toLowerCase()) {
            case "player"  -> tabPlayer(args);
            case "event"   -> tabEvent(args);
            case "item"    -> tabItem(args);
            case "afkzone" -> args.length == 2 ? filter(List.of("set"), args[1]) : List.of();
            case "boss"    -> tabBoss(args);
            default        -> List.of();
        };
    }

    private List<String> tabBoss(String[] args) {
        if (args.length == 2)
            return filter(List.of("set", "spawn", "despawn"), args[1]);
        if (args.length == 3)
            return filter(new ArrayList<>(plugin.getBossManager().getBossKeys()), args[2]);
        return List.of();
    }

    private List<String> tabPlayer(String[] args) {
        if (args.length == 2) return onlinePlayers(args[1]);
        if (args.length == 3) return filter(
                List.of("set", "give", "take", "reset", "upgrade", "elder", "bonus"), args[2]);
        String action = args[2].toLowerCase();
        if (args.length == 4) {
            return switch (action) {
                case "set", "give", "take" -> filter(CURRENCY_TYPES, args[3]);
                case "reset"               -> filter(RESET_TYPES, args[3]);
                case "upgrade", "bonus"    -> filter(UPGRADE_TYPES, args[3]);
                case "elder"               -> filter(ELDER_TYPES, args[3]);
                default -> List.of();
            };
        }
        if (args.length == 5 && action.equals("set") && args[3].equalsIgnoreCase("pet")) {
            return filter(PET_RARITIES, args[4]);
        }
        return List.of();
    }

    private List<String> tabEvent(String[] args) {
        if (args.length == 2) return filter(List.of("start", "stop"), args[1]);
        if (args.length == 3 && args[1].equalsIgnoreCase("start"))
            return filter(plugin.getEventManager().getAvailableEventKeys(), args[2]);
        return List.of();
    }

    private List<String> tabItem(String[] args) {
        if (args.length == 2) return filter(new ArrayList<>(plugin.getItemManager().getItemKeys()), args[1]);
        if (args.length == 3) return onlinePlayers(args[2]);
        return List.of();
    }

    private void setMaterial(PlayerData d, String type, double v) {
        switch (type) {
            case "driftwood" -> d.setDriftwood(v);
            case "moss"      -> d.setMoss(v);
            case "reed"      -> d.setReed(v);
            case "clover"    -> d.setClover(v);
        }
    }

    private void addMaterial(PlayerData d, String type, double v) {
        switch (type) {
            case "driftwood" -> d.addDriftwood(v);
            case "moss"      -> d.addMoss(v);
            case "reed"      -> d.addReed(v);
            case "clover"    -> d.addClover(v);
        }
    }

    private void takeMaterial(PlayerData d, String type, double v) {
        switch (type) {
            case "driftwood" -> d.setDriftwood(Math.max(0, d.getDriftwood() - v));
            case "moss"      -> d.setMoss(Math.max(0, d.getMoss() - v));
            case "reed"      -> d.setReed(Math.max(0, d.getReed() - v));
            case "clover"    -> d.setClover(Math.max(0, d.getClover() - v));
        }
    }

    private boolean ensureData(CommandSender sender, OfflinePlayer target, String fallback) {
        if (target.getName() == null) {
            MessageUtil.send(sender, "player-not-found", Map.of("player", fallback));
            return false;
        }
        plugin.getDataManager().getPlayerData(target.getUniqueId());
        return true;
    }

    private double parseDouble(CommandSender sender, String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return Double.NaN; }
    }

    private int parseInt(CommandSender sender, String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { MessageUtil.send(sender, "invalid-number"); return Integer.MIN_VALUE; }
    }

    private com.pallux.gardencore.models.PetRarity parsePetRarity(CommandSender sender, String s) {
        try {
            return com.pallux.gardencore.models.PetRarity.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            err(sender, "Unknown pet rarity &e" + s + "&c. Valid: none, common, uncommon, rare, epic, legendary, mythic, divine");
            return null;
        }
    }

    private String nameOf(OfflinePlayer p, String fallback) {
        return p.getName() != null ? p.getName() : fallback;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }

    private List<String> onlinePlayers(String prefix) {
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(prefix.toLowerCase())) result.add(p.getName());
        }
        return result;
    }

    private void ok(CommandSender sender, String msg)  { MessageUtil.sendRaw(sender, "&7" + msg); }
    private void err(CommandSender sender, String msg) { MessageUtil.sendRaw(sender, "&c" + msg); }

    private String[] lines(String... raw) {
        String[] out = new String[raw.length];
        for (int i = 0; i < raw.length; i++) out[i] = ColorUtil.translate(raw[i]);
        return out;
    }
}
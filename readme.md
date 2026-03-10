# GardenCore — Plugin Documentation

> **Version:** 1.0 · **API:** Paper 1.21.4 · **Author:** Pallux

GardenCore is the core plugin for **Garden Simulator Tycoon**. Players break crops to earn **Fiber**, level up, unlock upgrades, research buffs, purchase elder perks, and find pets — all with a configurable multiplier stack built on top.

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Commands](#commands)
4. [Permissions](#permissions)
5. [Configuration Files](#configuration-files)
6. [Systems](#systems)
   - [Crops](#crops)
   - [Fiber & Multipliers](#fiber--multipliers)
   - [Leveling](#leveling)
   - [Upgrades](#upgrades)
   - [Research](#research)
   - [Elder Council](#elder-council)
   - [Pets](#pets)
   - [Events](#events)
   - [AFK Zone](#afk-zone)
   - [Custom Items](#custom-items)
   - [Islands Menu](#islands-menu)
   - [Alias Commands](#alias-commands)
7. [PlaceholderAPI Placeholders](#placeholderapi-placeholders)
8. [GUI Placeholders (Config-Level)](#gui-placeholders-config-level)
9. [Soft Dependencies](#soft-dependencies)
10. [Data Storage](#data-storage)

---

## Overview

The core game loop is:

1. **Break crops** → earn **Fiber** and **XP**, roll for **Materials** and **Pets**
2. **Spend Fiber** on **Upgrades**, **Research**, and **Elder Council** perks to grow multipliers
3. **Level up** to unlock higher-tier islands with better crops
4. **Find Pets** passively while farming for a flat fiber bonus

---

## Features

All toggleable features live under `features:` in `config.yml`.

| Key | Default | Description |
|-----|---------|-------------|
| `no-drop` | `true` | Prevents players from dropping items |
| `no-fall-damage` | `true` | Disables fall damage for all players |
| `no-hunger` | `true` | Freezes the hunger bar |
| `no-block-drops` | `true` | Crop blocks drop no physical items |
| `no-non-crop-break` | `true` | Players can only break registered crop blocks |
| `instant-replant` | `true` | Broken crops respawn automatically |
| `alias-commands` | `true` | Register custom `/` command aliases |
| `crop-farming` | `true` | Core farming loop (fiber & XP on crop break) |
| `material-drops` | `true` | Crops have a chance to drop crafting materials |
| `multipliers` | `true` | Multiplier system (upgrades, research, elder, events) |
| `upgrades` | `true` | `/upgrade` command and GUI |
| `events` | `true` | Scheduled and manual bonus events |

---

## Commands

### Player Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/garden` | `gc.player` | Opens the main Garden hub menu |
| `/upgrade [type]` | `gc.player` | Opens the Upgrades GUI, or upgrades directly if a type is given |
| `/research` | `gc.player` | Opens the Research GUI |
| `/elder` | `gc.player` | Opens the Elder Council perk menu |
| `/pets` | `gc.player` | Opens the Pets viewer menu |
| `/islands` | `gc.player` | Opens the Island travel menu |

**`/upgrade` types:** `fiber_amount`, `material_amount`, `material_chance`, `crop_cooldown`

---

### Admin Command — `/gca`

Permission: `gc.admin` (default: op)

#### Player Management

```
/gca player <player> set    <type> <amount>
/gca player <player> give   <type> <amount>
/gca player <player> take   <type> <amount>
/gca player <player> reset  <reset-type>
/gca player <player> upgrade <upgrade-type> <level>
/gca player <player> elder  <perk-type> <level>
/gca player <player> bonus  <upgrade-type> <percent>
```

**Currency types** for `set` / `give` / `take`:
`fiber`, `xp`, `level`, `driftwood`, `moss`, `reed`, `clover`, `pet`

When `type` is `pet`, the `<amount>` is a rarity name:
`none`, `common`, `uncommon`, `rare`, `epic`, `legendary`, `mythic`, `divine`

**Reset types:** `upgrades`, `fiber`, `materials`, `research`, `elder`, `all`

**Upgrade types:** `fiber_amount`, `material_amount`, `material_chance`, `crop_cooldown`

**Elder perk types:** `fiber_amount`, `material_amount`, `xp_gain`, `material_chance`

> `bonus` adds a permanent percentage bonus to a multiplier (separate from upgrade levels).
> `reset all` also despawns the player's pet cosmetic if they are online.

#### Event Management

```
/gca event start <key>   — Start an event by its config key
/gca event stop          — Stop the currently scheduled event
```

#### Item & Utility

```
/gca item <key> <player> [amount]   — Give a custom item to a player
/gca afkzone set                    — Set the AFK zone from your WorldEdit selection
/gca reload                         — Reload all config files
```

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `gc.admin` | op | Full access to `/gca` |
| `gc.player` | true | Access to all player commands |
| `gc.bypass.nodrop` | op | Can drop items |
| `gc.bypass.nofalldamage` | op | Takes fall damage |
| `gc.bypass.nohunger` | op | Loses hunger normally |
| `gc.bypass.noblockdrops` | op | Crop blocks drop items |
| `gc.bypass.instantreplant` | op | Crops do not auto-replant for this player |
| `gc.bypass.noncropsbreak` | op | Can break any block (not just crops) |

---

## Configuration Files

All files are inside the plugin's data folder. They are created from bundled defaults on first run.

| File | Purpose |
|------|---------|
| `config.yml` | Features toggle, prefix, event scheduler settings |
| `lang/messages.yml` | All player-facing messages and broadcast templates |
| `settings/crops.yml` | Crop definitions (material, fiber, XP, level requirement, replant delay) |
| `settings/materials.yml` | Drop chance and base amount per material |
| `settings/events.yml` | Event definitions (type, bonus value, display name) |
| `settings/items.yml` | Custom item definitions (name, material, lore, command) |
| `settings/aliascommands.yml` | Command alias mappings |
| `settings/afkzone.yml` | AFK zone settings and saved boundaries |
| `settings/pets.yml` | Pet rarity definitions, GUI layout, cosmetic skull textures |
| `settings/leveling.yml` | XP formula, level-up title, broadcast milestone |
| `guis/upgradesmenu.yml` | Upgrades GUI layout and lore templates |
| `guis/researchmenu.yml` | Research GUI layout, costs, durations, lore templates |
| `guis/eldermenu.yml` | Elder Council GUI layout, perk costs, bonus-per-level |
| `guis/gardenmenu.yml` | Main Garden hub menu layout and button commands |
| `guis/islandmenu.yml` | Island selection menu, locked/unlocked item templates |
| `data/playerdata.yml` | Persisted player data (auto-managed, do not edit manually) |

> Run `/gca reload` after editing any config file. Player data is **not** reloaded — it is loaded on join and saved on quit.

---

## Systems

### Crops

Defined in `settings/crops.yml`. Any Minecraft block material can be registered as a crop.

**Per-crop fields:**

| Field | Description |
|-------|-------------|
| `material` | Minecraft block material name |
| `display-name` | Name shown in level-requirement messages |
| `fiber` | Base fiber earned per break (before multipliers) |
| `xp` | Base XP earned per break (before multipliers) |
| `level-required` | Minimum player level to farm this crop |

**Global setting:**

| Field | Description |
|-------|-------------|
| `replant-delay` | Seconds before a broken crop block respawns (default `1.0`) |

> **Harvest cooldown** is separate from replant delay. Each player has an individual per-player cooldown governed by the **Crop Cooldown upgrade** (default 1.0 s, minimum 0.2 s). Breaks within the cooldown window are silently ignored.

**Predefined tiers:**

| Tier | Island | Level | Crops |
|------|--------|-------|-------|
| 1 | Plains | 1 | Short Grass, Fern, Bush |
| 2 | Savanna | 10 | Short Dry Grass, Dead Bush |
| 3 | Flower Field | 75 | Pink/Red/Orange/White Tulip, Cornflower |
| 4 | Nether Forest | 200 | Warped/Crimson Roots, Warped/Crimson Fungus |
| 5 | Coral Reef | 400 | Tube, Brain, Fire, Horn Coral |
| 6 | Mystic Forest | 1000 | Firefly Bush |

---

### Fiber & Multipliers

Fiber is the primary currency. The amount awarded per crop break is:

```
fiber_earned = crop.fiber × total_fiber_multiplier
```

**Total Fiber Multiplier** is the sum of all active sources:

| Source | Contribution |
|--------|-------------|
| Base | +1.0 |
| Fiber Amount Upgrade | `upgrade_level × 25` |
| Admin Bonus | `bonus_fiber_multiplier / 100` |
| Active Events | sum of all `FIBER_AMOUNT` event values / 100 |
| Research | `completed_researches × fiber-amount-per-research` |
| Elder — Fiber Amount | `elder_fiber_level × bonus-per-level` |
| Pet | flat bonus from the player's highest pet rarity |

**Total Material Amount Multiplier:**

| Source | Contribution |
|--------|-------------|
| Base | +1.0 |
| Material Amount Upgrade | `upgrade_level × 0.15` |
| Admin Bonus | `bonus_material_amount_multiplier / 100` |
| Active Events (`MATERIAL_AMOUNT`) | event value / 100 |
| Research | `completed_researches × material-amount-per-research` |
| Elder — Material Amount | `elder_material_amount_level × bonus-per-level` |

**Total Material Chance Multiplier:**

| Source | Contribution |
|--------|-------------|
| Base | +1.0 |
| Material Chance Upgrade | `upgrade_level × 0.04` |
| Admin Bonus | `bonus_material_chance_multiplier / 100` |
| Active Events (`MATERIAL_CHANCE`) | event value / 100 |
| Elder — Material Chance | `elder_material_chance_level × bonus-per-level` |

**Total XP Multiplier:**

| Source | Contribution |
|--------|-------------|
| Base | +1.0 |
| Active Events (`XP_AMOUNT`) | event value / 100 |
| Elder — XP Gain | `elder_xp_gain_level × bonus-per-level` |

---

### Leveling

Configured in `settings/leveling.yml`.

**XP Formula Modes:**

| Mode | Formula |
|------|---------|
| `linear` | `base × level` |
| `flat` | `base` (same every level) |
| `quadratic` | `base × level ^ exponent` |

Default: `linear` with `base: 100.0` — Level 1 requires 100 XP, Level 10 requires 1,000 XP, etc.

When a player levels up:
- A personal chat message is sent (`level.level-up`)
- A configurable on-screen title is shown (`level-up-title` / `level-up-subtitle`)
- Every `broadcast-milestone` levels a server-wide broadcast fires (`level.broadcast`)

Set `broadcast-milestone: 0` to disable milestone broadcasts.

---

### Upgrades

Opened with `/upgrade`. Purchased with Fiber.

**Cost formula:** `round(base × (current_level + 1) ^ exponent)`

| Upgrade | Max Level | Bonus Per Level | Base | Exponent |
|---------|-----------|----------------|------|---------|
| Fiber Amount | 200 | +25x fiber multiplier | 50 | 1.9 |
| Material Amount | 100 | +0.15x material amount multiplier | 150 | 1.9 |
| Material Chance | 50 | +0.04x material chance multiplier | 500 | 2.2 |
| Crop Cooldown | 8 | −0.1 s cooldown (min 0.2 s) | 200 | 1.9 |

**GUI lore placeholders** (`guis/upgradesmenu.yml`):

| Placeholder | Value |
|-------------|-------|
| `{level}` | Current upgrade level |
| `{max_level}` | Maximum upgrade level |
| `{cost}` | Fiber cost for next level |
| `{value}` | Current effective value (multiplier or cooldown in seconds) |

---

### Research

Opened with `/research`. A **sequential** unlock system — players complete one research at a time in order.

**Cost formula:** `round(base-cost × cost-growth ^ index)` (index is 0-based)

Default (`base-cost: 500`, `cost-growth: 1.45`):

| Research | Cost |
|----------|------|
| I | 500 |
| V | ~2,210 |
| X | ~14,167 |
| XXVIII | ~11.4M |

**Duration formula:** `base-duration-minutes × research_number` (1-based)

Default (`base-duration-minutes: 10`):

| Research | Duration |
|----------|---------|
| I | 10 min |
| X | 100 min |
| XXVIII | ~4h 40m |

**Per completed research bonus** (cumulative, stacks across all completions):
- `fiber-amount-per-research: 500.0` → added directly to the fiber multiplier
- `material-amount-per-research: 1.0` → added directly to the material amount multiplier

**GUI lore placeholders** (`guis/researchmenu.yml`):

| Placeholder | Value |
|-------------|-------|
| `{fiber_multi}` | Cumulative fiber bonus after this research completes |
| `{material_multi}` | Cumulative material bonus after this research completes |
| `{cost}` | Fiber cost |
| `{time}` | Total duration (formatted) |
| `{time_remaining}` | Time remaining on an in-progress research |

Cancelling research removes the active research but **does not refund** the fiber cost.

---

### Elder Council

Opened with `/elder`. Permanent endgame upgrades that cost **Fiber plus crafting materials**.

**Cost formula per perk per level:** `round(material-base × level ^ material-exponent)`
A `base` of `0` means that material is **not required** for that perk.

**Default Perks:**

| Perk | Max Level | Bonus/Level | Primary Cost |
|------|-----------|------------|--------------|
| Fiber Amount | 200 | +100x fiber multiplier | Fiber + Driftwood |
| Material Amount | 100 | +10x material amount multiplier | Fiber + Driftwood + Moss + Reed |
| XP Gain | 50 | +5x XP multiplier | Fiber + Driftwood + Moss + Reed + Clover |
| Material Chance | 25 | +0.1x material chance multiplier | Fiber + all materials (expensive) |

**GUI lore placeholders** (`guis/eldermenu.yml`):

| Placeholder | Value |
|-------------|-------|
| `{level}` | Current perk level |
| `{max_level}` | Maximum perk level |
| `{bonus}` | Total bonus at current level |
| `{next_bonus}` | Bonus from the next level |
| `{fiber_cost}` | Fiber cost for next level |
| `{driftwood_cost}` | Driftwood cost (0 if not required) |
| `{moss_cost}` | Moss cost (0 if not required) |
| `{reed_cost}` | Reed cost (0 if not required) |
| `{clover_cost}` | Clover cost (0 if not required) |
| `{has_fiber}` | ✔ or ✘ depending on affordability |
| `{has_driftwood}` | ✔ or ✘ |
| `{has_moss}` | ✔ or ✘ |
| `{has_reed}` | ✔ or ✘ |
| `{has_clover}` | ✔ or ✘ |
| `{fiber}` | Player's fiber balance (info item) |
| `{driftwood}` | Player's driftwood balance (info item) |
| `{moss}` | Player's moss balance (info item) |
| `{reed}` | Player's reed balance (info item) |
| `{clover}` | Player's clover balance (info item) |

---

### Pets

Pets are found passively by breaking crops and grant a **flat fiber multiplier bonus**.

- Players can only find pets of a **higher rarity** than what they currently own.
- Only the player's **best (highest) pet** is active at any time.
- An **ArmorStand cosmetic** orbits the player while a pet is equipped (configurable skull texture per rarity via `settings/pets.yml`).

**Default rarities:**

| Rarity | 1-in-N Chance | Fiber Bonus |
|--------|--------------|-------------|
| Common | 1 in 1,000 | +100x |
| Uncommon | 1 in 10,000 | +200x |
| Rare | 1 in 50,000 | +300x |
| Epic | 1 in 150,000 | +500x |
| Legendary | 1 in 250,000 | +1,000x |
| Mythic | 1 in 750,000 | +2,500x |
| Divine | 1 in 10,000,000 | +7,500x |

**Cosmetic configuration** (`settings/pets.yml` → `pets.rarities.<key>.cosmetic`):

| Field | Description |
|-------|-------------|
| `material` | Any Minecraft material. Use `PLAYER_HEAD` for a custom skull. |
| `skull-texture` | Base64 skin value (only used when `material: PLAYER_HEAD`). Grab from [minecraft-heads.com](https://minecraft-heads.com) → **Value** field. |

**GUI lore placeholders** (`settings/pets.yml`):

| Placeholder | Context | Value |
|-------------|---------|-------|
| `{rarity}` | `pet-lore`, `overview rarity-line` | Colored + bolded rarity name |
| `{fiber_bonus}` | `pet-lore`, `overview rarity-line` | Flat fiber bonus value |
| `{chance}` | `overview rarity-line`, `broadcast-message` | 1-in-N number |
| `{player}` | `broadcast-message` | Player name (colored) |

**`overview-lore` rarity lines** are defined with a special prefix syntax:
```yaml
- "rarity-line: {rarity} &8| &71/{chance} &8| &#FFD700+{fiber_bonus}x Fiber"
```
One line is injected per enabled rarity automatically.

---

### Events

Events apply a **temporary server-wide multiplier bonus** to all online players, shown on a boss bar.

**Event types:**

| Type | Affected Multiplier |
|------|-------------------|
| `FIBER_AMOUNT` | Fiber earned per crop break |
| `MATERIAL_AMOUNT` | Material drop quantity |
| `MATERIAL_CHANCE` | Material drop chance |
| `XP_AMOUNT` | XP earned per crop break |

**Scheduler settings** (`config.yml`):

| Key | Default | Description |
|-----|---------|-------------|
| `events.interval-minutes` | 30 | How often a random event fires automatically |
| `events.duration-minutes` | 5 | How long each event lasts |

Multiple events can run simultaneously (e.g. one scheduled + one triggered by an item ticket).

**Starting events manually:**
- `/gca event start <key>` — starts a named event as the "scheduled" slot
- Using an **Event Ticket item** — starts a separate, uniquely-keyed event that runs in parallel

**Boss bar message placeholders** (`lang/messages.yml`):

| Placeholder | Value |
|-------------|-------|
| `{event}` | Event display name |
| `{time}` | Seconds remaining |

---

### AFK Zone

A configurable 3D region where players receive a repeating reward.

**Setup:**
1. Make a WorldEdit (or FAWE) cuboid selection.
2. Run `/gca afkzone set`.

**Settings** (`settings/afkzone.yml`):

| Key | Description |
|-----|-------------|
| `enabled` | Enable/disable the AFK zone |
| `reward-interval` | Seconds between reward payouts |
| `reward-command` | Console command to execute. Use `%player%` for the player's name. |
| `title` / `subtitle` | On-screen title shown while inside. `{time}` = seconds until next reward. |
| `actionbar-enter` / `actionbar-leave` | Action bar flash on zone enter/leave |
| `zone.world` / `zone.min-*` / `zone.max-*` | Zone boundaries (set automatically by `/gca afkzone set`) |

---

### Custom Items

Defined in `settings/items.yml`. Given with `/gca item <key> <player> [amount]`.

Right-clicking a custom item executes its configured command (as console). Items are **single-use** — they are consumed on activation.

**Per-item fields:**

| Field | Description |
|-------|-------------|
| `name` | Display name (color codes supported) |
| `material` | Minecraft material |
| `lore` | List of lore lines |
| `command` | Command to execute. Use `%player%` for the player's name. Special: `gca event start <key>` starts an event directly without going through the console dispatcher. |

Items are identified via a **PersistentDataContainer** tag (`gc_item_key`) so renamed copies still work correctly.

---

### Islands Menu

Opened with `/islands`. Buttons teleport players to configured warps, gated behind minimum player levels.

**Per-island fields** (`guis/islandmenu.yml`):

| Field | Description |
|-------|-------------|
| `slot` | Inventory slot |
| `material` | Item material when unlocked |
| `name` | Display name when unlocked |
| `lore` | Lore when unlocked. Placeholder: `{level_required}` |
| `command` | Command run as the player on click (no leading `/`) |
| `level-required` | Minimum player level |

**Root-level locked appearance** applies to all locked buttons:

| Field | Description |
|-------|-------------|
| `locked-material` | Item when locked |
| `locked-name` | Name template. `{name}` = island's display name |
| `locked-lore` | Lore when locked. Placeholder: `{level_required}` |

---

### Alias Commands

Defined in `settings/aliascommands.yml`. Registers short `/command` aliases that dispatch another command as the player.

```yaml
aliases:
  afk:
    command: "warp afk"
```

Typing `/afk` will run `/warp afk` as the player. Arguments pass through (e.g. `/afk extra` runs `/warp afk extra`).

> Aliases are registered at startup. A server restart (or `/gca reload` is **not** sufficient for new alias registration — aliases must be registered at startup.

---

## PlaceholderAPI Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to be installed. All placeholders use the `%gc_<identifier>%` format.

| Placeholder | Description |
|-------------|-------------|
| `%gc_fiber_raw%` | Raw fiber balance (formatted with K/M/B suffixes) |
| `%gc_fiber_formatted%` | Same as `fiber_raw` |
| `%gc_level%` | Current player level |
| `%gc_xp%` | XP progress as a percentage (e.g. `42.5%`) |
| `%gc_driftwood_raw%` | Driftwood balance |
| `%gc_driftwood_formatted%` | Same as `driftwood_raw` |
| `%gc_moss_raw%` | Moss balance |
| `%gc_moss_formatted%` | Same as `moss_raw` |
| `%gc_reed_raw%` | Reed balance |
| `%gc_reed_formatted%` | Same as `reed_raw` |
| `%gc_clover_raw%` | Clover balance |
| `%gc_clover_formatted%` | Same as `clover_raw` |
| `%gc_multi_fiberamount%` | Total fiber multiplier (e.g. `x26`) |
| `%gc_multi_materialamount%` | Total material amount multiplier |
| `%gc_multi_materialchance%` | Total material chance multiplier |

---

## GUI Placeholders (Config-Level)

These placeholders are resolved inside YAML config files and are **not** PlaceholderAPI — they are processed internally per GUI.

### Garden Menu (`guis/gardenmenu.yml`) — Player Head lore

| Placeholder | Value |
|-------------|-------|
| `{player}` | Player name |
| `{level}` | Player level |
| `{xp}` | XP progress as a percentage |
| `{fiber}` | Formatted fiber balance |
| `{multi_fiber}` | Total fiber multiplier string |
| `{multi_material}` | Total material amount multiplier string |
| `{multi_material_chance}` | Total material chance multiplier string |
| `{upgrade_fiber}` | Fiber Amount upgrade level |
| `{upgrade_material}` | Material Amount upgrade level |
| `{upgrade_material_chance}` | Material Chance upgrade level |
| `{upgrade_cooldown}` | Crop Cooldown upgrade level |
| `{research_completed}` | Number of completed researches |
| `{research_active}` | Active research name + time remaining, or `None` |
| `{driftwood}` | Driftwood balance |
| `{moss}` | Moss balance |
| `{reed}` | Reed balance |
| `{clover}` | Clover balance |

### Upgrades GUI (`guis/upgradesmenu.yml`) — Item lore

| Placeholder | Value |
|-------------|-------|
| `{level}` | Current upgrade level |
| `{max_level}` | Maximum upgrade level |
| `{cost}` | Fiber cost for next level |
| `{value}` | Current effective value (multiplier or cooldown) |

### Research GUI (`guis/researchmenu.yml`) — Item lore

| Placeholder | Value |
|-------------|-------|
| `{fiber_multi}` | Cumulative fiber bonus after this research |
| `{material_multi}` | Cumulative material bonus after this research |
| `{cost}` | Fiber cost |
| `{time}` | Total research duration |
| `{time_remaining}` | Time remaining (in-progress only) |

### Elder Council GUI (`guis/eldermenu.yml`) — Perk item lore

| Placeholder | Value |
|-------------|-------|
| `{level}` | Current perk level |
| `{max_level}` | Maximum perk level |
| `{bonus}` | Total bonus at current level |
| `{next_bonus}` | Bonus from the next level |
| `{fiber_cost}` | Fiber cost for next level (`-` if maxed) |
| `{driftwood_cost}` | Driftwood cost (`-` if maxed, `0` if not required) |
| `{moss_cost}` | Moss cost |
| `{reed_cost}` | Reed cost |
| `{clover_cost}` | Clover cost |
| `{has_fiber}` | ✔ green / ✘ red |
| `{has_driftwood}` | ✔ green / ✘ red |
| `{has_moss}` | ✔ green / ✘ red |
| `{has_reed}` | ✔ green / ✘ red |
| `{has_clover}` | ✔ green / ✘ red |

### Elder Council GUI — Info item lore

| Placeholder | Value |
|-------------|-------|
| `{fiber}` | Player's fiber balance |
| `{driftwood}` | Player's driftwood balance |
| `{moss}` | Player's moss balance |
| `{reed}` | Player's reed balance |
| `{clover}` | Player's clover balance |

### Island Menu (`guis/islandmenu.yml`) — Lore

| Placeholder | Context | Value |
|-------------|---------|-------|
| `{level_required}` | Per-island lore and locked lore | Minimum level to use this island |
| `{name}` | `locked-name` | The island's display name (stripped of color) |

### AFK Zone (`settings/afkzone.yml`)

| Placeholder | Context | Value |
|-------------|---------|-------|
| `{time}` | `subtitle` | Seconds until next reward |
| `%player%` | `reward-command` | Player's name |

### Messages (`lang/messages.yml`)

Common message placeholders passed by the plugin:

| Message Key | Placeholders |
|-------------|-------------|
| `fiber.set` / `fiber.give` / `fiber.take` | `{player}`, `{amount}` |
| `fiber.harvest-title` / `fiber.harvest-subtitle` | `{amount}` |
| `material.found-actionbar` | `{amount}`, `{material}` |
| `upgrades.set` | `{upgrade}`, `{player}`, `{level}` |
| `upgrades.reset-*` | `{player}` |
| `level.level-up` | `{level}` |
| `level.broadcast` | `{player}`, `{level}` |
| `events.start` / `events.end` | `{event}` |
| `events.bossbar-active` | `{event}`, `{time}` |
| `crop.level-required` | `{level}`, `{crop}` |
| `research.started` | `{time}` |
| `research.completed` | `{fiber_multi}`, `{material_multi}` |
| `research.not-enough-fiber` | `{cost}`, `{balance}` |
| `islands.level-required` | `{level}` |
| `elder.purchased` | `{perk}`, `{level}`, `{bonus}` |
| `elder.max-reached` | `{perk}` |
| `player-not-found` | `{player}` |
| `invalid-arguments` | `{usage}` |
| `admin.reset-material` | `{player}` |
| `admin.reset-research` | `{player}` |
| `admin.reset-elder` | `{player}` |

---

## Soft Dependencies

| Plugin | Required? | Used For |
|--------|-----------|---------|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional | Exposes `%gc_*%` placeholders for scoreboards, chat plugins, etc. |
| [WorldEdit](https://enginehub.org/worldedit) / [FAWE](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) | Optional | `/gca afkzone set` reads your cuboid selection |

If neither WorldEdit nor FAWE is installed, `/gca afkzone set` will report an error. You can still define zone boundaries manually in `settings/afkzone.yml`.

---

## Data Storage

All player data is stored in `data/playerdata.yml` using UUID keys. The following values are persisted per player:

| Field | Description |
|-------|-------------|
| `fiber` | Fiber balance |
| `xp` | Current XP toward next level |
| `level` | Current level |
| `upgrades.*` | Upgrade levels for all four upgrade types |
| `bonus.*` | Admin-granted permanent bonus multipliers |
| `materials.*` | Driftwood, moss, reed, clover balances |
| `research.completed` | Number of completed researches |
| `research.active-index` | Index of the active research (−1 if none) |
| `research.active-start` | Unix timestamp (ms) when the active research started |
| `elder.*` | Elder perk levels for all four perk types |
| `pet.rarity` | The player's highest pet rarity |

Data is **loaded on join** and **saved on quit** (synchronously to prevent race conditions). Async saves are triggered throughout gameplay for intermediate persistence. A periodic auto-save does not run; the safest way to persist mid-session changes is the quit/join cycle or using `/gca reload` (which does not affect player data).

---

*Documentation generated for GardenCore v1.0 by Pallux.*

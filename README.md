<div align="center">

# 🌿 GardenCore

**The complete core plugin for Garden Simulator Tycoon servers.**

Crop farming · Upgrades · Research · Elder Council · Pets · Bosses · Events · Composters · AFK Zones

[![Paper](https://img.shields.io/badge/Paper-1.21.4-00aa00?style=flat-square)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://adoptium.net)

</div>

---

## 📖 Overview

GardenCore is a fully-featured progression plugin built around **crop farming**. Players break crops to earn **Fiber** — the core currency — which they spend on **Upgrades**, **Research**, and **Elder Perks** to grow their multipliers over time. Along the way they collect **crafting materials**, discover **Pets**, fight **Boss encounters**, and benefit from server-wide **Events** and placeable **Composters**.

Every system is driven by YAML config files, so you can tune values, costs, messages, and GUIs to fit your server without touching any code.

---

## ⚙️ Requirements

| Requirement | Version |
|---|---|
| [Paper](https://papermc.io/downloads) | 1.21.4 |
| Java | 21 |

### Optional (but recommended)

| Plugin | Purpose |
|---|---|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Enables `%gc_*%` placeholders for scoreboards, holograms, etc. |
| [WorldEdit](https://enginehub.org/worldedit) / [FAWE](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) | Required to define AFK Zones and Boss Zones in-game |

---

## 🚀 Installation

1. Drop `GardenCore.jar` into your `plugins/` folder.
2. Start the server — all config files will be generated automatically.
3. Stop the server and configure the files in `plugins/GardenCore/` to your liking.
4. Start the server again. That's it!

---

## 🎮 Player Commands

| Command | Description |
|---|---|
| `/garden` | Open the main Garden Menu (stats, quick navigation) |
| `/upgrade` | Open the Upgrades GUI |
| `/research` | Open the Research GUI |
| `/elder` | Open the Elder Council GUI |
| `/pets` | Open the Pets GUI |
| `/islands` | Open the Island travel menu |

---

## 🔧 Admin Commands (`/gca`)

> Requires the `gc.admin` permission.

### Player Management

| Command | Description |
|---|---|
| `/gca player <player> set <type> <amount>` | Set a currency value |
| `/gca player <player> give <type> <amount>` | Add to a currency value |
| `/gca player <player> take <type> <amount>` | Remove from a currency value |
| `/gca player <player> reset <type>` | Reset a category of data |
| `/gca player <player> upgrade <type> <level>` | Force-set an upgrade level |
| `/gca player <player> elder <type> <level>` | Force-set an elder perk level |
| `/gca player <player> bonus <type> <percent>` | Add a permanent bonus multiplier |

**`<type>` for set / give / take:** `fiber` `xp` `level` `driftwood` `moss` `reed` `clover`

**`<type>` for upgrades / bonus:** `fiber_amount` `material_amount` `material_chance` `crop_cooldown`

**`<type>` for elder:** `fiber_amount` `material_amount` `xp_gain` `material_chance`

**`<type>` for reset:** `upgrades` `fiber` `materials` `research` `elder` `all`

---

### Events

| Command | Description |
|---|---|
| `/gca event start <key>` | Force-start an event by its key from `events.yml` |
| `/gca event stop` | Stop the currently running scheduled event |

### Bosses

| Command | Description |
|---|---|
| `/gca boss set <key>` | Save the boss zone from your WorldEdit selection |
| `/gca boss spawn <key>` | Force-spawn a boss immediately |
| `/gca boss despawn <key>` | Force-despawn the currently active boss |

### Items & Utility

| Command | Description |
|---|---|
| `/gca item <key> <player> [amount]` | Give a custom item to a player |
| `/gca afkzone set` | Save the AFK zone from your WorldEdit selection |
| `/gca reload` | Reload all config files |

---

## 🔑 Permissions

### Core

| Permission | Default | Description |
|---|---|---|
| `gc.admin` | OP | Full access to all `/gca` commands |
| `gc.player` | Everyone | Access to all player commands and GUIs |

### Bypass Permissions

| Permission | Default | Description |
|---|---|---|
| `gc.bypass.nodrop` | OP | Bypass item-drop prevention |
| `gc.bypass.nofalldamage` | OP | Bypass fall damage prevention |
| `gc.bypass.nohunger` | OP | Bypass hunger prevention |
| `gc.bypass.noblockdrops` | OP | Bypass crop block drop prevention |
| `gc.bypass.instantreplant` | OP | Bypass instant crop replanting |
| `gc.bypass.noncropsbreak` | OP | Allow breaking non-crop blocks |

### Donor Multiplier Permissions

Grant these to donor ranks to give bonus multipliers on top of all in-game progression. Nodes stack additively — a player can hold multiple.

**Format:** `gc.multi.<type>.donor.<value>`

```
gc.multi.fiber_amount.donor.50     → +50% Fiber Amount multiplier
gc.multi.material_amount.donor.25  → +25% Material Amount multiplier
gc.multi.xp.donor.100             → +100% XP multiplier
gc.multi.material_chance.donor.10  → +10% Material Chance multiplier
```

> **Example:** A VIP rank holding both `gc.multi.fiber_amount.donor.25` and `gc.multi.fiber_amount.donor.50` receives a combined **+75% Fiber Amount bonus** on top of all other multipliers.

---

## 📊 PlaceholderAPI Placeholders

> Requires PlaceholderAPI to be installed.

| Placeholder | Description |
|---|---|
| `%gc_fiber_raw%` | Fiber balance (e.g. `1.5K`) |
| `%gc_fiber_formatted%` | Fiber balance (formatted) |
| `%gc_level%` | Player level |
| `%gc_xp%` | XP progress as a percentage (e.g. `42.5%`) |
| `%gc_driftwood_raw%` | Driftwood balance |
| `%gc_moss_raw%` | Moss balance |
| `%gc_reed_raw%` | Reed balance |
| `%gc_clover_raw%` | Clover balance |
| `%gc_multi_fiberamount%` | Total Fiber Amount multiplier (e.g. `x26.5K`) |
| `%gc_multi_materialamount%` | Total Material Amount multiplier |
| `%gc_multi_materialchance%` | Total Material Chance multiplier |

---

## 🌾 Crop Farming

Players break **crop blocks** to earn **Fiber** and **XP**. Every crop has a minimum player level required to farm it — attempting to break a locked crop cancels the break and notifies the player.

Crops instantly replant after a short configurable delay, and each player has a **per-harvest cooldown** that can be reduced through the Crop Cooldown upgrade.

All crops, their fiber values, XP values, and level requirements are fully configurable in `settings/crops.yml`.

### Default Crop Tiers

| Tier | Island | Min. Level | Crops |
|---|---|---|---|
| 1 | Plains | 1 | Short Grass, Fern, Bush |
| 2 | Savanna | 10 | Short Dry Grass, Dead Bush |
| 3 | Flower Field | 75 | Pink, Red, Orange & White Tulip, Cornflower |
| 4 | Nether Forest | 200 | Warped & Crimson Roots, Warped & Crimson Fungus |
| 5 | Coral Reef | 400 | Tube, Brain, Fire & Horn Coral |
| 6 | Mystic Forest | 1000 | Firefly Bush |

---

## ⬆️ Upgrades

Players spend **Fiber** to permanently upgrade their multipliers via `/upgrade`.

| Upgrade | Effect per Level | Max Levels |
|---|---|---|
| **Fiber Amount** | +25x to fiber multiplier | 200 |
| **Material Amount** | +0.15x to material drop amount | 100 |
| **Material Chance** | +0.04x to material drop chance | 50 |
| **Crop Cooldown** | −0.1s harvest cooldown (min 0.2s) | 8 |

Costs scale exponentially. All costs are displayed in the GUI before purchase and are fully configurable in `guis/upgradesmenu.yml`.

---

## 🔬 Research

Research unlocks **permanent cumulative bonuses** to fiber and material multipliers. Only one research can be active at a time and they must be completed in order.

- Start a research from the GUI (`/research`) — it costs Fiber and takes real time to complete.
- Cancelling a research **loses all progress and the fiber cost**.
- Every completed research adds a flat bonus to all fiber and material multipliers.

Default settings: **28 total researches**, each adding **+500x Fiber** and **+1x Material Amount** to multipliers. All values are configurable in `guis/researchmenu.yml`.

---

## 🏛️ Elder Council

The Elder Council offers **powerful endgame perks** purchased with both **Fiber and crafting materials**. Open it with `/elder`.

| Perk | Effect per Level | Max Levels |
|---|---|---|
| **Fiber Amount** | +100x fiber multiplier | 200 |
| **Material Amount** | +10x material amount multiplier | 100 |
| **XP Gain** | +5x XP multiplier | 50 |
| **Material Chance** | +0.1x material chance multiplier | 25 |

All costs are configurable per perk per level in `guis/eldermenu.yml`.

---

## 🪨 Crafting Materials

Materials drop randomly when a player breaks a crop. They are used exclusively to purchase Elder Council perks.

| Material | Default Drop Chance |
|---|---|
| Driftwood | 5% |
| Moss | 2.5% |
| Reed | 1% |
| Clover | 0.1% |

Drop chances scale with the player's **Material Chance multiplier**. Drop amounts scale with the **Material Amount multiplier**. Configure base values in `settings/materials.yml`.

---

## 🐾 Pets

Pets are discovered **randomly while breaking crops** and provide a permanent **Fiber Amount bonus**. Players always keep their highest-rarity pet — finding a higher rarity automatically upgrades it. A cosmetic ArmorStand orbits the player to show off their pet.

### Default Rarities

| Rarity | Chance | Fiber Bonus |
|---|---|---|
| Common | 1 in 1,000 | +100x |
| Uncommon | 1 in 10,000 | +200x |
| Rare | 1 in 50,000 | +300x |
| Epic | 1 in 150,000 | +500x |
| Legendary | 1 in 250,000 | +1,000x |
| Mythic | 1 in 750,000 | +2,500x |
| Divine | 1 in 10,000,000 | +7,500x |

All rarities, chances, bonuses, and cosmetic skull textures are configurable in `settings/pets.yml`. Pets can be granted manually via `/gca player <player> set pet <rarity>`.

---

## 👾 Boss Encounters

Bosses are giant entities that spawn inside a defined WorldEdit zone. Players defeat them by attacking — each hit deals damage equal to the player's **total Fiber multiplier**.

- One random boss spawns automatically on a configurable timer (default: every 30 min).
- A **server-wide boss bar** shows HP and a countdown.
- If not killed in time, the boss **escapes** with no rewards.
- All players who dealt damage receive a **configurable reward command** on defeat.

### Default Bosses

| Boss | Island | Max HP | Time Limit |
|---|---|---|---|
| Meadow Shambler | Plains | 500K | 2 min |
| Dust Stalker | Savanna | 5M | 2 min |
| Bloom Wraith | Flower Field | 10M | 2 min |
| Infernal Reaper | Nether Forest | 100M | 2 min |
| Tide Colossus | Coral Reef | 750M | 2 min |
| Eternal Arbiter | Mystic Forest | 1.75B | 2 min |

### Boss Zone Setup

1. Make a WorldEdit/FAWE cuboid selection inside the island.
2. Run `/gca boss set <key>`.
3. Done — the boss will roam this zone automatically.

All boss stats, skull textures, and reward commands are configurable in `settings/bosses.yml`.

---

## ⚡ Events

Events grant **temporary server-wide multiplier bonuses** shown via a boss bar with a countdown. Events start automatically on a timer or can be triggered manually by admins or players using **Event Ticket** items.

### Default Events

| Event Key | Bonus Type | Bonus |
|---|---|---|
| `fiber_50` | Fiber Amount | +50% |
| `fiber_100` | Fiber Amount | +100% |
| `fiber_250` | Fiber Amount | +250% |
| `fiber_500` | Fiber Amount | +500% |
| `material_amount_20` | Material Amount | +20% |
| `material_amount_75` | Material Amount | +75% |
| `material_amount_150` | Material Amount | +150% |
| `material_chance_10` | Material Chance | +10% |
| `material_chance_25` | Material Chance | +25% |
| `xp_50` | XP | +50% |
| `xp_100` | XP | +100% |
| `xp_250` | XP | +250% |

Configure event names, types, and values in `settings/events.yml`. Timer settings are in `config.yml`.

---

## 🧪 Lucky Composters

Lucky Composters are **placeable, area-of-effect buff blocks** delivered to players via `/gca item <key> <player>`. When placed, they display a floating hologram and apply a buff to all nearby players.

- **Single-use** — placing the block activates it; breaking it cancels the buff.
- **Persist through restarts** — an active composter resumes its countdown after reboot.

### Default Composter Types

| Item Key | Buff | Duration | Radius |
|---|---|---|---|
| `composter_fiber_100` | +100% Fiber Amount | 15 min | 20 blocks |
| `composter_fiber_250` | +250% Fiber Amount | 15 min | 20 blocks |
| `composter_xp_50` | +50% XP | 15 min | 20 blocks |
| `composter_xp_75` | +75% XP | 15 min | 20 blocks |
| `composter_material_100` | +100% Material Amount | 15 min | 20 blocks |
| `composter_material_150` | +150% Material Amount | 15 min | 20 blocks |

---

## 💤 AFK Zone

Define a region where players earn rewards simply by standing still. A title and countdown is shown while inside, and an action bar message notifies them on entry or exit.

**Setup:**
1. Make a WorldEdit/FAWE selection over the desired area.
2. Run `/gca afkzone set`.

Configure the reward command, interval, and display text in `settings/afkzone.yml`.

---

## 📈 Leveling

Players gain XP by breaking crops and level up automatically. The XP required per level uses a configurable formula.

| Formula Mode | Behaviour |
|---|---|
| `linear` | XP required = `base × level` |
| `flat` | Same XP cost every level |
| `quadratic` | XP required = `base × level^exponent` |

- A configurable **level-up title** is shown on screen when leveling up.
- A **server-wide broadcast** fires every N levels (configurable milestone).

---

## 🎫 Custom Items

Custom items are defined in `settings/items.yml` and given out via `/gca item <key> <player> [amount]`.

| Type | Example Keys | How It Works |
|---|---|---|
| **Event Tickets** | `ticket_fiber_100`, `ticket_xp_50` | Right-click to instantly start the linked event for all players (single-use) |
| **Lucky Composters** | `composter_fiber_100`, `composter_xp_75` | Place the block to activate the area buff (single-use) |

You can define additional custom items with any right-click console command in the config.

---

## ⚙️ Configuration Files

| File | What it controls |
|---|---|
| `config.yml` | Plugin prefix, feature toggles, event timer settings |
| `lang/messages.yml` | Every player-facing message and title |
| `settings/crops.yml` | Crop materials, fiber/XP values, level requirements, replant delay |
| `settings/materials.yml` | Material names, drop chances, base amounts |
| `settings/events.yml` | Event keys, display names, types, and bonus values |
| `settings/items.yml` | Custom item definitions (composters, tickets, hologram lines) |
| `settings/pets.yml` | Pet rarities — chances, bonuses, colors, cosmetic skull textures |
| `settings/leveling.yml` | XP formula, level-up title, broadcast milestone |
| `settings/bosses.yml` | Boss stats, skull textures, reward commands, spawn timer, zone data |
| `settings/afkzone.yml` | AFK zone bounds, reward command, interval, display text |
| `settings/aliascommands.yml` | Custom slash-command aliases |
| `settings/blocked-commands.yml` | Commands blocked from non-admin players |
| `guis/upgradesmenu.yml` | Upgrades GUI layout and item lore |
| `guis/researchmenu.yml` | Research GUI, costs, durations, bonuses per research |
| `guis/eldermenu.yml` | Elder Council GUI, perk costs, max levels, bonuses per level |
| `guis/gardenmenu.yml` | Main Garden Menu layout and button commands |
| `guis/islandmenu.yml` | Island menu layout and level requirements |

### Feature Toggles

Every major feature can be independently disabled in `config.yml` under the `features:` block, letting you run only the systems your server needs.

---

## 💾 Player Data

All player data is stored as individual YAML files in `plugins/GardenCore/userdata/`. No database setup required.

- Auto-saved every **5 minutes** while online.
- Saved on **disconnect** and **server shutdown**.
- Loaded **asynchronously on join** to prevent lag spikes.

---

*Made by **Pallux** — [BuiltByBit Profile](https://builtbybit.com/members/pallux.34066/)*

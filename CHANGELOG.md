## 1.11.2

- Clarify that FPS Booster's MAX FPS profile optimizes entity distance, particles and entity shadows without setting an artificial 240 FPS ceiling.
- Disable VSync while FPS Booster is enabled and restore the previous VSync preference when disabled, removing a possible display-refresh ceiling.
- Show Minecraft's current FPS limit and VSync state in the FPS Booster interface.
- The goal is higher real-world FPS in singleplayer and multiplayer; results depend on hardware and rendering workload.

## 1.11.1

### FPS and HUD allocation improvements
- Removed Formatter/varargs overhead from Frame Stats, Offhand Counter, XP Progress, World Time, Mount Health, Flight Status, and Last Death HUD telemetry updates.
- Skip scheduled inventory, equipment, and potion HUD refresh dispatch when their modules are disabled.
- No render-distance, simulation-distance, gameplay, or graphics-quality changes.
- These are targeted CPU/allocation reductions; measured FPS gains have not been established.

### Versioning
- MazClient advances from **1.11.0** to **1.11.1**.
- MazLauncher remains **0.6.69**.

## 1.11.0

### True live ping measurement
- Replaced the Ping HUD's primary latency source with an asynchronous client-side TCP round-trip probe to the exact active server endpoint every ~750 ms.
- The probe runs entirely off the render/client thread on a low-priority daemon worker, so slow or failed probes cannot stall gameplay or reduce FPS.
- Vanilla `PlayerInfo` latency remains as fallback until the first live RTT result is available or when a live probe cannot be measured.
- Existing ping quality, median, jitter, range and stability diagnostics remain available, now driven by the fresher RTT samples.
- No module toggle or manual setup was added; the behavior is automatic whenever the Ping HUD is enabled.

### Versioning
- MazClient advances from **1.10.19** to **1.11.0** under the existing patch-rollover rule.
- MazLauncher remains **0.6.54**.

## 1.10.19

### Real-time ping HUD
- Fixed the Ping HUD feeling frozen or delayed by moving latency reads out of the staggered telemetry phase and checking Minecraft's latest server-reported latency every client tick.
- The main Ping value now updates immediately whenever Minecraft exposes a new latency value instead of waiting for the 250 ms statistics sample window.
- Median, jitter, range and stability remain sampled diagnostics and are shown as secondary information.
- No extra network packets are sent; MazClient only reads Minecraft's existing PlayerInfo latency value more responsively.

### Versioning
- MazClient advances from **1.10.18** to **1.10.19**.
- MazLauncher remains **0.6.53**.

## 1.10.18

### Core render hot-path optimization
- Collapsed MazClient's specialized HUD rendering from many separate Fabric HUD registry callbacks into one dispatcher, reducing per-frame callback/registry overhead while preserving every HUD and module toggle.
- Cached the Hide Scoreboard module lookup inside the HUD mixin so scoreboard rendering no longer performs a module-manager name lookup each time Minecraft tries to draw the sidebar.
- Removed Java Formatter/String.format usage from MazHUD's frequently refreshed telemetry strings for reach, target health, speed, direction, compass, session time and potion durations; lightweight fixed-decimal/time helpers now avoid Formatter churn and temporary allocation pressure.
- These optimizations are always active. No new toggle or settings screen was added.
- Gameplay, render distance, simulation distance, module behavior and HUD layouts remain unchanged.

### Versioning
- MazClient advances from **1.10.17** to **1.10.18**.
- MazLauncher remains **0.6.53**.

## 1.10.17

### More aggressive FPS Booster
- Increased the default FPS Booster particle thinning from keeping roughly 1 in 8 particles to roughly 1 in 16.
- Expanded particle thinning up to 1 in 32 particles for an aggressive low-overhead rendering profile.
- Added a one-click **MAX FPS** control to the FPS Booster screen. It enables FPS Booster, uses the lowest safe entity-distance scaling already supported by Minecraft, disables entity shadows, and applies the strongest particle thinning profile.
- Render distance and simulation distance remain completely player-controlled and are never changed by FPS Booster.
- Existing modules, HUDs, configs, launcher behavior and gameplay features remain intact.

### Versioning
- MazClient advances from **1.10.16** to **1.10.17**.
- MazLauncher remains **0.6.51**.

## 1.10.16

### Maintenance cleanup
- Centralized all archived client and launcher changelogs under the `archive/` folder and removed the remaining duplicate archive file from the repository root.
- Updated the active client and launcher changelogs so their historical-reference paths point at the new archive location.
- Removed empty no-op Gradle repository placeholder blocks that had no effect on dependency resolution or publishing behavior.
- Audited the Java source tree for unused imports, dead private fields and dead private helpers; no active feature code was removed because none of those safe deletion candidates were present.
- Preserved all client modules, HUDs, launcher services, module configuration screens and runtime behavior unchanged.

### Versioning
- MazClient advances from **1.10.15** to **1.10.16**.
- MazLauncher remains **0.6.50**.

## 1.10.15

### Vanilla utility icons
- Restored native Minecraft icon rendering for small utility buttons such as accessibility, language and similar helper controls.
- MazClient now leaves those icon contents intact and adds only a subtle Maz border/hover frame.
- Normal vanilla menu buttons continue using the full MazClient button styling.

### Versioning
- MazClient advances from **1.10.14** to **1.10.15**.

## 1.10.14

### Vanilla Minecraft menus with Maz styling
- Removed the custom Maz home and pause screen replacements so Minecraft now owns the title, pause, singleplayer, multiplayer, options and other vanilla menu layouts again.
- Kept MazClient's dark visual theme on supported vanilla menus by styling their backgrounds and buttons without moving Minecraft's widgets or replacing its screen instances.
- Removed the obsolete `MazHomeScreen`, `MazPauseScreen` and `LegacyMenuLifecycle` code paths so the old custom vanilla-menu replacements cannot return unexpectedly.

### Modules menu
- Kept the Maz Modules screen as the dedicated custom client menu opened with Right Shift.
- Restored the module search bar.
- Module search now filters by both module names and descriptions while keeping category grouping, scrolling and click targets in sync with filtered results.

### Versioning
- MazClient advances from **1.10.13** to **1.10.14**.
- FPS Booster behavior, configs, HUD behavior, managed mods and launcher behavior are unchanged.

## 1.10.13

### Restore the full old Maz menu flow
- Re-activated the historical `LegacyMenuLifecycle` as a Fabric client entrypoint so the surviving custom `MazHomeScreen` and `MazPauseScreen` are actually used again instead of only existing unused in source.
- The title screen now transitions into the original custom Maz home card after the existing four-tick stability guard; the in-world vanilla pause screen is replaced with the original custom Maz pause card only while a world is active.
- Keeps the restored 1.2-era Modules screen, HUD Editor, Module Details, FPS Booster, About and Config Profiles screens unchanged, so the whole Maz menu stack uses the old custom flow again.
- The existing disconnect/title safety guard remains intact; gameplay/container, chat, death and loading/progress screens remain untouched.
- FPS Booster behavior, render distance, simulation distance, configs, managed mods and launcher behavior are unchanged.

## 1.10.12

### Restore the original Maz menu
- Restored the module menu geometry and interaction model directly from the MazClient 1.2.1 implementation instead of approximating the later redesign.
- Restored the compact 500x320 card, 125px module-category sidebar, grouped 34px module rows, original toggle layout, draggable scrollbar and HUD Editor header action.
- Kept the current dark Maz palette, current runtime version text and the newer Combat category while removing the oversized 1.10-era module menu geometry.
- Module behavior, HUD behavior, configuration persistence and FPS Booster behavior are unchanged.

# MazClient Changelog

## 1.10.11

### Restore the old Maz menus
- Reverted the global Minecraft menu geometry to the exact pre-1.10.8 implementation from MazClient 1.10.7 instead of trying to patch the chopped 1.10.8 layout.
- Restores the older Maz menu behavior and layout while keeping the module-category sidebar confined to the Modules screen.
- Gameplay/container screens, chat, death and loading/progress screens remain excluded from global menu theming.

### Managed mod delivery
- MazLauncher 0.6.48 installers now carry the requested Fabric 26.2 managed mod stack inside the installer payload so fresh MazClient installs do not depend on live Modrinth resolution to receive those mods.
- Runtime Modrinth resolution remains available for updates/fallback, and the existing fail-closed verification still rejects incomplete managed stacks.
- Public release assets remain limited to exactly one Windows EXE installer; bundled third-party JARs remain internal to that installer.

### Versioning
- MazClient advances from **1.10.10** to **1.10.11**.
- MazLauncher advances from **0.6.47** to **0.6.48**.

Every public MazClient release must have notes here before the GitHub Release is published.

Versioning rule: MazClient patch versions run from `0` through `19`. After `X.Y.19`, the next release rolls over to `X.(Y+1).0` instead of using patch `20` or higher. Historical releases keep their original version numbers.

Historical notes through **1.10.1** are preserved verbatim in `archive/CHANGELOG_ARCHIVE_1.10.1_AND_EARLIER.md`.

## 1.10.10

### Complete module descriptions
- Added purpose-written descriptions for every module currently registered by MazClient across Performance, HUD, Combat, Utility and Visual categories.
- Descriptions now explain the actual displayed data or behavior, including specialized HUDs, warning thresholds/intent, input modules and visual toggles instead of falling back to a generic "MazClient module" label.
- Module search continues to index descriptions, and the existing module list/details UI displays the new text without changing module behavior or configuration.
- Removed stale description entries for the built-in Armor, Totem and Potion HUD modules replaced by managed dedicated mods in 1.10.9.
- FPS Booster behavior is unchanged and never modifies render distance or simulation distance.

### Versioning and distribution
- MazClient advances from **1.10.9** to **1.10.10**; MazLauncher remains **0.6.45**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer.

## 1.10.9

### Managed PvP/QoL mod stack
- Added managed Minecraft 26.2 Fabric installs for **Sodium Extra**, **Reese's Sodium Options**, **Balm**, **WalksyLib**, **Shield Fixes**, **Crosshair Addons Public**, **ukulib**, **TotemCounter**, **uku's Armor HUD**, and **Status Effect Timer**.
- **Indium is intentionally not included**: Sodium 0.6+ implements the Fabric Rendering API directly and current Indium is incompatible with modern Sodium.
- Removed MazClient's overlapping built-in **Armor Durability**, **Armor HUD**, **Totem Counter**, and **Potion HUD** modules so the dedicated mods own those features without duplicate overlays or duplicate telemetry work.
- Existing Fabric API, Sodium, Lithium, Simple Voice Chat, ImmediatelyFast, EntityCulling, and FerriteCore management remains intact.
- FPS Booster is unchanged and still never modifies render distance or simulation distance.

### Versioning and distribution
- MazClient advances from **1.10.8** to **1.10.9** and MazLauncher advances from **0.6.44** to **0.6.45** because launcher changes are required to provision the new managed stack.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal.

## 1.10.8

### Purpose-built Maz menu layouts
- Rebuilt normal Minecraft menu geometry around the MazClient dark-card design instead of wrapping vanilla widget placement in a global shell.
- Settings-style screens now use a compact two-column Maz control grid, while list-driven screens such as Multiplayer, World Selection, Resource Packs and Language keep their native content/list behavior with dedicated Maz action rows.
- Removed the fake global HUD Editor header control. Home, Pause and Modules retain their dedicated custom layouts, and the v1.4 module-category sidebar remains confined to Modules.
- Minecraft still owns the underlying vanilla screen instances, widgets, actions, focus and lifecycle, preserving disconnect/title stability while MazClient controls menu geometry and presentation.
- Gameplay/container screens, chat, death and loading/progress screens remain excluded from menu restyling.
- FPS Booster is unchanged and does not modify render distance or simulation distance.

### Versioning and distribution
- MazClient advances from **1.10.7** to **1.10.8**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal.

## 1.10.7

### Module sidebar confined to Modules
- Removed the module-category sidebar from the global Minecraft menu theme. The v1.4 category sidebar now appears **only** in `MazMenuScreen`, where Performance, HUD, Combat, Visual and Utility are real interactive module categories.
- Home and Pause remain fully custom Maz screens without a sidebar. Options, Video, Controls, Accessibility, Language, Multiplayer, World Selection, Resource Packs and similar normal menus keep the custom dark Maz panel/button treatment without displaying fake module navigation.
- Corrected the title lifecycle so returning to the title screen after leaving a world restores `MazHomeScreen` after the vanilla title widgets are safely initialized, rather than exposing the vanilla title layout. The existing stability delay remains to avoid panorama/disconnect softlocks.
- Old dark Maz colors, custom buttons, Minecraft utility icons and hover names, `Made by awnkr_par`, menu behavior, focus, narration and config persistence are preserved.
- FPS Booster remains unchanged and does not modify render distance or simulation distance.

### Versioning and distribution
- MazClient advances from **1.10.6** to **1.10.7**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw JARs remain internal.

## 1.10.6

### Totem Counter HUD
- Added an opt-in **Totem Counter** HUD module for Minecraft 26.2 that displays the local player's current Totem of Undying count, including a totem held in the offhand.
- The counter reads only already-loaded local inventory state. It never equips, refills, moves, selects, or automatically uses totems and sends no gameplay packets.
- Inventory counting is cached on a **500 ms telemetry cadence** rather than tied to render FPS or every client tick, so even very high-FPS sessions avoid unnecessary repeated inventory scans.
- The module uses MazClient's persistent HUD layout/opacity system and custom HUD styling, so it can be enabled, repositioned, and retained across sessions like the existing HUD modules.
- Fresh 26.2 research shows passive totem-count displays remain a common lightweight client-side PvP/QoL feature, while several neighboring totem mods include auto-equip/refill/cursor automation. MazClient intentionally implements only the passive counter behavior.
- FPS Booster is unchanged and still never modifies render distance or simulation distance. Config persistence, offline operation, smart caching, Simple Voice Chat management, managed mods, the custom dark v1.4 menu system, and disconnect stability are preserved.

### Versioning and distribution
- MazClient advances from **1.10.5** to **1.10.6**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cloud package path.

## 1.10.5

### Low air warning
- Added an opt-in **Low Air Warning** utility module for Minecraft 26.2 that samples only the local player's client-visible air supply twice per second.
- The module sends one local MazClient system message at about **5 seconds** of air remaining and escalates once at about **2 seconds**. It stays quiet at the same severity and resets after resurfacing, recovery, death, disconnect, or disable.
- This is warning-only QoL: it sends no packets, changes no movement, selects no items, and performs no automated swimming, breathing, or other gameplay actions.
- Fresh 26.2 research shows numerical oxygen awareness remains a useful client-side HUD/QoL pattern while the existing MazClient stack already covers many common performance optimizers and status displays. This release therefore adds a focused survival warning instead of another overlapping dependency.
- FPS Booster is unchanged and still never modifies render distance or simulation distance. Config persistence, offline operation, smart caching, Simple Voice Chat management, managed mods, the custom dark v1.4 menu system, and disconnect stability are preserved.

### Versioning and distribution
- MazClient advances from **1.10.4** to **1.10.5**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cloud package path.

## 1.10.4

### Low hunger warning
- Added an opt-in **Low Hunger Warning** utility module for Minecraft 26.2 that samples only the local player's client-visible food level once per second.
- The module sends one local MazClient system message when hunger drops to **6/20**, then escalates once at **2/20**. It stays quiet at the same severity and resets after recovery, death, disconnect, or disable.
- This is warning-only QoL: it sends no packets, selects no food, presses no use key, and performs no automatic eating or other gameplay automation.
- Fresh 26.2 research shows low-hunger alerts remain a common client-side QoL feature, while several current 26.2 mods automate eating. MazClient intentionally implements only the non-cheat reminder behavior.
- FPS Booster is unchanged and still never modifies render distance or simulation distance. Config persistence, offline operation, smart caching, Simple Voice Chat management, managed mods, the custom dark v1.4 menu system, and disconnect stability are preserved.

### Versioning and distribution
- MazClient advances from **1.10.3** to **1.10.4**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cloud package path.

## 1.10.3

### Low health warning
- Added an opt-in **Low Health Warning** utility module for Minecraft 26.2 that samples only the local player's client-visible health once per second.
- The module sends one local MazClient system message when health crosses **30%** of maximum, then escalates once at **15%**. It does not repeat the same severity every tick and resets cleanly after recovery, death, disconnect, or disable.
- Thresholds scale with modified maximum health rather than assuming ten hearts, so temporary health modifiers remain compatible.
- This feature sends no packets, uses no automated movement or item actions, and performs no gameplay automation. It is client-safe for singleplayer and normal multiplayer.
- Fresh 26.2 research continues to show configurable low-health visual warnings as a common lightweight QoL pattern, while the current performance ecosystem already has broad coverage from Sodium/Lithium/ImmediatelyFast/FerriteCore/culling stacks; this release therefore avoids another overlapping optimizer.
- FPS Booster is unchanged and still never modifies render distance or simulation distance. Config persistence, offline operation, smart caching, Simple Voice Chat management, managed mods, the custom dark v1.4 menu system, and disconnect stability are preserved.

### Versioning and distribution
- MazClient advances from **1.10.2** to **1.10.3**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cloud package path.

## 1.10.2

### Inventory full warning
- Added an opt-in **Inventory Full Warning** utility module for Minecraft 26.2 that checks only the 36 main inventory slots once per second using client-visible inventory state.
- The module emits one local MazClient system message when the inventory transitions from having free space to completely full, then stays quiet until at least one slot is freed before it can warn again. This avoids repeated chat spam while mining, looting, or farming.
- The check stops and resets cleanly when no player is present or when the module is disabled, preserving disconnect stability and avoiding stale state between worlds or servers.
- This feature sends no packets, moves no items, automates no inputs, and performs no gameplay actions. It is client-safe for singleplayer and normal multiplayer.
- Fresh Minecraft 26.2 research continues to show full-inventory alerts as a common lightweight QoL feature, while current performance packs already converge on Sodium, Lithium, ImmediatelyFast, FerriteCore, culling tools, and similar optimizers; this release therefore avoids stacking another overlapping performance dependency.
- FPS Booster is unchanged and still never modifies render distance or simulation distance. Config persistence, offline operation, smart caching, Simple Voice Chat management, managed mods, the custom dark v1.4 menu system, and disconnect stability are preserved.

### Versioning and distribution
- MazClient advances from **1.10.1** to **1.10.2**; MazLauncher remains **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cloud package path.

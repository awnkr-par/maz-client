## 0.6.80

- Automatically download and install a private Eclipse Temurin JDK 25 when no Java 25 runtime is available.
- Launch Minecraft with JDK 25 without requiring administrator privileges or changes to system Java settings.
- Reuse the installed runtime for future launches.

## 0.6.79

- Publish the updated Windows launcher with Fabric Loader 0.20.1 for Sodium Extra compatibility.
- Include Java 25 runtime selection for Fast Noise Ocl Integration on Minecraft 1.21.11.
- Refresh the cloud update payload and launcher version for existing installations.

## 0.6.78

- Update the bundled MazClient to 1.11.2 with FPS Booster refinements.
- Use Fabric Loader 0.20.1 for the Minecraft 26.2 and 1.21.11 compatibility paths.
- Improve Java 25 selection and startup error reporting for mods that require Java 25+.
- Refuse to publish a release when the current MazClient JAR cannot be compiled; never substitute an older cloud-channel JAR.

## 0.6.77

- Remove partial launcher installer downloads and their temporary directories when downloading or SHA-256 verification fails.
- Clean up installer files if starting the downloaded Windows installer fails.
- Preserve the original update failure when best-effort temporary-file cleanup is unsuccessful.

## 0.6.76

- Prevent launcher releases from silently overwriting older version tags when a new release cannot be created.
- Preserve unrelated assets when updating an existing release, so Windows and Linux installers can share a release safely.
- Verify the newly uploaded Windows installer by name without assuming the release contains only one platform asset.

## 0.6.75

- Scale the entire MazLauncher interface proportionally as the window is resized or restored from maximized mode.
- Keep tabs, buttons, labels and overlays aligned at compact window sizes without cropping the fixed design layout.

## 0.6.74

- Apply the existing Maz visual pack stack (Low Fire, Low Shield PvP and Smaller Totem) to Minecraft 1.21.11 as part of the final compatibility launch pipeline.
- Prepare and enable managed overlays immediately before Minecraft starts, after the game directory and Fabric profile are initialized.
- Keep Minecraft 26.2 support and the existing dark Maz menu palette intact.

## 0.6.73

- Added a Delete Selected Local Account button to the Accounts tab.
- Delete is available only when a local account is selected, and asks for confirmation.
- Removes the local account from saved launcher profiles without touching Minecraft worlds or Microsoft accounts.

## 0.6.72

- Replaced the Mods tab's raw JAR filename list with individual mod rows.
- Each row shows only the mod name and Enabled/Disabled toggle state.
- Quick Exp and Record-able appear in the same list as all other installed mods.
- Each optional mod can be toggled directly from its own row.

## 0.6.71

### All optional mods enabled by default and individually toggleable
- All optional managed mods, including Quick Exp and Record-able, are enabled by default for new installations.
- The Mods tab TOGGLE button can disable and re-enable managed mods without the launcher restoring them on the next launch.
- Explicitly disabled JARs remain disabled during bundled-mod synchronization and mod downloads.
- MazClient and Fabric API remain protected because disabling either can prevent the client from starting.
- Disable Quick Exp manually before joining servers whose rules prohibit it; restart Minecraft after toggling mods.

## 0.6.70

### Quick Exp is optional for PvP server compatibility
- Added a dedicated Quick Exp toggle in the launcher Mods tab.
- Quick Exp is now disabled by default and is not automatically installed as a required managed mod.
- Disabling removes its active JAR and persists across launches; enabling downloads a compatible JAR.
- Relaunch Minecraft after changing the toggle. Always follow the rules of the server you join.

## 0.6.69
- Added an Accounts tab to MazLauncher with saved local offline profiles and Microsoft account sign-in.
- Added account switching and restoration of saved Microsoft accounts.
- Local accounts are offline-only and do not provide premium-server authentication.
- MazLauncher advances from **0.6.68** to **0.6.69**.

## 0.6.68
- Made the Minecraft **1.21.11** core mod stack best-effort so a temporary Modrinth/CDN failure cannot prevent the base MazClient from launching.
- Added a Modrinth Maven fallback for managed mod downloads.
- Added Modrinth download metadata to managed mod requests.
- MazLauncher advances from **0.6.67** to **0.6.68**.

## 0.6.67
- Added the core MazLauncher mod stack to the Minecraft **1.21.11** compatibility profile: Sodium, Lithium, Simple Voice Chat, ImmediatelyFast, Entity Culling, FerriteCore, Cloth Config, Text Placeholder API, and Mod Menu.
- 1.21.11 now gets its own version-compatible Mod Menu instead of the 26.2 build.
- Prevented the 26.2-only optional performance services from running against the 1.21.11 profile and reintroducing wrong-version JARs.
- MazLauncher advances from **0.6.66** to **0.6.67**.

## 0.6.66
- Fixed the Minecraft **1.21.11** compatibility JAR cache invalidation so an already-installed broken 1.11.0 JAR is replaced after a compatibility rebuild with the same MazClient version.
- The launcher now records the compatibility build revision alongside the MazClient version marker.
- MazLauncher advances from **0.6.65** to **0.6.66**.

## 0.6.65
- Changed the Minecraft **1.21.11** MazClient profile to **base compatibility mode**: only Fabric API + MazClient are installed.
- Removed automatic Sodium, Lithium, and ImmediatelyFast injection from 1.21.11 so optional performance mods cannot prevent the target server profile from starting.
- Retained the new startup stdout/stderr capture and `mazlauncher-launch.log` diagnostics from 0.6.64.
- MazLauncher advances from **0.6.64** to **0.6.65**.

## 0.6.64
- Added real-time Minecraft stdout/stderr capture for MazLauncher launches.
- If Minecraft exits during startup, the launcher now includes the actual Java/Fabric output and writes the full diagnostic log to the selected game directory as `mazlauncher-launch.log`.
- This replaces the unhelpful generic early-exit message with the actual startup failure.
- MazLauncher advances from **0.6.63** to **0.6.64**.

## 0.6.63
- Fixed MazLauncher reporting a successful launch immediately after starting Java without checking whether Minecraft actually stayed running.
- MazLauncher now watches the new Minecraft process for the first few seconds and reports the real early exit code when Fabric/Java/profile startup fails.
- This makes 1.21.11 launch failures visible instead of looking like the Launch button did nothing.
- MazLauncher advances from **0.6.62** to **0.6.63**.

## 0.6.62
- Added a hard **Fabric API pre-launch check** for MazClient compatibility profiles.
- If Fabric API is missing after dependency resolution, MazLauncher now removes any broken/stale Fabric API files, retries installation once, and verifies that a non-empty Fabric API JAR is present before Minecraft starts.
- If Fabric API still cannot be installed, MazLauncher stops with a direct launcher error instead of opening Minecraft and letting Fabric crash with an incompatible-mods screen.
- Synced the launcher project and installer metadata so this build can publish cleanly.
- MazLauncher advances from **0.6.61** to **0.6.62**.

## 0.6.61
- Fixed the Minecraft **1.21.11** profile deleting its freshly installed **Fabric API** immediately before launch.
- Compatibility-profile cleanup now runs before dependency installation, so Fabric API 0.140.0+ remains present when Fabric starts.
- MazLauncher advances from **0.6.60** to **0.6.61**.

## 0.6.60
- Fixed an immediate **Minecraft 1.21.11 startup crash** caused by MazLauncher reusing bundled **Minecraft 26.2 managed mod JARs** inside the compatibility profile.
- Bundled managed mods are now used only for the primary Minecraft 26.2 profile.
- The 1.21.11 profile now deletes any wrong-version managed JARs left behind by older launcher builds before resolving fresh 1.21.11-compatible dependencies.
- MazLauncher advances from **0.6.59** to **0.6.60**.

## 0.6.59
- Added a **Minecraft version selector for MazClient** directly inside the normal MazLauncher.
- MazClient can now launch either the normal **Minecraft 26.2** build or the separate **Minecraft 1.21.11** compatibility build without installing a second launcher.
- Minecraft 1.21.11 uses its own isolated installation directory, Fabric profile, compatible dependency set, and MazClient JAR so its mods/config/cache cannot collide with 26.2.
- The 1.21.11 profile intentionally uses a smaller compatibility-first managed mod set; optional performance mods are skipped instead of blocking launch when a compatible build is unavailable.
- MazLauncher advances from **0.6.58** to **0.6.59**. MazClient remains **1.11.0** on both supported Minecraft lines.

## 0.6.58
- Brought **Record-able** back as an **optional recording add-on** instead of a permanently loaded managed mod.
- Added an **ENABLE RECORDER / DISABLE RECORDER** control in the Mods tab for each MazClient installation.
- Normal mode keeps Record-able completely absent from the active mods folder, preserving the smoother performance restored in 0.6.57.
- Enabling the recorder installs the bundled verified Fabric 26.2 Record-able JAR for that MazClient version; disabling it removes the JAR again so it cannot consume resources during normal play.
- The recorder remains packaged inside the installer for fast/offline enablement, but it is not loaded by Fabric unless the user explicitly enables recording mode.
- Record-able's current 26.2 releases provide low-end performance presets, adaptive capture rate, hardware encoding support and Deferred Capture for lower gameplay overhead while recording.
- MazLauncher advances from **0.6.57** to **0.6.58**. MazClient remains **1.11.0**.

## 0.6.57
- Removed **Record-able** from MazLauncher's required managed mod stack after it caused unacceptable performance overhead in normal gameplay.
- Fresh installs no longer bundle or synchronize Record-able.
- Upgrading existing MazClient installations now removes leftover `record-able*.jar` and `record-able*.jar.disabled` files from the instance mods folder so the recorder does not remain active after the launcher update.
- All other managed mods, including Mouse Tweaks and the performance stack, remain unchanged.
- MazLauncher advances from **0.6.56** to **0.6.57**.

## 0.6.56
- Added **Record-able** for Fabric 26.2 to the required managed mod stack.
- Record-able provides direct in-game gameplay/video and game-audio recording with configurable start/stop recording keybinds, replay-buffer saves, bookmarks, settings hotkeys, cancel recording, and other bindable controls.
- The mod supports hardware encoders such as NVENC, AMF, and QuickSync and can download FFmpeg on first use instead of requiring a bundled FFmpeg binary.
- MazLauncher now resolves, bundles, synchronizes, protects, and verifies Record-able with the rest of the managed stack.
- MazLauncher advances from **0.6.55** to **0.6.56**.

## 0.6.55
- Added **Mouse Tweaks 2.31 for Fabric 26.2** to the required managed mod stack.
- Mouse Tweaks improves inventory handling with enhanced RMB dragging, LMB drag interactions, and scroll-wheel item movement.
- The mod is client-side and uses the already-managed Fabric API dependency.
- MazLauncher now resolves, bundles, synchronizes, protects, and verifies Mouse Tweaks with the rest of the managed stack.
- MazLauncher advances from **0.6.54** to **0.6.55**.

## 0.6.54
- Fixed a startup-blocking dependency error introduced with Fast Noise: **zconfig** is now included as a required managed Minecraft 26.2 Fabric mod.
- MazLauncher now resolves, bundles, synchronizes, protects, and verifies zconfig alongside Fast Noise so Fabric can satisfy Fast Noise's `zconfig >= 1.0.0+26.x` requirement.
- Existing MazClient 1.10.19 installs receive zconfig through the same managed-mod synchronization path before launch.
- MazLauncher advances from **0.6.53** to **0.6.54**.

## 0.6.53
- Added **More Culling** to the always-on managed Minecraft 26.2 Fabric stack to skip additional hidden block/model surfaces and reduce unnecessary rendering work.
- Added **Cloth Config API**, required by More Culling's 26.2 Fabric release.
- Added **Fast Noise** to reduce vanilla world-generation CPU cost and smooth chunk-generation/loading spikes while preserving vanilla-compatible generation behavior.
- Kept **C2ME** out of the required stack for now because its Minecraft 26.2 builds are beta/alpha rather than release-grade; MazLauncher continues to require stable release builds for managed mods.
- These optimizations are always present in MazClient installations and are not exposed as MazClient toggle modules.
- MazLauncher advances from **0.6.52** to **0.6.53**.

## 0.6.52
- Added **Quick Exp** to the managed Minecraft 26.2 Fabric stack as the compatible rapid XP-bottle mod for this Minecraft version.
- Added **Multi Key Bindings** to the managed stack so players can assign multiple keys and modifier combinations to the same Minecraft action.
- Both mods are now resolved, bundled into the installer payload, synchronized into MazClient instances, and treated as required managed mods by MazLauncher.
- MazLauncher advances from **0.6.51** to **0.6.52**.

## 0.6.51
- Fixed the Skins tab cape preview so imported cape PNGs render as a separate cape model behind the player instead of appearing almost flush against the base body.
- Preserved Minecraft's standard cape UV mapping while adding a small shoulder offset/angle so the cape reads as its own layer in the 3D preview.
- Added a **RESET** button beside **IMPORT CAPE PNG** that clears the local cape preview and cape selection without changing the skin or the Minecraft account's active cape.
- MazLauncher advances from **0.6.50** to **0.6.51**.

## 0.6.50
- Fixed the launcher Mod List so opening it synchronizes the CI-bundled required mod JARs into the selected MazClient instance before enumerating installed mods.
- Required managed mods now physically appear in the instance mods directory and Mod List without requiring a game launch first.
- MazLauncher advances from **0.6.49** to **0.6.50**.

## 0.6.49
- Fixed bundled managed-mod installation to use a release-generated project-to-filename manifest instead of guessed filename prefixes.
- This fixes bundled JAR discovery for real filenames such as WalksyLib, shieldfixes, CrosshairAddons and uku's Armor HUD.
- MazLauncher now copies the exact CI-downloaded JAR for each required project into the launched MazClient instance's mods directory and validates the copied file before launch.
- MazLauncher advances from **0.6.48** to **0.6.49**.

# MazLauncher Changelog

## 0.6.48
- Bundles the complete requested Fabric 26.2 managed mod stack inside the MazLauncher installer and installs those exact JARs into the active MazClient version directory before using the network fallback.
- Keeps fail-closed managed-mod verification, so a missing bundled/downloaded JAR cannot silently produce a partial client.
- MazLauncher advances from **0.6.47** to **0.6.48**.

## 0.6.47
- Made the expanded managed mod stack fail-closed: MazLauncher now records the exact JAR resolved for every required mod and verifies that file exists after installation/cache fallback.
- A failed or missing required mod now produces one explicit error naming every missing/failed Modrinth project instead of allowing MazClient to launch with a silently incomplete stack.
- Offline fallback remains supported only when a previously downloaded matching managed JAR is actually present.
- MazLauncher advances from **0.6.46** to **0.6.47**.

## 0.6.46
- Fixed ComboBox dropdown popup rows so their backgrounds, borders, hover state, selected state and text all follow MazLauncher's dynamic theme resources.
- Dark mode dropdowns now stay dark instead of mixing dark text/theme styling with Windows' light popup background; light mode continues to use the light palette.
- MazLauncher advances from **0.6.45** to **0.6.46**.

## 0.6.45
- Expanded the managed Minecraft 26.2 stack with Sodium Extra, Reese's Sodium Options, Balm, WalksyLib, Shield Fixes, Crosshair Addons Public, ukulib, TotemCounter, uku's Armor HUD, and Status Effect Timer.
- Keeps required libraries managed alongside their dependent mods and preserves offline fallback to already-cached JARs.
- Indium is intentionally excluded because modern Sodium provides Fabric Rendering API support itself and Indium is incompatible with Sodium 0.6+.
- MazLauncher advances from **0.6.44** to **0.6.45**.


Historical notes through **0.6.40** are preserved verbatim in `archive/LAUNCHER_CHANGELOG_ARCHIVE_0.6.40_AND_EARLIER.md`.

## 0.6.44

### Coalesced cloud-manifest reads
- Added a short-lived in-memory cache for the validated cloud manifest so launch/update paths that ask for the same manifest within 30 seconds reuse one result instead of repeating identical GitHub requests.
- Added a `SemaphoreSlim` gate around manifest refreshes so concurrent callers share one network/cache refresh instead of racing multiple downloads and cache writes.
- Online and offline manifest results both populate the same memory cache after successful parsing; malformed or unavailable manifests are never cached as valid state.
- Preserved the existing atomic on-disk manifest cache, 5-second network timeout, SHA256 verification, offline fallback, and launcher-update verification behavior.
- Existing MazClient config, HUD setup, managed performance mods, resource packs, account cache, offline operation, smart caching, Simple Voice Chat management, and disconnect stability remain unchanged.
- FPS Booster is unchanged and still never modifies render distance or simulation distance.

### Research notes
- Current Minecraft 26.2 client packs still commonly combine Sodium/Lithium with targeted optimizers such as ImmediatelyFast, Entity Culling, FerriteCore, ModernFix and related culling/loading tools. MazLauncher already covers the core managed stack, so this cycle avoids stacking another overlapping performance mod merely for feature count.
- Current 26.2 HUD/QoL clients commonly expose FPS, ping, coordinates and frame/memory diagnostics. MazClient already includes those capabilities, including detailed frame pacing and GC diagnostics, so this cycle focuses on launcher reliability rather than duplicating HUD modules.

### Versioning and distribution
- This is a **MazLauncher-only reliability release**. MazClient remains **1.9.16**.
- MazLauncher assembly and Inno Setup metadata are synchronized at **0.6.44**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cache path.

## 0.6.43

### Atomic cloud-manifest cache
- Changed MazLauncher's cloud manifest cache to use a write-to-temp + atomic replace path instead of writing directly over the last known-good cache file.
- A launcher interruption, disk hiccup, or process termination during a manifest refresh can no longer leave a partially written `latest-cloud-manifest.json` behind and destroy offline fallback state.
- Online manifest parsing and offline cached-manifest parsing now share the same deserialization path so fallback behavior stays consistent.
- Temporary manifest cache files are cleaned up on both success and failure.
- Existing MazClient config, HUD setup, managed performance mods, resource packs, account cache, offline operation, smart caching, Simple Voice Chat management, and disconnect stability remain unchanged.
- FPS Booster is unchanged and still never modifies render distance or simulation distance.

### Research notes
- Current Minecraft 26.2 Fabric performance stacks continue to center on Sodium/Lithium plus targeted rendering and memory optimizations; MazLauncher already manages Sodium, Lithium, ImmediatelyFast, Entity Culling, and FerriteCore, so this cycle avoids adding another overlapping optimization mod.
- Simple Voice Chat 2.6.22 is the current stable Minecraft 26.2 Fabric release observed during this cycle and includes fixes for reconnect races, audio initialization failures, OpenAL resource leaks, and speaker breakage. MazLauncher's existing Modrinth resolver already selects the latest stable compatible release automatically, so no hardcoded voice-chat version change is needed.

### Versioning and distribution
- This is a **MazLauncher-only reliability release**. MazClient remains **1.9.3**.
- MazLauncher assembly and Inno Setup metadata are synchronized at **0.6.43**.
- Public GitHub Release assets must remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cache path.

## 0.6.42

### 3D skin and cape appearance preview
- Replaced the flat skin-image preview path with an interactive **3D Minecraft player preview** built directly into MazLauncher. Imported skins are mapped onto the block model, Classic/Steve and Slim/Alex arm widths update live, and the model can be dragged to rotate or mouse-wheeled to zoom.
- Added cape support to the Skins tab. Signed-in users can load the capes their Minecraft account already owns, preview each cape on the same 3D player, activate an owned cape, or hide the currently active account cape.
- The launcher reads the signed-in profile from Minecraft Services and can also load the currently active account skin into the 3D preview when no local skin PNG has been selected.
- Added **custom cape PNG import for local 3D preview**. Standard 2:1 cape atlases such as 64x32, 128x64, and 256x128 are accepted and wrapped onto the 3D cape model.
- Custom cape PNGs are deliberately not presented as account uploads: Minecraft Services only allows activating a cape entitlement already owned by the signed-in account. The launcher explains this distinction instead of pretending an arbitrary cape can be uploaded to Mojang.
- Skin uploading continues to use the existing authenticated Minecraft Services skin endpoint; cape activation uses the account cape endpoint and never alters gameplay packets or server behavior.
- Existing MazClient config, HUD setup, managed performance mods, resource packs, account cache, offline behavior, smart caching, Simple Voice Chat management, and disconnect stability remain untouched.
- FPS Booster is unchanged and still never modifies render distance or simulation distance.

### Versioning and distribution
- This is a **MazLauncher-only appearance/QoL release**. MazClient remains **1.9.3**.
- MazLauncher assembly and Inno Setup metadata are synchronized at **0.6.42**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cache path.

## 0.6.41

### SmoothHud removal
- Removed **SmoothHud** from MazLauncher's managed Minecraft 26.2 Fabric stack. MazLauncher no longer resolves, downloads, validates, caches, or launches SmoothHud.
- Deleted the dedicated `SmoothHudService` integration and removed SmoothHud from managed-stack initialization.
- Expanded retired-mod cleanup so existing MazClient installations automatically delete legacy SmoothHud JARs, the `.maz-smoothhud` marker, and SmoothHud-named config files on launcher startup. This covers previously managed cached installs so upgrading users do not keep the mod or its leftover config after the integration is removed.
- BetterF3 retirement cleanup remains intact.
- Existing MazClient config, HUD setup, performance mods, resource packs, account cache, offline behavior, smart caching, Simple Voice Chat management, and disconnect stability remain untouched.
- FPS Booster is unchanged and still never modifies render distance or simulation distance.

### Versioning and distribution
- This is a **MazLauncher-only** removal/cleanup release. MazClient remains unchanged.
- MazLauncher assembly and Inno Setup metadata are synchronized at **0.6.41**.
- Public GitHub Release assets remain limited to exactly one Windows EXE installer; raw MazClient and third-party mod JARs remain internal to the installer/cache path.

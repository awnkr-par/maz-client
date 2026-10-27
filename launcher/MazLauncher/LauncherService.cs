using System.Text;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.Auth.Microsoft;
using CmlLib.Core.ProcessBuilder;

namespace MazLauncher;

public sealed class LauncherService
{
    public const string MinecraftVersion = "26.2";
    public const string CompatibilityMinecraftVersion = "1.21.11";
    public const string CompatibilityMazClientVersion = "1.11.0";
    // Bump this whenever the same-version compatibility JAR is rebuilt so existing
    // installations do not keep a stale cached JAR with the same MazClient version.
    private const string CompatibilityMazClientBuildId = "c8a6cdbe72c81b7c5d1ff819297000e72af81c63";
    public const string FabricLoaderVersion = "0.20.1";

    public static IReadOnlyList<string> SupportedMazMinecraftVersions { get; } =
        new[] { MinecraftVersion, CompatibilityMinecraftVersion };

    private static readonly string DataRoot = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "MazLauncher"
    );
    private static readonly string CacheRoot = Path.Combine(DataRoot, "cache");
    private static readonly string VanillaCatalogCache = Path.Combine(CacheRoot, "vanilla-versions.txt");
    private static readonly string MazCatalogCache = Path.Combine(CacheRoot, "mazclient-versions.txt");
    private static readonly string LatestVanillaMarker = Path.Combine(CacheRoot, "latest-vanilla-cached.txt");
    private static readonly string LatestMazMarker = Path.Combine(CacheRoot, "latest-mazclient-cached.txt");
    private const string QuickExpSlug = "quick-exp";
    private const string QuickExpPrefix = "quick";
    private const string QuickExpMarker = ".quick-exp-disabled";
    private const string RecordableSlug = "record-able";
    private const string RecordablePrefix = "record-able";
    private const string RecordableMarker = ".recordable-disabled";

    private static readonly (string Slug, string Prefix)[] ExpandedManagedMods =
    {
        ("sodium-extra", "sodium-extra"),
        ("reeses-sodium-options", "reeses"),
        ("balm", "balm"),
        ("walksylib", "walksylib"),
        ("complete-shield-fixes", "shield"),
        ("crosshair-addons-public", "crosshair"),
        ("ukulib", "ukulib"),
        ("totemcounter", "totemcounter"),
        ("ukus-armor-hud", "armor-hud"),
        ("tiertagger", "tiertagger"),
        ("statuseffecttimer", "statuseffecttimer"),
        ("multi-key-bindings", "multi-key-bindings"),
        ("cloth-config", "cloth-config"),
        ("moreculling", "moreculling"),
        ("zconfig", "zconfig"),
        ("zfastnoise", "zfastnoise"),
        ("mouse-tweaks", "MouseTweaks")
    };

    private readonly HttpClient http = new();
    private readonly JELoginHandler loginHandler;
    private readonly CloudUpdateService cloudUpdates = new();

    public LauncherService()
    {
        Directory.CreateDirectory(DataRoot);
        Directory.CreateDirectory(CacheRoot);
        Directory.CreateDirectory(Path.Combine(DataRoot, "installations"));

        loginHandler = new JELoginHandlerBuilder()
            .WithAccountManager(Path.Combine(CacheRoot, "accounts.json"))
            .Build();

        http.DefaultRequestHeaders.UserAgent.ParseAdd($"MazLauncher/{CloudUpdateService.CurrentLauncherVersion}");
    }

    public async Task<MSession> SignInAsync() => await loginHandler.Authenticate();

    public async Task SignOutAsync() => await loginHandler.SignoutWithBrowser();

    public async Task<MSession?> TryRestoreSessionAsync()
    {
        var account = loginHandler.AccountManager.GetAccounts().FirstOrDefault();
        if (account == null) return null;
        try { return await loginHandler.Authenticate(account); }
        catch { return null; }
    }

    public async Task<IReadOnlyList<MSession>> TryRestoreSessionsAsync()
    {
        var restored = new List<MSession>();
        foreach (var account in loginHandler.AccountManager.GetAccounts())
        {
            try
            {
                var authenticated = await loginHandler.Authenticate(account);
                if (!restored.Any(existing => string.Equals(existing.Username, authenticated.Username, StringComparison.OrdinalIgnoreCase)))
                    restored.Add(authenticated);
            }
            catch
            {
                // One expired or revoked account must not prevent other saved accounts from appearing.
            }
        }

        return restored;
    }

    public Task<CloudManifest?> GetCloudManifestAsync() => cloudUpdates.GetManifestAsync();

    public async Task CheckForLauncherUpdateAsync()
    {
        if (await cloudUpdates.HasLauncherUpdateAsync())
            await cloudUpdates.DownloadAndApplyLauncherUpdateAsync();
    }

    public async Task<IReadOnlyList<string>> GetVanillaVersionsAsync()
    {
        try
        {
            var catalogPath = Path.Combine(DataRoot, "catalog");
            Directory.CreateDirectory(catalogPath);
            var launcher = new MinecraftLauncher(new MinecraftPath(catalogPath));
            var versions = (await launcher.GetAllVersionsAsync())
                .Select(v => v.Name)
                .Where(name => !string.IsNullOrWhiteSpace(name) && !name.StartsWith("fabric-loader-", StringComparison.OrdinalIgnoreCase))
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToList();
            await WriteVersionCacheAsync(VanillaCatalogCache, versions);
            return versions;
        }
        catch
        {
            var cached = ReadVersionCache(VanillaCatalogCache);
            if (cached.Count > 0) return cached;
            return new[] { MinecraftVersion };
        }
    }

    public async Task<IReadOnlyList<string>> GetMazClientVersionsAsync()
    {
        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, "https://api.github.com/repos/km7bnv/maz-client/releases?per_page=100");
            request.Headers.Accept.ParseAdd("application/vnd.github+json");
            using var response = await http.SendAsync(request);
            response.EnsureSuccessStatusCode();
            using var stream = await response.Content.ReadAsStreamAsync();
            using var doc = await JsonDocument.ParseAsync(stream);

            var versions = new List<string>();
            foreach (var release in doc.RootElement.EnumerateArray())
            {
                if (release.TryGetProperty("draft", out var draft) && draft.GetBoolean()) continue;
                if (!release.TryGetProperty("tag_name", out var tagElement)) continue;
                var tag = tagElement.GetString();
                if (string.IsNullOrWhiteSpace(tag) || !tag.StartsWith("v", StringComparison.OrdinalIgnoreCase)) continue;
                var version = tag[1..];
                if (Version.TryParse(version, out _)) versions.Add(version);
            }

            var ordered = versions
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .OrderByDescending(v => Version.TryParse(v, out var parsed) ? parsed : new Version(0, 0))
                .ToList();
            await WriteVersionCacheAsync(MazCatalogCache, ordered);
            return ordered;
        }
        catch
        {
            var cached = ReadVersionCache(MazCatalogCache);
            if (cached.Count > 0) return cached;
            var manifest = await GetCloudManifestAsync();
            if (manifest != null && Version.TryParse(manifest.MazClientVersion, out _))
                return new[] { manifest.MazClientVersion };
            return Array.Empty<string>();
        }
    }

    public async Task WarmLatestOfflineCacheAsync(MSession session, Action<string, int>? progress = null)
    {
        var vanillaDir = Path.Combine(DataRoot, "installations", "vanilla", SafeName(MinecraftVersion));
        var vanillaMarker = await ReadMarkerAsync(LatestVanillaMarker);
        var vanillaCurrent = string.Equals(vanillaMarker, MinecraftVersion, StringComparison.OrdinalIgnoreCase)
                             && Directory.Exists(vanillaDir)
                             && Directory.Exists(Path.Combine(vanillaDir, "versions"));

        if (vanillaCurrent)
        {
            progress?.Invoke($"Vanilla {MinecraftVersion} cache already current — skipping", 20);
        }
        else
        {
            progress?.Invoke($"Vanilla cache needs refresh — caching {MinecraftVersion}...", 5);
            Directory.CreateDirectory(vanillaDir);
            await PrepareVersionAsync(vanillaDir, MinecraftVersion, session, progress);
            await File.WriteAllTextAsync(LatestVanillaMarker, MinecraftVersion);
            progress?.Invoke($"Vanilla {MinecraftVersion} cache refreshed", 45);
        }

        progress?.Invoke("Checking latest MazClient cache version...", 50);
        var mazVersions = await GetMazClientVersionsAsync();
        var latestMaz = mazVersions.FirstOrDefault();
        if (string.IsNullOrWhiteSpace(latestMaz))
        {
            progress?.Invoke("MazClient version unavailable — keeping existing cache", 100);
            return;
        }

        var mazDir = GetMazInstallationDirectory(latestMaz);
        var modsDir = Path.Combine(mazDir, "mods");
        var mazMarker = await ReadMarkerAsync(LatestMazMarker);
        var fabricVersion = $"fabric-loader-{FabricLoaderVersion}-{MinecraftVersion}";
        var fabricProfile = Path.Combine(mazDir, "versions", fabricVersion, fabricVersion + ".json");
        var mazJar = Path.Combine(modsDir, "maz-client.jar");
        var mazVersionMarker = Path.Combine(modsDir, ".mazclient-version");
        var installedMazVersion = await ReadMarkerAsync(mazVersionMarker);
        var voiceChatPresent = HasManagedMod(modsDir, "voicechat-");
        var immediatelyFastPresent = HasManagedMod(modsDir, "ImmediatelyFast-");
        var entityCullingPresent = HasManagedMod(modsDir, "entityculling-");
        var ferriteCorePresent = HasManagedMod(modsDir, "ferritecore-");
        var mazCurrent = string.Equals(mazMarker, latestMaz, StringComparison.OrdinalIgnoreCase)
                         && string.Equals(installedMazVersion, latestMaz, StringComparison.OrdinalIgnoreCase)
                         && File.Exists(mazJar)
                         && File.Exists(fabricProfile)
                         && voiceChatPresent
                         && immediatelyFastPresent
                         && entityCullingPresent
                         && ferriteCorePresent;

        if (mazCurrent)
        {
            progress?.Invoke($"MazClient {latestMaz} cache already current — skipping", 100);
            return;
        }

        progress?.Invoke($"MazClient cache needs refresh — caching {latestMaz}...", 55);
        Directory.CreateDirectory(mazDir);
        await EnsureFabricProfileAsync(mazDir, MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "fabric-api", "fabric-api-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "sodium", "sodium-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "lithium", "lithium-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "simple-voice-chat", "voicechat-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "immediatelyfast", "ImmediatelyFast-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "entityculling", "entityculling-", MinecraftVersion);
        await EnsureModrinthModAsync(mazDir, "ferrite-core", "ferritecore-", MinecraftVersion);
        await EnsureExpandedManagedModsAsync(mazDir, MinecraftVersion);
        await SyncQuickExpAddonAsync(mazDir, MinecraftVersion);
        CleanupOldMazClientJars(mazDir);
        await EnsureMazClientVersionAsync(mazDir, latestMaz, progress);
        await PrepareVersionAsync(mazDir, fabricVersion, session, progress);
        await File.WriteAllTextAsync(LatestMazMarker, latestMaz);
        progress?.Invoke($"Offline cache ready: Vanilla {MinecraftVersion} + MazClient {latestMaz}", 100);
    }

    public Task LaunchVanillaAsync(MSession session, Action<string, int>? progress = null) =>
        LaunchVanillaAsync(session, MinecraftVersion, progress);

    public async Task LaunchVanillaAsync(MSession session, string version, Action<string, int>? progress = null)
    {
        if (string.IsNullOrWhiteSpace(version)) throw new ArgumentException("Choose a Vanilla Minecraft version first.", nameof(version));
        var gameDir = Path.Combine(DataRoot, "installations", "vanilla", SafeName(version));
        Directory.CreateDirectory(gameDir);
        await LaunchAsync(gameDir, version, session, progress);
    }

    public Task LaunchMazAsync(MSession session, Action<string, int>? progress = null) =>
        LaunchMazLatestAsync(session, progress);

    private async Task LaunchMazLatestAsync(MSession session, Action<string, int>? progress)
    {
        var manifest = await GetCloudManifestAsync();
        if (manifest == null) throw new InvalidOperationException("MazClient version list is unavailable.");
        await LaunchMazAsync(session, manifest.MazClientVersion, progress);
    }

    public Task LaunchMazAsync(MSession session, string mazClientVersion, Action<string, int>? progress = null) =>
        LaunchMazAsync(session, mazClientVersion, MinecraftVersion, progress);

    public async Task LaunchMazAsync(MSession session, string mazClientVersion, string minecraftVersion, Action<string, int>? progress = null)
    {
        if (string.IsNullOrWhiteSpace(mazClientVersion)) throw new ArgumentException("Choose a MazClient version first.", nameof(mazClientVersion));
        if (!SupportedMazMinecraftVersions.Contains(minecraftVersion, StringComparer.OrdinalIgnoreCase))
            throw new ArgumentException($"MazClient does not support Minecraft {minecraftVersion} in this launcher.", nameof(minecraftVersion));

        if (string.Equals(minecraftVersion, CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
            mazClientVersion = CompatibilityMazClientVersion;

        var gameDir = GetMazInstallationDirectory(mazClientVersion, minecraftVersion);
        Directory.CreateDirectory(gameDir);

        progress?.Invoke($"Preparing MazClient {mazClientVersion} for Minecraft {minecraftVersion}...", 5);
        await EnsureFabricProfileAsync(gameDir, minecraftVersion);

        // Clean stale/wrong-version managed JARs before installing this profile's
        // dependencies. Doing this after Fabric API installation would delete the
        // freshly installed 1.21.11 Fabric API and make Fabric reject MazClient.
        if (string.Equals(minecraftVersion, CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
            CleanupCompatibilityMods(gameDir);

        progress?.Invoke("Checking Fabric API...", 15);
        await EnsureRequiredFabricApiAsync(gameDir, minecraftVersion, progress);

        if (string.Equals(minecraftVersion, CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
        {
            // 1.21.11 now gets the same core MazLauncher mod experience as 26.2,
            // using Modrinth's exact 1.21.11-compatible builds instead of the 26.2 jars.
            // Keep this stack deliberately limited to mods confirmed compatible with
            // the compatibility profile so the proven base client remains stable.
            progress?.Invoke("Checking Sodium...", 22);
            await TryEnsureCompatibilityModAsync(gameDir, "sodium", "sodium-", minecraftVersion, progress);
            progress?.Invoke("Checking Lithium...", 27);
            await TryEnsureCompatibilityModAsync(gameDir, "lithium", "lithium-", minecraftVersion, progress);
            progress?.Invoke("Checking Simple Voice Chat...", 32);
            await TryEnsureCompatibilityModAsync(gameDir, "simple-voice-chat", "voicechat-", minecraftVersion, progress);
            progress?.Invoke("Checking ImmediatelyFast...", 37);
            await TryEnsureCompatibilityModAsync(gameDir, "immediatelyfast", "ImmediatelyFast-", minecraftVersion, progress);
            progress?.Invoke("Checking Entity Culling...", 41);
            await TryEnsureCompatibilityModAsync(gameDir, "entityculling", "entityculling-", minecraftVersion, progress);
            progress?.Invoke("Checking FerriteCore...", 45);
            await TryEnsureCompatibilityModAsync(gameDir, "ferrite-core", "ferritecore-", minecraftVersion, progress);
            progress?.Invoke("Checking Cloth Config...", 49);
            await TryEnsureCompatibilityModAsync(gameDir, "cloth-config", "cloth-config-", minecraftVersion, progress);
            progress?.Invoke("Checking Text Placeholder API...", 53);
            await TryEnsureCompatibilityModAsync(gameDir, "placeholder-api", "placeholder-api-", minecraftVersion, progress);
            progress?.Invoke("Checking Mod Menu...", 57);
            await TryEnsureCompatibilityModAsync(gameDir, "modmenu", "modmenu-", minecraftVersion, progress);
            progress?.Invoke("Checking full MazClient mod stack...", 61);
            await EnsureExpandedManagedModsAsync(gameDir, minecraftVersion);
            await SyncQuickExpAddonAsync(gameDir, minecraftVersion);
            RemoveRecordableFiles(Path.Combine(gameDir, "mods"));
        }
        else
        {
            progress?.Invoke("Checking Sodium...", 22);
            await EnsureModrinthModAsync(gameDir, "sodium", "sodium-", minecraftVersion);
            progress?.Invoke("Checking Lithium...", 29);
            await EnsureModrinthModAsync(gameDir, "lithium", "lithium-", minecraftVersion);
            progress?.Invoke("Checking Simple Voice Chat...", 34);
            await EnsureModrinthModAsync(gameDir, "simple-voice-chat", "voicechat-", minecraftVersion);
            progress?.Invoke("Checking ImmediatelyFast...", 37);
            await EnsureModrinthModAsync(gameDir, "immediatelyfast", "ImmediatelyFast-", minecraftVersion);
            progress?.Invoke("Checking Entity Culling...", 39);
            await EnsureModrinthModAsync(gameDir, "entityculling", "entityculling-", minecraftVersion);
            progress?.Invoke("Checking FerriteCore...", 41);
            await EnsureModrinthModAsync(gameDir, "ferrite-core", "ferritecore-", minecraftVersion);
            progress?.Invoke("Checking expanded MazClient mod stack...", 44);
            await EnsureExpandedManagedModsAsync(gameDir, minecraftVersion);
            await SyncQuickExpAddonAsync(gameDir, minecraftVersion);
            await SyncRecordableAddonAsync(gameDir, minecraftVersion);
        }

        CleanupOldMazClientJars(gameDir);
        await EnsureMazClientVersionAsync(gameDir, mazClientVersion, minecraftVersion, progress);

        // Prepare the overlays after the compatibility profile and its game directory
        // are fully initialized, immediately before Minecraft reads options.txt.
        // This applies the same Maz visual-pack stack to 1.21.11 and 26.2.
        progress?.Invoke("Applying Maz visual palette: Low Fire, Low Shield and Smaller Totem...", 88);
        await new ManagedResourcePackService().EnsureForMazClientAsync(mazClientVersion, minecraftVersion);

        var fabricVersion = $"fabric-loader-{FabricLoaderVersion}-{minecraftVersion}";
        await LaunchAsync(gameDir, fabricVersion, session, progress);
    }

    private async Task EnsureRequiredFabricApiAsync(string gameDir, string minecraftVersion, Action<string, int>? progress)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);

        await EnsureModrinthModAsync(gameDir, "fabric-api", "fabric-api-", minecraftVersion);

        var installed = Directory.EnumerateFiles(modsDir)
            .FirstOrDefault(path => Path.GetFileName(path).StartsWith("fabric-api-", StringComparison.OrdinalIgnoreCase)
                                    && path.EndsWith(".jar", StringComparison.OrdinalIgnoreCase));

        if (installed != null && File.Exists(installed) && new FileInfo(installed).Length > 10_000)
            return;

        progress?.Invoke($"Fabric API did not remain installed for Minecraft {minecraftVersion}; retrying...", 16);

        foreach (var path in Directory.EnumerateFiles(modsDir)
                     .Where(path => Path.GetFileName(path).StartsWith("fabric-api-", StringComparison.OrdinalIgnoreCase)))
        {
            try { File.Delete(path); } catch { }
        }

        await EnsureModrinthModAsync(gameDir, "fabric-api", "fabric-api-", minecraftVersion);

        installed = Directory.EnumerateFiles(modsDir)
            .FirstOrDefault(path => Path.GetFileName(path).StartsWith("fabric-api-", StringComparison.OrdinalIgnoreCase)
                                    && path.EndsWith(".jar", StringComparison.OrdinalIgnoreCase));

        if (installed == null || !File.Exists(installed) || new FileInfo(installed).Length <= 10_000)
            throw new InvalidOperationException(
                $"MazLauncher could not install Fabric API for Minecraft {minecraftVersion}. Launch was stopped before starting Minecraft.");
    }

    private async Task TryEnsureOptionalModAsync(
        string gameDir,
        string projectSlug,
        string filePrefix,
        string minecraftVersion,
        string displayName,
        Action<string, int>? progress,
        int progressValue)
    {
        progress?.Invoke($"Checking {displayName}...", progressValue);
        try
        {
            await EnsureModrinthModAsync(gameDir, projectSlug, filePrefix, minecraftVersion);
        }
        catch (Exception ex)
        {
            progress?.Invoke($"{displayName} unavailable for {minecraftVersion} — continuing without it", progressValue);
            System.Diagnostics.Debug.WriteLine($"{displayName} optional compatibility mod skipped: {ex.Message}");
        }
    }

    public string GetMazModsDirectory(string mazClientVersion)
    {
        var mods = Path.Combine(GetMazInstallationDirectory(mazClientVersion), "mods");
        Directory.CreateDirectory(mods);
        return mods;
    }

    public IReadOnlyList<string> GetInstalledMods(string mazClientVersion)
    {
        var dir = GetMazModsDirectory(mazClientVersion);
        SyncBundledManagedModsToInstance(dir);
        RemoveQuickExpFilesIfDisabled(dir);
        if (File.Exists(Path.Combine(dir, RecordableMarker))) RemoveRecordableFiles(dir);
        return Directory.EnumerateFiles(dir)
            .Where(path => path.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) || path.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase))
            .Select(Path.GetFileName)
            .Where(name => name != null)
            .Cast<string>()
            .OrderBy(name => name, StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    public string GetModDisplayName(string fileName)
    {
        if (fileName.StartsWith(QuickExpPrefix, StringComparison.OrdinalIgnoreCase)) return "Quick Exp";
        if (fileName.StartsWith(RecordablePrefix, StringComparison.OrdinalIgnoreCase)) return "Record-able";
        foreach (var mod in ExpandedManagedMods)
            if (fileName.StartsWith(mod.Prefix, StringComparison.OrdinalIgnoreCase))
                return mod.Slug.Replace('-', ' ');
        foreach (var (prefix, name) in new (string Prefix, string Name)[] {
            ("maz-client", "MazClient"), ("fabric-api-", "Fabric API"),
            ("sodium-", "Sodium"), ("lithium-", "Lithium"),
            ("voicechat-", "Simple Voice Chat"), ("immediatelyfast-", "ImmediatelyFast"),
            ("entityculling-", "Entity Culling"), ("ferritecore-", "FerriteCore"),
            ("modmenu-", "Mod Menu"), ("placeholder-api-", "Placeholder API") })
            if (fileName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)) return name;
        var nameWithoutExtension = fileName.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase)
            ? fileName[..^".jar.disabled".Length]
            : Path.GetFileNameWithoutExtension(fileName);
        return nameWithoutExtension;
    }

    private static void SyncBundledManagedModsToInstance(string modsDir)
    {
        var bundledModsDir = Path.Combine(AppContext.BaseDirectory, "payload", "managed-mods");
        var bundledManifest = Path.Combine(bundledModsDir, "manifest.json");
        if (!File.Exists(bundledManifest)) return;

        using var manifestDoc = JsonDocument.Parse(File.ReadAllText(bundledManifest));
        foreach (var mod in ExpandedManagedMods)
        {
            if (!manifestDoc.RootElement.TryGetProperty(mod.Slug, out var bundledName)) continue;
            var fileName = bundledName.GetString();
            if (string.IsNullOrWhiteSpace(fileName)) continue;

            var bundled = Path.Combine(bundledModsDir, fileName);
            if (!File.Exists(bundled) || new FileInfo(bundled).Length == 0) continue;

            var target = Path.Combine(modsDir, fileName);
            if (Directory.EnumerateFiles(modsDir).Any(path => Path.GetFileName(path).StartsWith(mod.Prefix, StringComparison.OrdinalIgnoreCase)
                && path.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase))) continue;
            File.Copy(bundled, target, true);
            DeleteOtherModVersions(modsDir, mod.Prefix, target);
        }
    }

    // Quick Exp is opt-in because some PvP servers prohibit its behavior.
    // Keep the preference separate from the JAR so launcher updates cannot re-enable it.
    public bool IsQuickExpEnabled(string mazClientVersion)
    {
        return !File.Exists(Path.Combine(GetMazModsDirectory(mazClientVersion), QuickExpMarker));
    }

    public async Task SetQuickExpEnabledAsync(string mazClientVersion, bool enabled)
    {
        var gameDir = GetMazInstallationDirectory(mazClientVersion);
        var modsDir = GetMazModsDirectory(mazClientVersion);
        var marker = Path.Combine(modsDir, QuickExpMarker);
        if (enabled)
        {
            await EnsureModrinthModAsync(gameDir, QuickExpSlug, QuickExpPrefix, MinecraftVersion);
            if (File.Exists(marker)) File.Delete(marker);
        }
        else
        {
            await File.WriteAllTextAsync(marker, "disabled");
            RemoveQuickExpFiles(modsDir);
        }
    }

    private async Task SyncQuickExpAddonAsync(string gameDir, string minecraftVersion)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        if (File.Exists(Path.Combine(modsDir, QuickExpMarker)))
            RemoveQuickExpFiles(modsDir);
        else
            await EnsureModrinthModAsync(gameDir, QuickExpSlug, QuickExpPrefix, minecraftVersion);
    }

    private static void RemoveQuickExpFilesIfDisabled(string modsDir)
    {
        if (File.Exists(Path.Combine(modsDir, QuickExpMarker))) RemoveQuickExpFiles(modsDir);
    }

    private static void RemoveQuickExpFiles(string modsDir)
    {
        if (!Directory.Exists(modsDir)) return;
        foreach (var path in Directory.EnumerateFiles(modsDir))
        {
            var name = Path.GetFileName(path);
            if (name.StartsWith(QuickExpPrefix, StringComparison.OrdinalIgnoreCase)
                && (name.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)
                    || name.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase)))
                File.Delete(path);
        }
    }

    public bool IsRecordableEnabled(string mazClientVersion)
    {
        var modsDir = GetMazModsDirectory(mazClientVersion);
        return !File.Exists(Path.Combine(modsDir, RecordableMarker));
    }

    public async Task SetRecordableEnabledAsync(string mazClientVersion, bool enabled)
    {
        var gameDir = GetMazInstallationDirectory(mazClientVersion);
        var modsDir = GetMazModsDirectory(mazClientVersion);
        var marker = Path.Combine(modsDir, RecordableMarker);

        if (enabled)
        {
            await EnsureModrinthModAsync(gameDir, RecordableSlug, RecordablePrefix, MinecraftVersion);
            if (File.Exists(marker)) File.Delete(marker);
            return;
        }

        await File.WriteAllTextAsync(marker, "disabled");
        RemoveRecordableFiles(modsDir);
    }

    public void AddMod(string mazClientVersion, string sourcePath)
    {
        if (!File.Exists(sourcePath)) throw new FileNotFoundException("Mod JAR not found.", sourcePath);
        if (!sourcePath.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)) throw new InvalidOperationException("Only .jar mods can be added.");
        var target = Path.Combine(GetMazModsDirectory(mazClientVersion), Path.GetFileName(sourcePath));
        File.Copy(sourcePath, target, true);
    }

    public void ToggleMod(string mazClientVersion, string fileName)
    {
        if (fileName.StartsWith(QuickExpPrefix, StringComparison.OrdinalIgnoreCase)) throw new InvalidOperationException("Use the Quick Exp toggle to enable or disable this add-on.");
        if (IsEssentialMod(fileName)) throw new InvalidOperationException("MazClient and Fabric API are required to run this profile. Other mods can be toggled.");
        var dir = GetMazModsDirectory(mazClientVersion);
        var source = Path.Combine(dir, fileName);
        if (!File.Exists(source)) return;
        var target = fileName.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase)
            ? source[..^".disabled".Length]
            : source + ".disabled";
        File.Move(source, target, true);
    }

    public void RemoveMod(string mazClientVersion, string fileName)
    {
        if (IsManagedCoreMod(fileName)) throw new InvalidOperationException("Managed mods can be disabled with TOGGLE but cannot be removed because the launcher manages their versions.");
        var path = Path.Combine(GetMazModsDirectory(mazClientVersion), fileName);
        if (File.Exists(path)) File.Delete(path);
    }

    private static bool IsEssentialMod(string fileName) =>
        fileName.StartsWith("maz-client", StringComparison.OrdinalIgnoreCase)
        || fileName.StartsWith("fabric-api-", StringComparison.OrdinalIgnoreCase);

    private static bool IsManagedCoreMod(string fileName)
    {
        var name = fileName.ToLowerInvariant();
        return name.StartsWith("maz-client")
               || name.StartsWith("fabric-api-")
               || name.StartsWith("sodium-")
               || name.StartsWith("lithium-")
               || name.StartsWith("voicechat-")
               || name.StartsWith("immediatelyfast-")
               || name.StartsWith("entityculling-")
               || name.StartsWith("ferritecore-")
               || ExpandedManagedMods.Any(mod => name.StartsWith(mod.Prefix, StringComparison.OrdinalIgnoreCase));
    }

    private static bool HasManagedMod(string modsDir, string filePrefix)
    {
        if (!Directory.Exists(modsDir)) return false;
        return Directory.EnumerateFiles(modsDir)
            .Select(Path.GetFileName)
            .Where(name => name != null)
            .Any(name => name!.StartsWith(filePrefix, StringComparison.OrdinalIgnoreCase)
                         && name.EndsWith(".jar", StringComparison.OrdinalIgnoreCase));
    }

    private static string GetMazInstallationDirectory(string version) =>
        GetMazInstallationDirectory(version, MinecraftVersion);

    private static string GetMazInstallationDirectory(string version, string minecraftVersion) =>
        string.Equals(minecraftVersion, MinecraftVersion, StringComparison.OrdinalIgnoreCase)
            ? Path.Combine(DataRoot, "installations", "mazclient", SafeName(version))
            : Path.Combine(DataRoot, "installations", "mazclient", "mc-" + SafeName(minecraftVersion), SafeName(version));

    private static string SafeName(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        return new string(value.Select(ch => invalid.Contains(ch) ? '_' : ch).ToArray());
    }

    private static async Task<string> ReadMarkerAsync(string path)
    {
        if (!File.Exists(path)) return string.Empty;
        try { return (await File.ReadAllTextAsync(path)).Trim(); }
        catch { return string.Empty; }
    }

    private static (int MinimumRamMb, int MaximumRamMb) GetConfiguredMemory()
    {
        var settings = LauncherPreferences.Load();
        settings.NormalizeMemory();
        return (settings.MinimumRamMb, settings.MaximumRamMb);
    }

    private async Task PrepareVersionAsync(string gameDir, string version, MSession session, Action<string, int>? progress)
    {
        var launcher = new MinecraftLauncher(new MinecraftPath(gameDir));
        launcher.FileProgressChanged += (_, e) =>
        {
            var total = Math.Max(1, e.TotalTasks);
            var pct = 10 + (int)Math.Round((e.ProgressedTasks / (double)total) * 80.0);
            progress?.Invoke($"Caching {e.Name}...", Math.Clamp(pct, 10, 90));
        };
        var memory = GetConfiguredMemory();
        await launcher.InstallAndBuildProcessAsync(version, new MLaunchOption
        {
            Session = session,
            MaximumRamMb = memory.MaximumRamMb,
            MinimumRamMb = memory.MinimumRamMb
        });
    }

    private async Task LaunchAsync(string gameDir, string version, MSession session, Action<string, int>? progress)
    {
        var launcher = new MinecraftLauncher(new MinecraftPath(gameDir));
        launcher.FileProgressChanged += (_, e) =>
        {
            var total = Math.Max(1, e.TotalTasks);
            var pct = 40 + (int)Math.Round((e.ProgressedTasks / (double)total) * 55.0);
            progress?.Invoke($"Checking {e.Name}...", Math.Clamp(pct, 40, 95));
        };

        progress?.Invoke($"Checking Minecraft {version} cache...", 40);
        var memory = GetConfiguredMemory();
        var process = await launcher.InstallAndBuildProcessAsync(version, new MLaunchOption
        {
            Session = session,
            MaximumRamMb = memory.MaximumRamMb,
            MinimumRamMb = memory.MinimumRamMb
        });
        var launchLogPath = Path.Combine(gameDir, "mazlauncher-launch.log");
        var launchLog = new StringBuilder();
        void CaptureLaunchLine(string? line)
        {
            if (string.IsNullOrWhiteSpace(line)) return;
            lock (launchLog)
            {
                launchLog.AppendLine(line);
                if (launchLog.Length > 32_000)
                    launchLog.Remove(0, launchLog.Length - 32_000);
            }

            try { File.AppendAllText(launchLogPath, line + Environment.NewLine); }
            catch { }
        }

        // Some optional performance mods (notably zfastnoise-ocl) require Java 25.
        // CmlLib may select a bundled Java 21 runtime for Minecraft 1.21.11;
        // override it only when a Java 25+ installation can be verified.
        var java25 = FindJava25Executable();
        if (java25 == null)
            java25 = await InstallJava25RuntimeAsync();
        if (java25 != null)
            process.StartInfo.FileName = java25;
        else if (string.Equals(version, $"fabric-loader-{FabricLoaderVersion}-{CompatibilityMinecraftVersion}", StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("MazClient 1.21.11 performance mods require Java 25+. Install JDK 25 and restart MazLauncher, or disable zfastnoise-ocl.");

        process.StartInfo.UseShellExecute = false;
        process.StartInfo.CreateNoWindow = true;
        process.StartInfo.RedirectStandardOutput = true;
        process.StartInfo.RedirectStandardError = true;
        process.OutputDataReceived += (_, e) => CaptureLaunchLine(e.Data);
        process.ErrorDataReceived += (_, e) => CaptureLaunchLine(e.Data);

        progress?.Invoke($"Launching Minecraft with {memory.MinimumRamMb}–{memory.MaximumRamMb} MB RAM...", 100);
        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();

        // Do not tell the UI launch succeeded if Java/Fabric immediately dies.
        // Keep the actual Java/Fabric output so the launcher can show the real cause.
        await Task.Delay(5000);
        if (process.HasExited)
        {
            string details;
            lock (launchLog)
                details = launchLog.ToString().Trim();

            if (details.Length > 6_000)
                details = details[^6_000..];

            throw new InvalidOperationException(
                $"Minecraft exited immediately with code {process.ExitCode}.\n\n" +
                $"Launch log: {launchLogPath}\n\n" +
                (string.IsNullOrWhiteSpace(details)
                    ? "Minecraft produced no stdout/stderr before exiting."
                    : details));
        }
    }

    private static async Task<string?> InstallJava25RuntimeAsync()
    {
        // Use a private runtime: no administrator privileges or global JAVA_HOME changes.
        var runtimeRoot = Path.Combine(DataRoot, "runtime", "jdk-25");
        var javaw = Path.Combine(runtimeRoot, "bin", "javaw.exe");
        if (File.Exists(javaw)) return javaw;

        var archive = Path.Combine(Path.GetTempPath(), "mazlauncher-jdk25-" + Guid.NewGuid().ToString("N") + ".zip");
        var staging = runtimeRoot + ".install-" + Guid.NewGuid().ToString("N");
        try
        {
            Directory.CreateDirectory(staging);
            using var client = new HttpClient { Timeout = TimeSpan.FromMinutes(5) };
            client.DefaultRequestHeaders.UserAgent.ParseAdd("MazLauncher/0.6.80");
            using var response = await client.GetAsync(
                "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse",
                HttpCompletionOption.ResponseHeadersRead);
            response.EnsureSuccessStatusCode();
            await using (var source = await response.Content.ReadAsStreamAsync())
            await using (var target = File.Create(archive))
                await source.CopyToAsync(target);

            System.IO.Compression.ZipFile.ExtractToDirectory(archive, staging);
            var extracted = Directory.EnumerateFiles(staging, "javaw.exe", SearchOption.AllDirectories)
                .FirstOrDefault(p => string.Equals(Path.GetFileName(Path.GetDirectoryName(p)), "bin", StringComparison.OrdinalIgnoreCase));
            if (extracted == null) throw new InvalidDataException("Downloaded JDK 25 has no javaw.exe.");
            var home = Directory.GetParent(Path.GetDirectoryName(extracted)!)!.FullName;
            Directory.CreateDirectory(Path.GetDirectoryName(runtimeRoot)!);
            if (!Directory.Exists(runtimeRoot)) Directory.Move(home, runtimeRoot);
            if (!File.Exists(javaw)) throw new InvalidDataException("JDK 25 installation was incomplete.");
            return javaw;
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException("Could not install the required Java 25 runtime automatically. Check your internet connection or install JDK 25 manually. " + ex.Message, ex);
        }
        finally
        {
            try { if (File.Exists(archive)) File.Delete(archive); } catch (IOException) { }
            try { if (Directory.Exists(staging)) Directory.Delete(staging, true); } catch (IOException) { }
        }
    }

    private static string? FindJava25Executable()
    {
        var candidates = new List<string>();
        foreach (var variable in new[] { "JAVA_HOME", "JDK_HOME" })
        {
            var home = Environment.GetEnvironmentVariable(variable);
            if (!string.IsNullOrWhiteSpace(home))
                candidates.Add(Path.Combine(home, "bin", "javaw.exe"));
        }
        foreach (var root in new[] {
            Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
            Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86)
        })
        {
            foreach (var vendor in new[] { "Java", "Eclipse Adoptium", "Microsoft", "Amazon Corretto", "Zulu" })
            {
                var directory = Path.Combine(root, vendor);
                if (!Directory.Exists(directory)) continue;
                try
                {
                    foreach (var jdk in Directory.EnumerateDirectories(directory))
                        candidates.Add(Path.Combine(jdk, "bin", "javaw.exe"));
                }
                catch (IOException) { }
                catch (UnauthorizedAccessException) { }
            }
        }
        foreach (var candidate in candidates.Distinct(StringComparer.OrdinalIgnoreCase))
        {
            if (!File.Exists(candidate)) continue;
            var java = Path.Combine(Path.GetDirectoryName(candidate)!, "java.exe");
            try
            {
                using var process = new System.Diagnostics.Process();
                process.StartInfo = new System.Diagnostics.ProcessStartInfo(java, "-version")
                {
                    UseShellExecute = false,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };
                if (!process.Start()) continue;
                var output = process.StandardError.ReadToEnd();
                if (!process.WaitForExit(3000)) { process.Kill(); continue; }
                var match = System.Text.RegularExpressions.Regex.Match(output, @"version ""(?<major>\d+)");
                if (match.Success && int.TryParse(match.Groups["major"].Value, out var major) && major >= 25)
                    return candidate;
            }
            catch (Exception) { /* Try the next installed JDK. */ }
        }
        return null;
    }

    private async Task EnsureFabricProfileAsync(string gameDir, string minecraftVersion)
    {
        var versionId = $"fabric-loader-{FabricLoaderVersion}-{minecraftVersion}";
        var versionDir = Path.Combine(gameDir, "versions", versionId);
        var jsonPath = Path.Combine(versionDir, versionId + ".json");
        if (File.Exists(jsonPath)) return;
        Directory.CreateDirectory(versionDir);
        var url = $"https://meta.fabricmc.net/v2/versions/loader/{minecraftVersion}/{FabricLoaderVersion}/profile/json";
        var json = await http.GetStringAsync(url);
        await File.WriteAllTextAsync(jsonPath, json);
    }

    private Task EnsureMazClientVersionAsync(string gameDir, string version, Action<string, int>? progress) =>
        EnsureMazClientVersionAsync(gameDir, version, MinecraftVersion, progress);

    private async Task EnsureMazClientVersionAsync(string gameDir, string version, string minecraftVersion, Action<string, int>? progress)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        var target = Path.Combine(modsDir, "maz-client.jar");
        var marker = Path.Combine(modsDir, ".mazclient-version");
        var expectedMarker = string.Equals(minecraftVersion, CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase)
            ? $"{version}|{CompatibilityMazClientBuildId}"
            : version;
        if (File.Exists(target) && File.Exists(marker) && string.Equals((await File.ReadAllTextAsync(marker)).Trim(), expectedMarker, StringComparison.OrdinalIgnoreCase)) return;

        if (string.Equals(minecraftVersion, CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
        {
            progress?.Invoke($"Downloading MazClient {version} for Minecraft {minecraftVersion}...", 35);
            var compatibilityTag = $"mc-{CompatibilityMinecraftVersion}-v{CompatibilityMazClientVersion}";
            var compatibilityJar = $"maz-client-{CompatibilityMinecraftVersion}-{CompatibilityMazClientVersion}.jar";
            var compatibilityUrl = $"https://github.com/km7bnv/maz-client/releases/download/{compatibilityTag}/{compatibilityJar}";
            var compatibilityTemp = target + "." + Guid.NewGuid().ToString("N") + ".download";
            try
            {
                using var response = await http.GetAsync(compatibilityUrl, HttpCompletionOption.ResponseHeadersRead);
                if (!response.IsSuccessStatusCode)
                    throw new InvalidOperationException($"MazClient's Minecraft {minecraftVersion} compatibility build is not published yet.");

                await using (var source = await response.Content.ReadAsStreamAsync())
                await using (var destination = new FileStream(compatibilityTemp, FileMode.CreateNew, FileAccess.Write, FileShare.None))
                {
                    await source.CopyToAsync(destination);
                    await destination.FlushAsync();
                }

                if (new FileInfo(compatibilityTemp).Length < 10_000)
                    throw new InvalidDataException("Downloaded MazClient compatibility JAR is unexpectedly small.");

                File.Move(compatibilityTemp, target, true);
                await File.WriteAllTextAsync(marker, expectedMarker);
                return;
            }
            finally
            {
                if (File.Exists(compatibilityTemp)) File.Delete(compatibilityTemp);
            }
        }

        var manifest = await GetCloudManifestAsync();
        if (manifest != null && string.Equals(version, manifest.MazClientVersion, StringComparison.OrdinalIgnoreCase))
        {
            progress?.Invoke($"Downloading MazClient {version} from cloud...", 35);
            var installedVersion = await cloudUpdates.EnsureMazClientCurrentAsync(gameDir, progress);
            if (!string.Equals(installedVersion, version, StringComparison.OrdinalIgnoreCase) || !File.Exists(target))
                throw new InvalidOperationException($"MazClient {version} could not be downloaded from the cloud package channel.");

            await File.WriteAllTextAsync(marker, version);
            return;
        }

        progress?.Invoke($"Downloading legacy MazClient {version}...", 35);
        var url = $"https://github.com/km7bnv/maz-client/releases/download/v{version}/maz-client-{version}.jar";
        var temp = target + "." + Guid.NewGuid().ToString("N") + ".download";
        try
        {
            using var response = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
            if (!response.IsSuccessStatusCode)
                throw new InvalidOperationException($"MazClient {version} is not available from the legacy package source. Choose the latest MazClient version or reinstall/update MazLauncher.");

            await using (var source = await response.Content.ReadAsStreamAsync())
            await using (var destination = new FileStream(temp, FileMode.CreateNew, FileAccess.Write, FileShare.None))
            {
                await source.CopyToAsync(destination);
                await destination.FlushAsync();
            }
            if (new FileInfo(temp).Length < 10_000) throw new InvalidDataException("Downloaded MazClient JAR is unexpectedly small.");
            File.Move(temp, target, true);
            await File.WriteAllTextAsync(marker, version);
        }
        finally
        {
            if (File.Exists(temp)) File.Delete(temp);
        }
    }

    private async Task SyncRecordableAddonAsync(string gameDir, string minecraftVersion)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        var marker = Path.Combine(modsDir, RecordableMarker);

        if (File.Exists(marker))
            RemoveRecordableFiles(modsDir);
        else
            await EnsureModrinthModAsync(gameDir, RecordableSlug, RecordablePrefix, minecraftVersion);
    }

    private static void RemoveRecordableFiles(string modsDir)
    {
        if (!Directory.Exists(modsDir)) return;
        foreach (var path in Directory.EnumerateFiles(modsDir)
                     .Where(path => {
                         var name = Path.GetFileName(path);
                         return name.StartsWith(RecordablePrefix, StringComparison.OrdinalIgnoreCase)
                                && (name.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)
                                    || name.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase));
                     }))
            File.Delete(path);
    }

    private async Task EnsureExpandedManagedModsAsync(string gameDir, string minecraftVersion)
    {
        var installedFiles = new List<(string Slug, string FileName)>();
        var failures = new List<string>();

        foreach (var mod in ExpandedManagedMods)
        {
            try
            {
                var installed = await EnsureModrinthModAsync(gameDir, mod.Slug, mod.Prefix, minecraftVersion);
                installedFiles.Add((mod.Slug, Path.GetFileName(installed)));
            }
            catch (Exception ex)
            {
                failures.Add($"{mod.Slug}: {ex.Message}");
            }
        }

        var modsDir = Path.Combine(gameDir, "mods");
        foreach (var mod in ExpandedManagedMods)
        {
            var resolved = installedFiles.FirstOrDefault(item =>
                string.Equals(item.Slug, mod.Slug, StringComparison.OrdinalIgnoreCase));
            if (string.IsNullOrWhiteSpace(resolved.FileName) ||
                !File.Exists(Path.Combine(modsDir, resolved.FileName)))
            {
                if (!failures.Any(f => f.StartsWith(mod.Slug + ":", StringComparison.OrdinalIgnoreCase)))
                    failures.Add($"{mod.Slug}: compatible JAR was not present after installation");
            }
        }

        if (failures.Count > 0)
            throw new InvalidOperationException(
                "MazClient's required managed mod stack is incomplete for Minecraft " + minecraftVersion +
                ". Missing/failed mods: " + string.Join(" | ", failures));
    }

    private async Task TryEnsureCompatibilityModAsync(
        string gameDir,
        string projectSlug,
        string filePrefix,
        string minecraftVersion,
        Action<string, int>? progress)
    {
        try
        {
            await EnsureModrinthModAsync(gameDir, projectSlug, filePrefix, minecraftVersion);
        }
        catch (Exception ex)
        {
            // Compatibility mods are performance/QoL extras. Never let a transient
            // Modrinth/CDN failure stop the proven MazClient 1.21.11 base from launching.
            progress?.Invoke($"Could not install {projectSlug} for {minecraftVersion}; continuing without it.", 58);
            try
            {
                var logPath = Path.Combine(gameDir, "mazlauncher-mod-download.log");
                await File.AppendAllTextAsync(logPath,
                    $"[{DateTimeOffset.UtcNow:O}] {projectSlug}: {ex.GetType().Name}: {ex.Message}{Environment.NewLine}");
            }
            catch { }
        }
    }

    private async Task<string> EnsureModrinthModAsync(string gameDir, string projectSlug, string filePrefix, string minecraftVersion)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);

        // The bundled payload is built for the launcher's primary Minecraft version
        // (currently 26.2). Never reuse those JARs for compatibility profiles such as
        // 1.21.11; doing so gives Fabric a folder full of wrong-version mods and causes
        // an immediate startup crash.
        var bundledModsDir = Path.Combine(AppContext.BaseDirectory, "payload", "managed-mods");
        var bundledManifest = Path.Combine(bundledModsDir, "manifest.json");
        if (string.Equals(minecraftVersion, MinecraftVersion, StringComparison.OrdinalIgnoreCase)
            && File.Exists(bundledManifest))
        {
            using var manifestDoc = JsonDocument.Parse(await File.ReadAllTextAsync(bundledManifest));
            if (manifestDoc.RootElement.TryGetProperty(projectSlug, out var bundledName))
            {
                var fileName = bundledName.GetString();
                if (!string.IsNullOrWhiteSpace(fileName))
                {
                    var bundled = Path.Combine(bundledModsDir, fileName);
                    if (!File.Exists(bundled) || new FileInfo(bundled).Length == 0)
                        throw new InvalidDataException($"Bundled managed mod {projectSlug} is missing or empty: {fileName}");
                    var bundledTarget = Path.Combine(modsDir, fileName);
                    var disabledBundled = Directory.EnumerateFiles(modsDir).FirstOrDefault(path =>
                        Path.GetFileName(path).StartsWith(filePrefix, StringComparison.OrdinalIgnoreCase)
                        && path.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase));
                    if (disabledBundled != null) return disabledBundled;
                    File.Copy(bundled, bundledTarget, true);
                    DeleteOtherModVersions(modsDir, filePrefix, bundledTarget);
                    if (!File.Exists(bundledTarget) || new FileInfo(bundledTarget).Length == 0)
                        throw new InvalidDataException($"Bundled managed mod {projectSlug} was not copied into the MazClient mods directory.");
                    return bundledTarget;
                }
            }
        }

        var disabledExisting = Directory.EnumerateFiles(modsDir)
            .FirstOrDefault(path => Path.GetFileName(path).StartsWith(filePrefix, StringComparison.OrdinalIgnoreCase)
                                    && path.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase));
        if (disabledExisting != null) return disabledExisting;
        var existing = Directory.EnumerateFiles(modsDir)
            .FirstOrDefault(path => Path.GetFileName(path).StartsWith(filePrefix, StringComparison.OrdinalIgnoreCase)
                                    && path.EndsWith(".jar", StringComparison.OrdinalIgnoreCase));
        try
        {
            var query = $"https://api.modrinth.com/v2/project/{projectSlug}/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22{minecraftVersion}%22%5D";
            using var stream = await http.GetStreamAsync(query);
            using var doc = await JsonDocument.ParseAsync(stream);
            var versions = doc.RootElement;
            JsonElement? selectedVersion = null;
            foreach (var version in versions.EnumerateArray())
            {
                if (version.TryGetProperty("version_type", out var type) && string.Equals(type.GetString(), "release", StringComparison.OrdinalIgnoreCase)) { selectedVersion = version; break; }
            }
            if (selectedVersion == null) throw new InvalidOperationException($"No release build of {projectSlug} was found for Minecraft {minecraftVersion} on Fabric.");
            var files = selectedVersion.Value.GetProperty("files");
            JsonElement selectedFile = files[0];
            foreach (var file in files.EnumerateArray()) if (file.TryGetProperty("primary", out var primary) && primary.GetBoolean()) { selectedFile = file; break; }
            var downloadUrl = selectedFile.GetProperty("url").GetString()!;
            var metadataSeparator = downloadUrl.Contains('?', StringComparison.Ordinal) ? "&" : "?";
            downloadUrl += metadataSeparator
                + "mr_download_reason=standalone"
                + "&mr_game_version=" + Uri.EscapeDataString(minecraftVersion)
                + "&mr_loader=fabric";
            var fileName = selectedFile.GetProperty("filename").GetString()!;
            var target = Path.Combine(modsDir, fileName);
            if (File.Exists(target)) { DeleteOtherModVersions(modsDir, filePrefix, target); return target; }

            var versionId = selectedVersion.Value.GetProperty("id").GetString();
            var projectId = selectedVersion.Value.GetProperty("project_id").GetString();
            var temp = target + "." + Guid.NewGuid().ToString("N") + ".download";
            try
            {
                try
                {
                    await using var source = await http.GetStreamAsync(downloadUrl);
                    await using var destination = new FileStream(temp, FileMode.CreateNew, FileAccess.Write, FileShare.None);
                    await source.CopyToAsync(destination);
                    await destination.FlushAsync();
                }
                catch (HttpRequestException)
                {
                    if (string.IsNullOrWhiteSpace(versionId) || string.IsNullOrWhiteSpace(projectId))
                        throw;

                    // Modrinth exposes the same version through its Maven endpoint.
                    // Use it as a CDN fallback when a CDN edge returns an HTTP error.
                    var mavenUrl =
                        $"https://api.modrinth.com/maven/maven/modrinth/{projectId}/{versionId}/{projectId}-{versionId}.jar";

                    await using var source = await http.GetStreamAsync(mavenUrl);
                    await using var destination = new FileStream(temp, FileMode.CreateNew, FileAccess.Write, FileShare.None);
                    await source.CopyToAsync(destination);
                    await destination.FlushAsync();
                }

                File.Move(temp, target, true);
            }
            finally { if (File.Exists(temp)) File.Delete(temp); }

            DeleteOtherModVersions(modsDir, filePrefix, target);
            if (!File.Exists(target) || new FileInfo(target).Length == 0)
                throw new InvalidDataException($"{projectSlug} download did not produce a usable JAR.");
            return target;
        }
        catch when (existing != null && File.Exists(existing))
        {
            // Offline: keep the last successfully cached managed mod instead of blocking launch.
            return existing;
        }
    }

    private static async Task WriteVersionCacheAsync(string path, IEnumerable<string> versions)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        await File.WriteAllLinesAsync(path, versions.Where(v => !string.IsNullOrWhiteSpace(v)));
    }

    private static IReadOnlyList<string> ReadVersionCache(string path)
    {
        if (!File.Exists(path)) return Array.Empty<string>();
        return File.ReadAllLines(path)
            .Select(v => v.Trim())
            .Where(v => !string.IsNullOrWhiteSpace(v))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    private static void DeleteOtherModVersions(string modsDir, string filePrefix, string keepPath)
    {
        foreach (var existing in Directory.EnumerateFiles(modsDir)
                     .Where(path => Path.GetFileName(path).StartsWith(filePrefix, StringComparison.OrdinalIgnoreCase)
                                    && path.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)))
            if (!string.Equals(existing, keepPath, StringComparison.OrdinalIgnoreCase)) File.Delete(existing);
    }

    private static void CleanupCompatibilityMods(string gameDir)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);

        // Remove managed JARs that may have been copied by an older launcher build
        // before 1.21.11 had strict version isolation. The correct 1.21.11 builds are
        // downloaded again immediately after this cleanup.
        var managedPrefixes = new[]
        {
            "fabric-api-",
            "sodium-",
            "lithium-",
            "voicechat-",
            "ImmediatelyFast-",
            "entityculling-",
            "ferritecore-",
            "cloth-config-",
            "placeholder-api-",
            "modmenu-",
            "record-able"
        };

        foreach (var mod in ExpandedManagedMods)
            managedPrefixes = managedPrefixes.Append(mod.Prefix).ToArray();

        foreach (var path in Directory.EnumerateFiles(modsDir))
        {
            var name = Path.GetFileName(path);
            if (managedPrefixes.Any(prefix => name.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                && (name.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)
                    || name.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase)))
            {
                File.Delete(path);
            }
        }
    }

    private static void CleanupOldMazClientJars(string gameDir)
    {
        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        var target = Path.Combine(modsDir, "maz-client.jar");
        foreach (var existing in Directory.EnumerateFiles(modsDir, "maz-client-*.jar"))
            if (!string.Equals(existing, target, StringComparison.OrdinalIgnoreCase)) File.Delete(existing);

        if (!File.Exists(Path.Combine(modsDir, RecordableMarker)))
            RemoveRecordableFiles(modsDir);
    }
}

using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Reflection;
using System.Security.Cryptography;
using System.Text.Json;
using System.Threading;

namespace MazLauncher;

public sealed class CloudUpdateService
{
    private const string ManifestUrl = "https://github.com/km7bnv/maz-client/releases/download/cloud/latest.json";
    private const string MazClientJarUrl = "https://github.com/km7bnv/maz-client/releases/download/cloud/maz-client.jar";
    private const string ReleaseApiBaseUrl = "https://api.github.com/repos/km7bnv/maz-client/releases/tags";
    private static readonly TimeSpan ManifestMemoryTtl = TimeSpan.FromSeconds(30);

    private static readonly string CacheDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "MazLauncher",
        "cache"
    );
    private static readonly string ManifestCachePath = Path.Combine(CacheDir, "latest-cloud-manifest.json");

    private readonly HttpClient http = new();
    private readonly SemaphoreSlim manifestGate = new(1, 1);
    private CloudManifest? memoryManifest;
    private DateTimeOffset memoryManifestExpiresUtc = DateTimeOffset.MinValue;

    public CloudUpdateService()
    {
        Directory.CreateDirectory(CacheDir);
        http.Timeout = TimeSpan.FromSeconds(5);
        http.DefaultRequestHeaders.UserAgent.ParseAdd($"MazLauncher/{CurrentLauncherVersion}");
        http.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");
    }

    public async Task<CloudManifest?> GetManifestAsync()
    {
        var now = DateTimeOffset.UtcNow;
        if (memoryManifest != null && now < memoryManifestExpiresUtc)
            return memoryManifest;

        await manifestGate.WaitAsync();
        try
        {
            now = DateTimeOffset.UtcNow;
            if (memoryManifest != null && now < memoryManifestExpiresUtc)
                return memoryManifest;

            try
            {
                var json = await http.GetStringAsync(ManifestUrl);
                var manifest = DeserializeManifest(json);
                if (manifest != null)
                {
                    await WriteManifestCacheAtomicallyAsync(json);
                    RememberManifest(manifest);
                }
                return manifest;
            }
            catch
            {
                try
                {
                    if (!File.Exists(ManifestCachePath)) return null;
                    var cachedJson = await File.ReadAllTextAsync(ManifestCachePath);
                    var cachedManifest = DeserializeManifest(cachedJson);
                    if (cachedManifest != null)
                        RememberManifest(cachedManifest);
                    return cachedManifest;
                }
                catch
                {
                    return null;
                }
            }
        }
        finally
        {
            manifestGate.Release();
        }
    }

    private void RememberManifest(CloudManifest manifest)
    {
        memoryManifest = manifest;
        memoryManifestExpiresUtc = DateTimeOffset.UtcNow + ManifestMemoryTtl;
    }

    private static CloudManifest? DeserializeManifest(string json)
    {
        return JsonSerializer.Deserialize<CloudManifest>(json, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });
    }

    private static async Task WriteManifestCacheAtomicallyAsync(string json)
    {
        Directory.CreateDirectory(CacheDir);
        var tempPath = ManifestCachePath + "." + Guid.NewGuid().ToString("N") + ".tmp";
        try
        {
            await File.WriteAllTextAsync(tempPath, json);
            File.Move(tempPath, ManifestCachePath, true);
        }
        finally
        {
            if (File.Exists(tempPath)) File.Delete(tempPath);
        }
    }

    public static string CurrentLauncherVersion =>
        Assembly.GetExecutingAssembly().GetName().Version?.ToString(3) ?? "0.0.0";

    public async Task<bool> HasLauncherUpdateAsync()
    {
        var manifest = await GetManifestAsync();
        if (manifest == null || !Version.TryParse(manifest.LauncherVersion, out var remote))
            return false;

        return remote > Version.Parse(CurrentLauncherVersion);
    }

    public async Task<string?> EnsureMazClientCurrentAsync(string gameDir, Action<string, int>? progress = null)
    {
        var manifest = await GetManifestAsync();
        if (manifest == null || string.IsNullOrWhiteSpace(manifest.MazClientVersion))
            return null;

        var modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        Directory.CreateDirectory(CacheDir);

        var targetJar = Path.Combine(modsDir, "maz-client.jar");
        var versionMarker = Path.Combine(CacheDir, "maz-client-version.txt");
        var installedVersion = File.Exists(versionMarker) ? (await File.ReadAllTextAsync(versionMarker)).Trim() : string.Empty;

        var expectedHash = manifest.MazClientSha256?.Trim().ToLowerInvariant() ?? string.Empty;
        var hashMatches = File.Exists(targetJar) && !string.IsNullOrWhiteSpace(expectedHash) &&
                          string.Equals(await ComputeSha256Async(targetJar), expectedHash, StringComparison.OrdinalIgnoreCase);

        if (File.Exists(targetJar) && installedVersion == manifest.MazClientVersion && hashMatches)
            return manifest.MazClientVersion;

        progress?.Invoke($"Updating MazClient to {manifest.MazClientVersion}...", 30);
        var tempJar = targetJar + ".download";
        try
        {
            await using (var source = await http.GetStreamAsync(MazClientJarUrl))
            await using (var destination = File.Create(tempJar))
                await source.CopyToAsync(destination);

            if (!string.IsNullOrWhiteSpace(expectedHash))
            {
                var downloadedHash = await ComputeSha256Async(tempJar);
                if (!string.Equals(downloadedHash, expectedHash, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException("MazClient cloud download failed SHA256 verification.");
            }

            File.Move(tempJar, targetJar, true);
            await File.WriteAllTextAsync(versionMarker, manifest.MazClientVersion);
            return manifest.MazClientVersion;
        }
        finally
        {
            if (File.Exists(tempJar)) File.Delete(tempJar);
        }
    }

    private static async Task<string> ComputeSha256Async(string path)
    {
        await using var stream = File.OpenRead(path);
        var hash = await SHA256.HashDataAsync(stream);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private async Task<(string Name, string Url, string Sha256)> ResolveLauncherInstallerAsync(string launcherVersion)
    {
        // launcher-v<version> is the single authoritative release tag since MazLauncher 0.6.14.
        // Keep a legacy lookup only as a read-only fallback for historical releases; never
        // require the removed mazlauncher-v publisher for current updates.
        var release = await GetLauncherReleaseAsync($"launcher-v{launcherVersion}")
                      ?? await GetLauncherReleaseAsync($"mazlauncher-v{launcherVersion}");
        if (release == null)
            throw new InvalidOperationException($"MazLauncher {launcherVersion} release is not available yet.");

        using (release)
        {
            var doc = release.Document;
            if (!doc.RootElement.TryGetProperty("assets", out var assets))
                throw new InvalidDataException($"MazLauncher {launcherVersion} release has no downloadable assets.");

            var expectedSuffix = $"-MazLauncher-{launcherVersion}-Setup.exe";
            foreach (var asset in assets.EnumerateArray())
            {
                var name = asset.TryGetProperty("name", out var nameElement) ? nameElement.GetString() : null;
                var url = asset.TryGetProperty("browser_download_url", out var urlElement) ? urlElement.GetString() : null;
                var digest = asset.TryGetProperty("digest", out var digestElement) ? digestElement.GetString() : null;
                if (string.IsNullOrWhiteSpace(name)
                    || string.IsNullOrWhiteSpace(url)
                    || !name.EndsWith(expectedSuffix, StringComparison.OrdinalIgnoreCase))
                    continue;

                const string sha256Prefix = "sha256:";
                if (string.IsNullOrWhiteSpace(digest) || !digest.StartsWith(sha256Prefix, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException($"MazLauncher {launcherVersion} installer is missing a GitHub SHA256 digest.");

                var sha256 = digest[sha256Prefix.Length..].Trim().ToLowerInvariant();
                if (sha256.Length != 64 || sha256.Any(c => !Uri.IsHexDigit(c)))
                    throw new InvalidDataException($"MazLauncher {launcherVersion} installer has an invalid GitHub SHA256 digest.");

                return (name, url, sha256);
            }
        }

        throw new InvalidDataException($"MazLauncher {launcherVersion} release does not contain the expected Windows installer.");
    }

    private async Task<ReleaseDocument?> GetLauncherReleaseAsync(string tag)
    {
        using var response = await http.GetAsync($"{ReleaseApiBaseUrl}/{tag}");
        if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
            return null;
        response.EnsureSuccessStatusCode();

        await using var stream = await response.Content.ReadAsStreamAsync();
        var document = await JsonDocument.ParseAsync(stream);
        return new ReleaseDocument(document);
    }

    private sealed class ReleaseDocument : IDisposable
    {
        public ReleaseDocument(JsonDocument document) => Document = document;
        public JsonDocument Document { get; }
        public void Dispose() => Document.Dispose();
    }

    public async Task DownloadAndApplyLauncherUpdateAsync()
    {
        var manifest = await GetManifestAsync();
        if (manifest == null)
            throw new InvalidOperationException("Could not reach the MazLauncher update server.");

        if (!Version.TryParse(manifest.LauncherVersion, out var remoteVersion))
            throw new InvalidDataException("The cloud manifest contains an invalid MazLauncher version.");

        if (remoteVersion <= Version.Parse(CurrentLauncherVersion))
            return;

        var installer = await ResolveLauncherInstallerAsync(manifest.LauncherVersion);
        var tempRoot = Path.Combine(Path.GetTempPath(), "MazLauncherInstaller-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(tempRoot);
        try
        {
            var installerPath = Path.Combine(tempRoot, installer.Name);

            await using (var source = await http.GetStreamAsync(installer.Url))
            await using (var destination = new FileStream(installerPath, FileMode.CreateNew, FileAccess.Write, FileShare.None))
            {
                await source.CopyToAsync(destination);
                await destination.FlushAsync();
            }

            if (!File.Exists(installerPath) || new FileInfo(installerPath).Length < 1024 * 1024)
                throw new InvalidDataException("Downloaded MazLauncher installer is missing or unexpectedly small.");

            var downloadedHash = await ComputeSha256Async(installerPath);
            if (!string.Equals(downloadedHash, installer.Sha256, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Downloaded MazLauncher installer failed GitHub SHA256 verification.");

            var process = Process.Start(new ProcessStartInfo
            {
                FileName = installerPath,
                Arguments = "/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS",
                UseShellExecute = true,
                WorkingDirectory = tempRoot
            });

            if (process == null)
                throw new InvalidOperationException("Could not start the downloaded MazLauncher installer.");

            process.Dispose();
            Environment.Exit(0);
        }
        catch
        {
            // Cleanup is best-effort so it cannot hide the original download or launch error.
            try
            {
                Directory.Delete(tempRoot, recursive: true);
            }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
            throw;
        }
    }
}

public sealed class CloudManifest
{
    public string MazClientVersion { get; set; } = "unknown";
    public string MazClientSha256 { get; set; } = string.Empty;
    public string LauncherVersion { get; set; } = "0.0.0";
}

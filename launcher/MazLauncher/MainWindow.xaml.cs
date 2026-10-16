using System.Diagnostics;
using System.Collections.ObjectModel;
using System.Text.Json;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using CmlLib.Core.Auth;
using Microsoft.Win32;

namespace MazLauncher;

public partial class MainWindow : Window
{
    private readonly LauncherService launcher = new();
    private readonly CloudUpdateService cloudUpdates = new();
    private readonly LauncherPreferences preferences = LauncherPreferences.Load();
    private readonly HttpClient uiHttp = new();
    private MSession? session;
    private readonly ObservableCollection<LauncherAccountChoice> accountChoices = new();
    private readonly string localAccountsPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "MazLauncher", "local-accounts.json");
    private bool applyingPreferences;

    public MainWindow()
    {
        InitializeComponent();
        uiHttp.DefaultRequestHeaders.UserAgent.ParseAdd($"MazLauncher/{CloudUpdateService.CurrentLauncherVersion}");
        LoadPreferencesIntoUi();
        AccountsList.ItemsSource = accountChoices;
        LoadLocalAccounts();
        ApplyPreferences();
        SetLaunchButtons(false);
        UpdateAccountControls();
        Loaded += MainWindow_Loaded;
    }

    private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        var changelogTask = LoadChangelogAsync();
        SetBusy(true, "Loading installations...");
        try { await LoadVersionListsAsync(); }
        catch (Exception ex) { StatusText.Text = "Version catalog partially unavailable"; Debug.WriteLine(ex); }

        if (preferences.CheckUpdatesOnStartup)
        {
            try { await launcher.CheckForLauncherUpdateAsync(); }
            catch (Exception ex) { Debug.WriteLine(ex); }
        }

        try
        {
            StatusText.Text = "Restoring cached session...";
            var restoredSessions = await launcher.TryRestoreSessionsAsync();
            foreach (var restored in restoredSessions)
                AddOnlineAccount(restored, false);

            if (accountChoices.Count > 0)
                AccountsList.SelectedItem = accountChoices.FirstOrDefault(a => a.IsOnline) ?? accountChoices[0];
            else
            {
                session = null;
                AccountText.Text = "No account selected";
                SetLaunchButtons(false);
                StatusText.Text = "Ready — add a local or Microsoft account in Accounts";
            }
            UpdateAccountControls();
        }
        catch (Exception ex) { StatusText.Text = "Ready"; Debug.WriteLine(ex); }
        finally { Progress.Value = 0; SetBusy(false); }

        await changelogTask;
    }

    private void SettingsMenuButton_Click(object sender, RoutedEventArgs e) => SettingsOverlay.Visibility = Visibility.Visible;
    private void CloseSettingsButton_Click(object sender, RoutedEventArgs e) => SettingsOverlay.Visibility = Visibility.Collapsed;

    private void LoadPreferencesIntoUi()
    {
        applyingPreferences = true;
        KeepLauncherOpenCheck.IsChecked = preferences.KeepLauncherOpen;
        CheckUpdatesOnStartupCheck.IsChecked = preferences.CheckUpdatesOnStartup;
        HighContrastCheck.IsChecked = preferences.HighContrast;
        LargeTextCheck.IsChecked = preferences.LargeText;
        StrongFocusCheck.IsChecked = preferences.StrongFocus;
        ReducedMotionCheck.IsChecked = preferences.ReducedMotion;
        applyingPreferences = false;
    }

    private void SettingsChanged(object sender, RoutedEventArgs e)
    {
        if (applyingPreferences) return;
        preferences.KeepLauncherOpen = KeepLauncherOpenCheck.IsChecked == true;
        preferences.CheckUpdatesOnStartup = CheckUpdatesOnStartupCheck.IsChecked == true;
        preferences.HighContrast = HighContrastCheck.IsChecked == true;
        preferences.LargeText = LargeTextCheck.IsChecked == true;
        preferences.StrongFocus = StrongFocusCheck.IsChecked == true;
        preferences.ReducedMotion = ReducedMotionCheck.IsChecked == true;
        preferences.Save();
        ApplyPreferences();
    }

    private async void ThemeToggleButton_Click(object sender, RoutedEventArgs e)
    {
        if (themeTransitionRunning) return;
        var targetLightTheme = !preferences.LightTheme;
        await AnimateThemeTransitionAsync(() =>
        {
            preferences.LightTheme = targetLightTheme;
            preferences.Save();
            ApplyPreferences();
        });
    }

    private void ApplyPreferences()
    {
        if (preferences.HighContrast)
        {
            SetBrush("Bg", Colors.Black);
            SetBrush("Surface", Colors.Black);
            SetBrush("Surface2", Color.FromRgb(15, 15, 15));
            SetBrush("SurfaceHover", Color.FromRgb(35, 35, 35));
            SetBrush("InputBg", Colors.Black);
            SetBrush("InputBorder", Colors.White);
            SetBrush("Border", Colors.White);
            SetBrush("Text", Colors.White);
            SetBrush("Muted", Colors.White);
            SetBrush("Placeholder", Color.FromRgb(220, 220, 220));
            SetBrush("Accent", Colors.Yellow);
            SetBrush("AccentHover", Color.FromRgb(255, 238, 0));
            SetBrush("AccentText", Colors.Black);
        }
        else if (preferences.LightTheme)
        {
            SetBrush("Bg", Color.FromRgb(241, 245, 249));
            SetBrush("Surface", Colors.White);
            SetBrush("Surface2", Color.FromRgb(226, 232, 240));
            SetBrush("SurfaceHover", Color.FromRgb(203, 213, 225));
            SetBrush("InputBg", Color.FromRgb(248, 250, 252));
            SetBrush("InputBorder", Color.FromRgb(148, 163, 184));
            SetBrush("Border", Color.FromRgb(203, 213, 225));
            SetBrush("Text", Color.FromRgb(15, 23, 42));
            SetBrush("Muted", Color.FromRgb(71, 85, 105));
            SetBrush("Placeholder", Color.FromRgb(100, 116, 139));
            SetBrush("Accent", Color.FromRgb(88, 101, 242));
            SetBrush("AccentHover", Color.FromRgb(71, 82, 196));
            SetBrush("AccentText", Colors.White);
        }
        else
        {
            SetBrush("Bg", Color.FromRgb(15, 23, 42));
            SetBrush("Surface", Color.FromRgb(30, 41, 59));
            SetBrush("Surface2", Color.FromRgb(38, 52, 73));
            SetBrush("SurfaceHover", Color.FromRgb(51, 65, 85));
            SetBrush("InputBg", Color.FromRgb(23, 32, 51));
            SetBrush("InputBorder", Color.FromRgb(71, 85, 105));
            SetBrush("Border", Color.FromRgb(51, 65, 85));
            SetBrush("Text", Color.FromRgb(248, 250, 252));
            SetBrush("Muted", Color.FromRgb(203, 213, 225));
            SetBrush("Placeholder", Color.FromRgb(148, 163, 184));
            SetBrush("Accent", Color.FromRgb(99, 102, 241));
            SetBrush("AccentHover", Color.FromRgb(129, 140, 248));
            SetBrush("AccentText", Colors.White);
        }

        Background = (Brush)Resources["Bg"];
        RootGrid.LayoutTransform = preferences.LargeText ? new ScaleTransform(1.06, 1.06) : Transform.Identity;
        SettingsMenuButton.BorderThickness = preferences.StrongFocus ? new Thickness(2) : new Thickness(1);
        ThemeToggleButton.Content = preferences.LightTheme ? "☾ DARK" : "☀ LIGHT";
        ThemeToggleButton.ToolTip = preferences.LightTheme ? "Switch to Dark launcher theme" : "Switch to Light launcher theme";
    }

    private void SetBrush(string key, Color color) => Resources[key] = new SolidColorBrush(color);

    private async Task LoadChangelogAsync()
    {
        try
        {
            ChangelogText.Text = await uiHttp.GetStringAsync("https://raw.githubusercontent.com/km7bnv/maz-client/main/CHANGELOG.md");
        }
        catch (Exception ex)
        {
            Debug.WriteLine(ex);
            ChangelogText.Text = "Could not load the changelog right now. Check your internet connection and try again later.";
        }
    }

    private async Task LoadVersionListsAsync()
    {
        var vanillaTask = launcher.GetVanillaVersionsAsync();
        var mazTask = launcher.GetMazClientVersionsAsync();
        await Task.WhenAll(vanillaTask, mazTask);
        var vanilla = await vanillaTask;
        VanillaVersionBox.ItemsSource = vanilla;
        VanillaVersionBox.SelectedItem = vanilla.FirstOrDefault(v => string.Equals(v, LauncherService.MinecraftVersion, StringComparison.OrdinalIgnoreCase)) ?? vanilla.FirstOrDefault();
        var maz = await mazTask;
        MazMinecraftVersionBox.ItemsSource = LauncherService.SupportedMazMinecraftVersions;
        MazMinecraftVersionBox.SelectedItem = LauncherService.MinecraftVersion;
        MazClientVersionBox.ItemsSource = maz;
        ModsMazVersionBox.ItemsSource = maz;
        MazClientVersionBox.SelectedItem = maz.FirstOrDefault();
        ModsMazVersionBox.SelectedItem = maz.FirstOrDefault();
        UpdateSelectedVersionLabels();
        RefreshMods();
    }

    private async Task CacheLatestOfflineAsync()
    {
        if (session == null) return;
        try
        {
            await launcher.WarmLatestOfflineCacheAsync(session, UpdateProgress);
            StatusText.Text = "Ready — latest Vanilla + MazClient cached for offline play";
        }
        catch (Exception ex)
        {
            Debug.WriteLine(ex);
            StatusText.Text = "Ready — offline cache will finish next time internet is available";
        }
    }

    private async void SignInButton_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            SetBusy(true, "Opening Microsoft sign-in...");
            session = await launcher.SignInAsync();
            AddOnlineAccount(session, true);
            StatusText.Text = $"Microsoft account {session.Username} added — ready to launch";
            Progress.Value = 0;
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Microsoft sign-in failed", MessageBoxButton.OK, MessageBoxImage.Error); StatusText.Text = "Sign-in failed"; }
        finally { SetBusy(false); }
    }

    private async void SignOutButton_Click(object sender, RoutedEventArgs e)
    {
        if (MessageBox.Show("Are you sure you want to sign out of MazLauncher? Your cached Microsoft account will be removed from this launcher.", "Sign out of MazLauncher?", MessageBoxButton.YesNo, MessageBoxImage.Question, MessageBoxResult.No) != MessageBoxResult.Yes) return;
        try
        {
            SetBusy(true, "Signing out...");
            await launcher.SignOutAsync();
            foreach (var online in accountChoices.Where(a => a.IsOnline).ToList())
                accountChoices.Remove(online);
            AccountsList.SelectedItem = accountChoices.FirstOrDefault(a => a.IsLocal);
            if (AccountsList.SelectedItem == null)
            {
                session = null;
                AccountText.Text = "No account selected";
                SetLaunchButtons(false);
            }
            UpdateAccountControls();
            StatusText.Text = "Microsoft accounts signed out";
            Progress.Value = 0;
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Could not sign out", MessageBoxButton.OK, MessageBoxImage.Error); StatusText.Text = "Sign-out failed"; }
        finally { SetBusy(false); }
    }

    private void AccountsList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (AccountsList?.SelectedItem is not LauncherAccountChoice choice) { UpdateAccountControls(); return; }
        session = choice.Session;
        AccountText.Text = choice.IsOnline ? $"{choice.Username} (Microsoft)" : $"{choice.Username} (Local)";
        SetLaunchButtons(session != null);
        UpdateAccountControls();
        StatusText.Text = $"Selected {choice.DisplayName}";
    }

    private void CreateLocalAccountButton_Click(object sender, RoutedEventArgs e)
    {
        var username = LocalAccountNameBox.Text.Trim();
        if (username.Length is < 3 or > 16 || username.Any(ch => !(char.IsAsciiLetterOrDigit(ch) || ch == '_')))
        {
            MessageBox.Show("Local usernames must be 3–16 characters and use only A–Z, a–z, 0–9, or underscores.", "Invalid local username", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        if (accountChoices.Any(a => a.IsLocal && string.Equals(a.Username, username, StringComparison.OrdinalIgnoreCase)))
        {
            MessageBox.Show("A local account with that username already exists.", "Account already exists", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        accountChoices.Add(new LauncherAccountChoice(username, true, CmlLib.Core.Auth.MSession.CreateOfflineSession(username)));
        SaveLocalAccounts();
        AccountsList.SelectedItem = accountChoices.Last();
        LocalAccountNameBox.Clear();
        StatusText.Text = $"Created local account {username}";
    }

    private void DeleteLocalAccountButton_Click(object sender, RoutedEventArgs e)
    {
        if (AccountsList.SelectedItem is not LauncherAccountChoice choice || !choice.IsLocal) return;
        if (MessageBox.Show($"Delete local account {choice.Username}? This removes the saved profile from MazLauncher, not Minecraft worlds or game files.",
                "Delete local account", MessageBoxButton.YesNo, MessageBoxImage.Warning) != MessageBoxResult.Yes) return;

        var remaining = accountChoices.FirstOrDefault(a => !ReferenceEquals(a, choice));
        accountChoices.Remove(choice);
        SaveLocalAccounts();
        AccountsList.SelectedItem = remaining;
        if (remaining == null)
        {
            session = null;
            AccountText.Text = "No account selected";
            SetLaunchButtons(false);
        }
        UpdateAccountControls();
        StatusText.Text = $"Deleted local account {choice.Username}";
    }

    private void LoadLocalAccounts()
    {
        try
        {
            if (!File.Exists(localAccountsPath)) return;
            var names = JsonSerializer.Deserialize<List<string>>(File.ReadAllText(localAccountsPath)) ?? new List<string>();
            foreach (var name in names.Where(IsValidLocalUsername).Distinct(StringComparer.OrdinalIgnoreCase))
                accountChoices.Add(new LauncherAccountChoice(name, true, CmlLib.Core.Auth.MSession.CreateOfflineSession(name)));
        }
        catch (Exception ex) { Debug.WriteLine($"Could not load local accounts: {ex.Message}"); }
    }

    private void SaveLocalAccounts()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(localAccountsPath)!);
            var names = accountChoices.Where(a => a.IsLocal).Select(a => a.Username).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
            File.WriteAllText(localAccountsPath, JsonSerializer.Serialize(names, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch (Exception ex) { MessageBox.Show($"Could not save local accounts: {ex.Message}", "Account storage error", MessageBoxButton.OK, MessageBoxImage.Warning); }
    }

    private static bool IsValidLocalUsername(string username) =>
        username.Length is >= 3 and <= 16 && username.All(ch => char.IsAsciiLetterOrDigit(ch) || ch == '_');

    private void AddOnlineAccount(MSession onlineSession, bool select)
    {
        var existing = accountChoices.FirstOrDefault(a => a.IsOnline && string.Equals(a.Username, onlineSession.Username, StringComparison.OrdinalIgnoreCase));
        if (existing != null)
        {
            existing.Session = onlineSession;
            if (select) AccountsList.SelectedItem = existing;
            return;
        }

        var choice = new LauncherAccountChoice(onlineSession.Username, false, onlineSession);
        accountChoices.Add(choice);
        if (select) AccountsList.SelectedItem = choice;
    }

    private sealed class LauncherAccountChoice
    {
        public string Username { get; }
        public bool IsLocal { get; }
        public bool IsOnline => !IsLocal;
        public string DisplayName => IsLocal ? $"{Username}  •  LOCAL" : $"{Username}  •  MICROSOFT";
        public MSession Session { get; set; }

        public LauncherAccountChoice(string username, bool isLocal, MSession session)
        {
            Username = username;
            IsLocal = isLocal;
            Session = session;
        }
    }

    private async void CheckUpdatesButton_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            SetBusy(true, "Checking for updates...");
            var manifest = await launcher.GetCloudManifestAsync();
            if (manifest == null) { StatusText.Text = "Could not reach the update server"; return; }
            if (await cloudUpdates.HasLauncherUpdateAsync())
            {
                StatusText.Text = $"MazLauncher {manifest.LauncherVersion} found — applying update...";
                await cloudUpdates.DownloadAndApplyLauncherUpdateAsync();
                return;
            }
            StatusText.Text = $"Up to date — MazLauncher {CloudUpdateService.CurrentLauncherVersion}, MazClient cloud {manifest.MazClientVersion}";
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Update check failed", MessageBoxButton.OK, MessageBoxImage.Error); StatusText.Text = "Update check failed"; }
        finally { SetBusy(false); }
    }

    private async void VanillaButton_Click(object sender, RoutedEventArgs e) => await LaunchSelectedVanillaAsync();
    private async void MazButton_Click(object sender, RoutedEventArgs e) => await LaunchSelectedMazAsync();
    private async void LaunchVanillaSelectedButton_Click(object sender, RoutedEventArgs e) => await LaunchSelectedVanillaAsync();
    private async void LaunchMazSelectedButton_Click(object sender, RoutedEventArgs e) => await LaunchSelectedMazAsync();

    private async Task LaunchSelectedVanillaAsync()
    {
        if (session == null) return;
        var version = VanillaVersionBox.SelectedItem as string;
        if (string.IsNullOrWhiteSpace(version)) { MessageBox.Show("Choose a Vanilla version first."); return; }
        await RunLaunchAsync(() => launcher.LaunchVanillaAsync(session, version, UpdateProgress), $"Vanilla {version}");
    }

    private async Task LaunchSelectedMazAsync()
    {
        if (session == null) return;
        var version = MazClientVersionBox.SelectedItem as string;
        var minecraftVersion = MazMinecraftVersionBox.SelectedItem as string;
        if (string.IsNullOrWhiteSpace(version)) { MessageBox.Show("Choose a MazClient version first."); return; }
        if (string.IsNullOrWhiteSpace(minecraftVersion)) { MessageBox.Show("Choose a Minecraft version for MazClient first."); return; }

        if (string.Equals(minecraftVersion, LauncherService.CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
        {
            version = LauncherService.CompatibilityMazClientVersion;
        }

        UpdateProgress("Preparing Low Fire, Low Shield, and Smaller Totem resource packs...", 41);
        await managedResourcePacks.EnsureForMazClientAsync(version, minecraftVersion, AddLauncherLog);

        await RunLaunchAsync(
            () => launcher.LaunchMazAsync(session, version, minecraftVersion, UpdateProgress),
            $"MazClient {version} • Minecraft {minecraftVersion}");
        RefreshMods();
    }

    private void VanillaVersionBox_SelectionChanged(object sender, SelectionChangedEventArgs e) => UpdateSelectedVersionLabels();
    private void MazClientVersionBox_SelectionChanged(object sender, SelectionChangedEventArgs e) => UpdateSelectedVersionLabels();
    private void MazMinecraftVersionBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (MazMinecraftVersionBox?.SelectedItem is string minecraftVersion &&
            string.Equals(minecraftVersion, LauncherService.CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase))
        {
            MazClientVersionBox.IsEnabled = false;
        }
        else if (MazClientVersionBox != null)
        {
            MazClientVersionBox.IsEnabled = true;
        }
        UpdateSelectedVersionLabels();
    }
    private void ModsMazVersionBox_SelectionChanged(object sender, SelectionChangedEventArgs e) => RefreshMods();

    private void UpdateSelectedVersionLabels()
    {
        if (VanillaSelectedText != null) VanillaSelectedText.Text = VanillaVersionBox?.SelectedItem is string vanilla ? $"Minecraft {vanilla}" : "Choose a version";
        if (MazVersionText != null)
        {
            var minecraftVersion = MazMinecraftVersionBox?.SelectedItem as string ?? LauncherService.MinecraftVersion;
            var mazVersion = string.Equals(minecraftVersion, LauncherService.CompatibilityMinecraftVersion, StringComparison.OrdinalIgnoreCase)
                ? LauncherService.CompatibilityMazClientVersion
                : MazClientVersionBox?.SelectedItem as string;
            MazVersionText.Text = !string.IsNullOrWhiteSpace(mazVersion)
                ? $"MazClient {mazVersion} • MC {minecraftVersion}"
                : $"Choose a version • MC {minecraftVersion}";
        }
    }

    private string? SelectedModsVersion() => ModsMazVersionBox.SelectedItem as string;
    private sealed record ModToggleEntry(string Name, string FileName, string State);

    private void RefreshMods()
    {
        if (ModToggleList == null || ModsMazVersionBox == null) return;
        var version = SelectedModsVersion();
        var mods = string.IsNullOrWhiteSpace(version) ? Array.Empty<string>() : launcher.GetInstalledMods(version).ToArray();
        var entries = mods.Select(file => new ModToggleEntry(
            launcher.GetModDisplayName(file), file,
            file.EndsWith(".jar.disabled", StringComparison.OrdinalIgnoreCase) ? "Disabled" : "Enabled")).ToList();
        if (!string.IsNullOrWhiteSpace(version))
        {
            if (!entries.Any(m => m.Name == "Quick Exp"))
                entries.Add(new ModToggleEntry("Quick Exp", "", launcher.IsQuickExpEnabled(version) ? "Enabled" : "Disabled"));
            if (!entries.Any(m => m.Name == "Record-able"))
                entries.Add(new ModToggleEntry("Record-able", "", launcher.IsRecordableEnabled(version) ? "Enabled" : "Disabled"));
        }
        ModToggleList.ItemsSource = entries.OrderBy(m => m.Name, StringComparer.OrdinalIgnoreCase).ToList();
    }

    private async void ModRowToggle_Click(object sender, RoutedEventArgs e)
    {
        var version = SelectedModsVersion();
        if (string.IsNullOrWhiteSpace(version) || sender is not Button { Tag: ModToggleEntry mod }) return;
        try
        {
            if (mod.Name == "Quick Exp")
                await launcher.SetQuickExpEnabledAsync(version, mod.State == "Disabled");
            else if (mod.Name == "Record-able")
                await launcher.SetRecordableEnabledAsync(version, mod.State == "Disabled");
            else
                launcher.ToggleMod(version, mod.FileName);
            RefreshMods();
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Mod toggle", MessageBoxButton.OK, MessageBoxImage.Warning); }
    }

        private void AddModButton_Click(object sender, RoutedEventArgs e)
    {
        var version = SelectedModsVersion(); if (string.IsNullOrWhiteSpace(version)) return;
        var picker = new OpenFileDialog { Filter = "Fabric mod JAR (*.jar)|*.jar", Multiselect = false, Title = $"Add mod to MazClient {version}" };
        if (picker.ShowDialog() != true) return;
        try { launcher.AddMod(version, picker.FileName); RefreshMods(); StatusText.Text = $"Added {Path.GetFileName(picker.FileName)} to MazClient {version}"; }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Could not add mod", MessageBoxButton.OK, MessageBoxImage.Error); }
    }
    private void OpenModsFolderButton_Click(object sender, RoutedEventArgs e) { var version = SelectedModsVersion(); if (!string.IsNullOrWhiteSpace(version)) Process.Start(new ProcessStartInfo(launcher.GetMazModsDirectory(version)) { UseShellExecute = true }); }
    private void OpenSkinManagerButton_Click(object sender, RoutedEventArgs e) => Process.Start(new ProcessStartInfo("https://www.minecraft.net/msaprofile/mygames/editskin") { UseShellExecute = true });

    private async Task RunLaunchAsync(Func<Task> launch, string mode)
    {
        try
        {
            SetBusy(true, $"Preparing {mode}...");
            await launch();
            StatusText.Text = $"{mode} launched";
            if (!preferences.KeepLauncherOpen) Close();
        }
        catch (Exception ex)
        {
            Debug.WriteLine(ex);
            MessageBox.Show(ex.Message, $"Could not launch {mode}", MessageBoxButton.OK, MessageBoxImage.Error);
            StatusText.Text = $"{mode} launch failed";
        }
        finally { SetBusy(false); }
    }

    private void UpdateProgress(string text, int percent) => Dispatcher.Invoke(() => { StatusText.Text = text; Progress.Value = Math.Clamp(percent, 0, 100); });
    private void SetBusy(bool busy, string? status = null)
    {
        SignInButton.IsEnabled = !busy; SignOutButton.IsEnabled = !busy; CheckUpdatesButton.IsEnabled = !busy; SettingsMenuButton.IsEnabled = !busy;
        if (AccountsList != null) AccountsList.IsEnabled = !busy;
        if (LocalAccountNameBox != null) LocalAccountNameBox.IsEnabled = !busy;
        if (CreateLocalAccountButton != null) CreateLocalAccountButton.IsEnabled = !busy;
        if (AccountsSignInButton != null) AccountsSignInButton.IsEnabled = !busy;
        ThemeToggleButton.IsEnabled = true;
        VanillaButton.IsEnabled = !busy && session != null; MazButton.IsEnabled = !busy && session != null; LaunchVanillaSelectedButton.IsEnabled = !busy && session != null; LaunchMazSelectedButton.IsEnabled = !busy && session != null;
        if (status != null) StatusText.Text = status;
    }
    private void SetLaunchButtons(bool enabled) { VanillaButton.IsEnabled = enabled; MazButton.IsEnabled = enabled; LaunchVanillaSelectedButton.IsEnabled = enabled; LaunchMazSelectedButton.IsEnabled = enabled; }
    private void UpdateAccountControls()
    {
        var onlineSelected = AccountsList?.SelectedItem is LauncherAccountChoice choice && choice.IsOnline;
        DeleteLocalAccountButton.IsEnabled = AccountsList?.SelectedItem is LauncherAccountChoice selected && selected.IsLocal;
        SignInButton.Visibility = onlineSelected ? Visibility.Collapsed : Visibility.Visible;
        SignOutButton.Visibility = accountChoices.Any(a => a.IsOnline) ? Visibility.Visible : Visibility.Collapsed;
    }
}

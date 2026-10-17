using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

namespace MazLauncher.Linux;

// Native Linux UI foundation. Minecraft launch and Microsoft authentication
// are intentionally not exposed until their Linux implementations are ready.
public sealed class MainWindow : Window
{
    private static readonly string DataDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "MazLauncher");
    private static readonly string AccountsFile = Path.Combine(DataDirectory, "local-accounts.json");
    private readonly List<string> accounts = new();
    private readonly ListBox accountList = new();
    private readonly TextBox username = new() { Watermark = "Minecraft username (3–16 characters)" };
    private readonly TextBlock status = new() { Text = "Linux launcher foundation — Minecraft launch coming next." };
    private readonly Button remove = new() { Content = "DELETE SELECTED", IsEnabled = false };

    public MainWindow()
    {
        Title = "MazLauncher — Linux";
        Width = 920;
        Height = 650;
        MinWidth = 560;
        MinHeight = 430;
        Background = new SolidColorBrush(Color.Parse("#0F172A"));
        LoadAccounts();

        var heading = new TextBlock
        {
            Text = "M  MazLauncher",
            FontSize = 30,
            FontWeight = FontWeight.Bold,
            Foreground = Brushes.White
        };
        var tabs = new TabControl();
        tabs.Items.Add(new TabItem
        {
            Header = "PLAY",
            Content = new StackPanel
            {
                Spacing = 14,
                Children =
                {
                    new TextBlock { Text = "MazClient for Linux", FontSize = 24, FontWeight = FontWeight.Bold },
                    new TextBlock { Text = "Minecraft 26.2  •  Minecraft 1.21.11" },
                    new TextBlock { Text = "Linux game installation and launch support is in development." },
                    new TextBlock { Text = "This initial build manages local launcher accounts; it does not launch Minecraft yet.", TextWrapping = TextWrapping.Wrap }
                }
            }
        });
        var create = new Button { Content = "CREATE LOCAL ACCOUNT" };
        create.Click += (_, _) => CreateAccount();
        remove.Click += (_, _) => RemoveAccount();
        accountList.SelectionChanged += (_, _) => remove.IsEnabled = accountList.SelectedItem is string;
        tabs.Items.Add(new TabItem
        {
            Header = "ACCOUNTS",
            Content = new StackPanel
            {
                Spacing = 12,
                Children =
                {
                    new TextBlock { Text = "Local accounts", FontSize = 22, FontWeight = FontWeight.Bold },
                    new TextBlock { Text = "Stored only on this computer. Microsoft sign-in is not implemented yet." },
                    accountList, username,
                    new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, Children = { create, remove } }
                }
            }
        });
        tabs.Items.Add(new TabItem
        {
            Header = "ABOUT",
            Content = new TextBlock
            {
                Text = "MazLauncher Linux 0.1.0 — Avalonia / .NET 8\nNative Linux UI foundation.\nGame launching, Microsoft login, mods and managed packs are planned.",
                TextWrapping = TextWrapping.Wrap
            }
        });

        Content = new Grid
        {
            RowDefinitions = new RowDefinitions("Auto,*,Auto"),
            Margin = new Thickness(24),
            Children =
            {
                heading,
                Place(tabs, 1),
                Place(status, 2)
            }
        };
        RefreshAccounts();
    }

    private static T Place<T>(T control, int row) where T : Control
    {
        Grid.SetRow(control, row);
        return control;
    }

    private void LoadAccounts()
    {
        try
        {
            if (File.Exists(AccountsFile))
                accounts.AddRange(JsonSerializer.Deserialize<List<string>>(File.ReadAllText(AccountsFile)) ?? new());
        }
        catch (Exception ex) { status.Text = "Could not read accounts: " + ex.Message; }
    }

    private void SaveAccounts()
    {
        Directory.CreateDirectory(DataDirectory);
        File.WriteAllText(AccountsFile, JsonSerializer.Serialize(accounts, new JsonSerializerOptions { WriteIndented = true }));
    }

    private void RefreshAccounts() => accountList.ItemsSource = accounts.ToArray();

    private void CreateAccount()
    {
        var name = username.Text?.Trim() ?? "";
        if (name.Length is < 3 or > 16 || !name.All(ch => char.IsAsciiLetterOrDigit(ch) || ch == '_'))
        {
            status.Text = "Username must be 3–16 letters, numbers, or underscores.";
            return;
        }
        if (accounts.Contains(name, StringComparer.OrdinalIgnoreCase))
        {
            status.Text = "That local account already exists.";
            return;
        }
        accounts.Add(name);
        try { SaveAccounts(); }
        catch (Exception ex) { accounts.Remove(name); status.Text = ex.Message; return; }
        RefreshAccounts();
        username.Text = "";
        status.Text = "Created local account " + name;
    }

    private void RemoveAccount()
    {
        if (accountList.SelectedItem is not string name) return;
        accounts.Remove(name);
        try { SaveAccounts(); }
        catch (Exception ex) { accounts.Add(name); status.Text = ex.Message; return; }
        RefreshAccounts();
        status.Text = "Deleted local account " + name;
    }
}

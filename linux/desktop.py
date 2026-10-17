"""MazLauncher Linux desktop UI. Requires PySide6.

Visual structure follows launcher/MazLauncher/MainWindow.xaml.
Unimplemented game operations are explicitly disabled rather than simulated.
"""
import json
import re
import sys
from pathlib import Path
from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QApplication, QCheckBox, QComboBox, QFileDialog, QFrame, QGridLayout,
    QHBoxLayout, QLabel, QLineEdit, QListWidget, QMainWindow, QMessageBox,
    QProgressBar, QPushButton, QScrollArea, QStackedWidget, QTabWidget,
    QTextEdit, QVBoxLayout, QWidget
)
from mazlauncher import config_dir, release_info

DARK = {"bg":"#0F172A","surface":"#1E293B","surface2":"#263449","hover":"#334155",
        "input":"#172033","border":"#334155","accent":"#6366F1","text":"#F8FAFC","muted":"#CBD5E1"}
LIGHT = {"bg":"#F8FAFC","surface":"#FFFFFF","surface2":"#E2E8F0","hover":"#CBD5E1",
         "input":"#F1F5F9","border":"#CBD5E1","accent":"#4F46E5","text":"#0F172A","muted":"#475569"}

def accounts_path():
    return config_dir() / "local-accounts.json"

def preferences_path():
    return config_dir() / "preferences.json"

def load_preferences():
    defaults = {"light_theme": False, "active_account": None,
                "keep_open": True, "check_updates": True,
                "high_contrast": False, "large_text": False,
                "strong_focus": False, "reduced_motion": False}
    try:
        stored = json.loads(preferences_path().read_text(encoding="utf-8"))
        if isinstance(stored, dict):
            for key in defaults:
                if key in stored and isinstance(stored[key], type(defaults[key])) and key != "active_account":
                    defaults[key] = stored[key]
            name = stored.get("active_account")
            if name is None or isinstance(name, str):
                defaults["active_account"] = name
    except (FileNotFoundError, ValueError, OSError, TypeError):
        pass
    return defaults

def save_preferences(prefs):
    path = preferences_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(prefs, indent=2), encoding="utf-8")
    tmp.replace(path)

def load_accounts():
    try:
        data = json.loads(accounts_path().read_text(encoding="utf-8"))
        return [x for x in data if isinstance(x, str) and re.fullmatch(r"[A-Za-z0-9_]{3,16}", x)]
    except (FileNotFoundError, ValueError, OSError, TypeError):
        return []

def save_accounts(names):
    path = accounts_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(names, indent=2), encoding="utf-8")
    tmp.replace(path)

class MazWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("MazLauncher")
        self.resize(920, 650)
        self.setMinimumSize(820, 590)
        self.light = False
        self.names = load_accounts()
        self.settings_open = False
        self.root = QWidget()
        self.setCentralWidget(self.root)
        outer = QVBoxLayout(self.root)
        outer.setContentsMargins(28, 22, 28, 22)
        outer.setSpacing(12)
        header = QHBoxLayout()
        logo = QLabel("M")
        logo.setObjectName("logo")
        logo.setAlignment(Qt.AlignCenter)
        logo.setFixedSize(48, 48)
        header.addWidget(logo)
        title = QLabel("MazLauncher")
        title.setObjectName("title")
        header.addWidget(title)
        header.addStretch()
        settings = QPushButton("☰")
        settings.setFixedSize(44, 44)
        settings.clicked.connect(self.toggle_settings)
        header.addWidget(settings)
        outer.addLayout(header)

        self.stack = QStackedWidget()
        outer.addWidget(self.stack, 1)
        self.tabs = QTabWidget()
        self.stack.addWidget(self.tabs)
        self.settings_page = QWidget()
        self.stack.addWidget(self.settings_page)
        self.build_tabs()
        self.build_settings()

        bottom = QHBoxLayout()
        updates = QPushButton("CHECK FOR UPDATES")
        updates.clicked.connect(self.check_updates)
        bottom.addWidget(updates)
        bottom.addStretch()
        self.theme_button = QPushButton("☀ LIGHT")
        self.theme_button.clicked.connect(self.toggle_theme)
        bottom.addWidget(self.theme_button)
        outer.addLayout(bottom)
        self.progress = QProgressBar()
        self.progress.setRange(0, 100)
        self.progress.setValue(0)
        self.progress.setFixedHeight(8)
        outer.addWidget(self.progress)
        self.status = QLabel("Ready — Linux preview; launching not implemented")
        outer.addWidget(self.status)
        self.apply_theme()

    def page(self, name):
        w = QWidget()
        layout = QVBoxLayout(w)
        layout.setContentsMargins(18, 16, 18, 18)
        layout.setSpacing(12)
        self.tabs.addTab(w, name)
        return layout

    def disabled_action(self, label, hint):
        b = QPushButton(label)
        b.setEnabled(False)
        b.setToolTip(hint)
        return b

    def build_tabs(self):
        play = self.page("PLAY")
        row = QHBoxLayout()
        row.addWidget(self.disabled_action("VANILLA\nUses selection from Installations", "Minecraft launch integration pending"))
        row.addWidget(self.disabled_action("MAZCLIENT\nVersion-isolated mods + configs", "Minecraft launch integration pending"))
        play.addLayout(row)
        play.addStretch()

        installations = self.page("INSTALLATIONS")
        installations.addWidget(QLabel("Vanilla installation"))
        r = QHBoxLayout()
        r.addWidget(QComboBox(), 1)
        r.addWidget(self.disabled_action("LAUNCH", "Installation backend pending"))
        installations.addLayout(r)
        installations.addWidget(QLabel("MazClient installation"))
        r = QHBoxLayout()
        version = QComboBox()
        version.addItems(["Minecraft 26.2", "Minecraft 1.21.11"])
        r.addWidget(version)
        r.addWidget(self.disabled_action("LAUNCH", "Installation backend pending"))
        installations.addLayout(r)
        installations.addStretch()

        mods = self.page("MODS")
        mods.addWidget(QLabel("MazClient version"))
        v = QComboBox()
        v.addItems(["Minecraft 26.2", "Minecraft 1.21.11"])
        mods.addWidget(v)
        mods.addWidget(QLabel("Managed mod controls require the Linux installation backend."))
        mods.addStretch()

        skins = self.page("SKINS")
        preview = QLabel("IMPORT A SKIN PNG")
        preview.setAlignment(Qt.AlignCenter)
        preview.setMinimumHeight(150)
        skins.addWidget(preview)
        skins.addWidget(QLabel("Skin model"))
        model = QComboBox()
        model.addItems(["Classic (Steve)", "Slim (Alex)"])
        skins.addWidget(model)
        r = QHBoxLayout()
        choose = QPushButton("IMPORT PNG")
        choose.clicked.connect(lambda: self.select_skin(preview))
        r.addWidget(choose)
        r.addWidget(self.disabled_action("APPLY SKIN", "Authenticated skin service pending"))
        skins.addLayout(r)
        skins.addStretch()

        accounts = self.page("ACCOUNTS")
        accounts.addWidget(QLabel("Local accounts"))
        self.account_list = QListWidget()
        accounts.addWidget(self.account_list)
        self.refresh_accounts()
        self.name_entry = QLineEdit()
        self.name_entry.setPlaceholderText("Minecraft username (3–16 characters)")
        accounts.addWidget(self.name_entry)
        add = QPushButton("CREATE LOCAL ACCOUNT")
        add.clicked.connect(self.add_account)
        accounts.addWidget(add)
        delete = QPushButton("DELETE SELECTED LOCAL ACCOUNT")
        delete.clicked.connect(self.delete_account)
        accounts.addWidget(delete)
        accounts.addWidget(self.disabled_action("ADD MICROSOFT ACCOUNT", "Microsoft OAuth integration pending"))

        changelog = self.page("CHANGELOG")
        self.changelog = QTextEdit()
        self.changelog.setReadOnly(True)
        self.changelog.setPlainText("MazLauncher Linux preview\n\nSee the repository changelog for released Windows functionality.")
        changelog.addWidget(self.changelog)

    def build_settings(self):
        l = QVBoxLayout(self.settings_page)
        l.addWidget(QLabel("SETTINGS"))
        for label in ["Keep launcher open after Minecraft starts",
                      "Check for launcher updates on startup", "High contrast",
                      "Larger launcher text", "Stronger keyboard focus indicators",
                      "Reduce motion and visual effects"]:
            l.addWidget(QCheckBox(label))
        l.addStretch()
        back = QPushButton("BACK")
        back.clicked.connect(self.toggle_settings)
        l.addWidget(back)

    def refresh_accounts(self):
        self.account_list.clear()
        self.account_list.addItems(self.names)

    def add_account(self):
        name = self.name_entry.text().strip()
        if not re.fullmatch(r"[A-Za-z0-9_]{3,16}", name):
            QMessageBox.warning(self, "Invalid name", "Use 3–16 letters, digits or underscores.")
            return
        if name.lower() in [n.lower() for n in self.names]:
            QMessageBox.warning(self, "Duplicate", "That local account already exists.")
            return
        self.names.append(name)
        save_accounts(self.names)
        self.refresh_accounts()
        self.name_entry.clear()
        self.status.setText("Local account created")

    def delete_account(self):
        item = self.account_list.currentItem()
        if not item:
            return
        if QMessageBox.question(self, "Delete local account", f"Delete {item.text()}?") != QMessageBox.Yes:
            return
        self.names.remove(item.text())
        save_accounts(self.names)
        self.refresh_accounts()
        self.status.setText("Local account deleted")

    def select_account(self, name):
        if name and name in self.names:
            self.preferences["active_account"] = name
            save_preferences(self.preferences)
            self.status.setText("Selected local account: " + name)

    def set_preference(self, key, value):
        self.preferences[key] = value
        save_preferences(self.preferences)
        if key in ("large_text", "high_contrast", "strong_focus"):
            self.apply_theme()

    def select_skin(self, preview):
        path, _ = QFileDialog.getOpenFileName(self, "Import skin", str(Path.home()), "PNG images (*.png)")
        if path:
            from PySide6.QtGui import QPixmap
            pix = QPixmap(path)
            if pix.isNull():
                QMessageBox.warning(self, "Invalid skin", "Cannot read this PNG.")
                return
            preview.setPixmap(pix.scaled(160, 160, Qt.KeepAspectRatio, Qt.FastTransformation))
            self.status.setText("Skin preview loaded; applying skins is not implemented")

    def check_updates(self):
        QApplication.setOverrideCursor(Qt.WaitCursor)
        try:
            info = release_info()
            self.status.setText("Latest GitHub release: " + info["tag"])
            QMessageBox.information(self, "Release information",
                                    f"Latest release: {info['tag']}\n{info['url']}\n\nAutomatic Linux updates are not implemented.")
        except Exception as exc:
            self.status.setText("Release lookup failed")
            QMessageBox.warning(self, "Update check", str(exc))
        finally:
            QApplication.restoreOverrideCursor()

    def toggle_settings(self):
        self.settings_open = not self.settings_open
        self.stack.setCurrentIndex(1 if self.settings_open else 0)

    def toggle_theme(self):
        self.light = not self.light
        self.theme_button.setText("☾ DARK" if self.light else "☀ LIGHT")
        self.apply_theme()

    def apply_theme(self):
        c = LIGHT if self.light else DARK
        self.setStyleSheet(f"""
            QWidget {{ background: {c['bg']}; color: {c['text']}; font-family: 'Segoe UI', 'Noto Sans', sans-serif; font-size: {font_size}px; }}
            QLabel#title {{ font-size: 30px; font-weight: 700; }}
            QLabel#logo {{ background: {c['accent']}; color: white; border-radius: 12px; font-size: 30px; font-weight: bold; }}
            QTabWidget::pane {{ border: none; background: {c['surface']}; }}
            QTabBar::tab {{ background: {c['input']}; color: {c['muted']}; padding: 10px 12px; margin-right: 6px; border-radius: 8px; font-weight: 600; }}
            QTabBar::tab:selected {{ background: {c['accent']}; color: white; }}
            QPushButton {{ background: {c['surface2']}; border: 1px solid {c['border']}; border-radius: 10px; padding: 9px 15px; font-weight: 600; }}
            QPushButton:hover:enabled {{ background: {c['hover']}; }}
            QPushButton:disabled {{ color: {c['muted']}; opacity: .5; }}
            QLineEdit, QComboBox, QListWidget, QTextEdit {{ background: {c['input']}; border: 1px solid {c['border']}; border-radius: 6px; padding: 8px; }}
            QProgressBar {{ background: {c['surface2']}; border: none; border-radius: 3px; }}
            QProgressBar::chunk {{ background: {c['accent']}; }}
        """)

def main():
    app = QApplication(sys.argv)
    window = MazWindow()
    window.show()
    return app.exec()

if __name__ == "__main__":
    raise SystemExit(main())

import os
import unittest
from unittest.mock import patch
import mazlauncher

class LinuxLauncherTests(unittest.TestCase):
    def test_xdg_data_directory(self):
        with patch.dict(os.environ, {"XDG_DATA_HOME": "/tmp/maz-test"}):
            self.assertEqual(str(mazlauncher.data_dir()), "/tmp/maz-test/mazlauncher")

    def test_xdg_config_directory(self):
        with patch.dict(os.environ, {"XDG_CONFIG_HOME": "/tmp/maz-config"}):
            self.assertEqual(str(mazlauncher.config_dir()), "/tmp/maz-config/mazlauncher")

    def test_release_endpoint_https(self):
        self.assertTrue(mazlauncher.API.startswith("https://api.github.com/"))

if __name__ == "__main__":
    unittest.main()

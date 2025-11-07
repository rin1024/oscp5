import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

////////////////////////////////////////////////////////////////////////////////
// 設定ファイルのロード
////////////////////////////////////////////////////////////////////////////////
void setupConfig() {
  try {
    Path configPath = Paths.get(dataPath("config.json"));
    if (Files.exists(configPath)) {
      config = loadJSONObject(dataPath("config.json"));
      MY_OSC_PORT = config.getInt("myOscPort", 54445);
      TARGET_OSC_IP_ADDRESS = config.getString("targetIpAddress", "localhost");
      TARGET_OSC_PORT = config.getInt("targetPort", 10000);
    } else {
      // デフォルト値でconfig.jsonを作成
      config = new JSONObject();
      config.setInt("myOscPort", MY_OSC_PORT);
      config.setString("targetIpAddress", TARGET_OSC_IP_ADDRESS);
      config.setInt("targetPort", TARGET_OSC_PORT);
      saveJSONObject(config, dataPath("config.json"));
    }
  } catch (Exception e) {
    println("Failed to load config: " + e);
    // デフォルト値を使用
    config = new JSONObject();
    config.setInt("myOscPort", MY_OSC_PORT);
    config.setString("targetIpAddress", TARGET_OSC_IP_ADDRESS);
    config.setInt("targetPort", TARGET_OSC_PORT);
  }
}

////////////////////////////////////////////////////////////////////////////////
// 設定ファイルの保存
////////////////////////////////////////////////////////////////////////////////
void saveConfig(int _myOscPort, String _targetIpAddress, int _targetPort) {
  try {
    MY_OSC_PORT = _myOscPort;
    TARGET_OSC_IP_ADDRESS = _targetIpAddress;
    TARGET_OSC_PORT = _targetPort;
    
    config.setInt("myOscPort", MY_OSC_PORT);
    config.setString("targetIpAddress", TARGET_OSC_IP_ADDRESS);
    config.setInt("targetPort", TARGET_OSC_PORT);
    
    saveJSONObject(config, dataPath("config.json"));
    
    println("Config saved");
  } catch (Exception e) {
    println("Failed to save config: " + e);
  }
}


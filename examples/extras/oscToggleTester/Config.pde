////////////////////////////////////////////////////////////////////////////////
// 設定ファイルのロード
////////////////////////////////////////////////////////////////////////////////
void setupConfig() {
  config = loadJSONObject(dataPath("config.json"));

  // 自分のポートを指定
  MY_OSC_PORT = config.getInt("myOscPort");

  // 最後に送った記録がある場合
  JSONObject lastQuery = config.getJSONObject("lastQuery");
  if (lastQuery != null) {
    DEFAULT_TARGET_IP_ADDRESS = lastQuery.getString("targetIpAddress");
    DEFAULT_TARGET_PORT = Integer.parseInt(lastQuery.getString("targetPort"));
  }

  for (int i=0; i<NUM_FIELDS; i++) {
    statusList[i] = false;
  }
}

void saveConfig(String _targetIp, String _targetPort, String _oscAddr, String _osrParam) {
  JSONObject lastQuery = new JSONObject();
  lastQuery.setString("targetIpAddress", _targetIp);
  lastQuery.setString("targetPort", _targetPort);
  lastQuery.setString("oscAddr", _oscAddr);
  lastQuery.setString("osrParam", _osrParam);

  config.setJSONObject("lastQuery", lastQuery);
  saveJSONObject(config, dataPath("config.json"));
}

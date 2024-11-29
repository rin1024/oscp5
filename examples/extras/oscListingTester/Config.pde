////////////////////////////////////////////////////////////////////////////////
// 設定ファイルのロード
////////////////////////////////////////////////////////////////////////////////
void setupConfig() {
  config = loadJSONObject(dataPath("config.json"));

  // 自分のポートを指定
  MY_OSC_PORT = config.getInt("myOscPort");
  DEFAULT_PREFIX_ADDR = config.getString("prefixAddr");

  // 最後に送った記録がある場合
  JSONObject lastQuery = config.getJSONObject("lastQuery");
  if (lastQuery != null) {
    DEFAULT_TARGET_IP_ADDRESS = lastQuery.getString("targetIpAddress");
    DEFAULT_TARGET_PORT = Integer.parseInt(lastQuery.getString("targetPort"));
    DEFAULT_OSC_FORMAT = lastQuery.getString("oscFormat");
    DEFAULT_OSC_PARAMS = lastQuery.getString("oscParams");
  }
}

void saveConfig(String _targetIp, String _targetPort, String _oscAddr, String _oscFormat, String _oscParamsAsString) {
      JSONObject lastQuery = new JSONObject();
      lastQuery.setString("targetIpAddress", _targetIp);
      lastQuery.setString("targetPort", _targetPort);
      lastQuery.setString("oscAddr", _oscAddr);
      lastQuery.setString("oscFormat", _oscFormat);
      lastQuery.setString("oscParams", _oscParamsAsString);

      config.setString("prefixAddr", targetPrefixAddressField.getText());
      config.setJSONObject("lastQuery", lastQuery);
      saveJSONObject(config, dataPath("config.json"));
}

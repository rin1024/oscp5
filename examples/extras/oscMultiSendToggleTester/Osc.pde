////////////////////////////////////////////////////////////////////////////////
// OSCを送る
////////////////////////////////////////////////////////////////////////////////
void sendOscToGo(int _index) {
  ArrayList<String> debugTextList = new ArrayList<String>();
  String targetIp = targetIpField.getText();
  int targetPort = Integer.parseInt(targetPortField.getText());
  String targetPrefixAddress = targetPrefixAddressField.getText();

  try {
    //String oscAddr = targetPrefixAddress + "_" + _index;
    String oscAddr = targetPrefixAddress + _index;
    int oscParam = statusList[_index] == true ? 1 : 0;

    // メッセージの整形をする
    OscMessage myMessage = new OscMessage(oscAddr);
    myMessage.add(oscParam);

    // 送信をする
    NetAddress targetLocation = new NetAddress(targetIp, targetPort);
    oscP5.send(targetLocation, myMessage);

    // 送信内容メモ
    debugTextList.add("target: " + targetIp + ":" + targetPort);
    debugTextList.add("addr: " + oscAddr);
    debugTextList.add("param: " + oscParam);

    // 最後の送信ログを次回利用するために記録
    saveConfig( targetIp, Integer.toString(targetPort), oscAddr, Integer.toString(oscParam));
  }
  catch (Exception e) {
    debugTextList.add(e.toString());
  }

  showLogText(debugTextList);

  sendTimer = millis();
}

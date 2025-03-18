////////////////////////////////////////////////////////////////////////////////
// OSCを送る
////////////////////////////////////////////////////////////////////////////////
void sendOsc(int _index) {
  ArrayList<String> debugTextList = new ArrayList<String>();
  String targetIp = targetIpField.getText();
  String targetPort = targetPortField.getText();
  String oscAddr = oscAddrField[_index].getText();
  String oscFormat = oscFormatField[_index].getText();
  String oscParamsAsString = oscParamsField[_index].getText();
  String[] oscParams = oscParamsAsString.split(" ");

  if (targetIp.equals("")) {
    debugTextList.add("targetIp is null. send failed.");
  }
  else if (targetPort.equals("")) {
    debugTextList.add("targetPort is null. send failed.");
  }
  else if (oscAddr.equals("")) {
    debugTextList.add("oscAddr is null. send failed.");
  }
  else if (oscFormat.equals("")) {
    debugTextList.add("oscFormat is null. send failed.");
  }
  else if (oscParamsAsString.equals("")) {
    debugTextList.add("oscParams is null. send failed.");
  }
  // formatの文字数とparamsのlengthがマッチするか
  else if (oscFormat.length() != oscParams.length) {
    debugTextList.add("oscFormat size and oscParams size. send failed.");
  }
  else {
    // メッセージの整形をする
    OscMessage myMessage = new OscMessage(oscAddr);
    try {
      for (int i=0;i<oscParams.length;i++) {
        char f = oscFormat.charAt(i);
        // integer
        if (f == 'i') {
          myMessage.add(Integer.parseInt(oscParams[i]));
        }
        // string
        else if (f == 's') {
          myMessage.add(oscParams[i]);
        }
        // float
        else if (f == 'f') {
          myMessage.add(Float.parseFloat(oscParams[i]));
        }
      }
    }
    catch (Exception e) {
      debugTextList.add(e.toString());
      return;
    }

    // 送信をする
    NetAddress targetLocation = new NetAddress(targetIp, Integer.parseInt(targetPort));
    oscP5.send(targetLocation, myMessage);

    // 送信内容メモ
    debugTextList.add("target: " + targetIp + ":" + targetPort);
    debugTextList.add("addr: " + oscAddr + " " + oscFormat);
    debugTextList.add("params: " + String.join(" ", oscParams));

    // 最後の送信ログを次回利用するために記録
    saveConfig( targetIp, targetPort, oscAddr, oscFormat, oscParamsAsString);
  }

  showLogText(debugTextList);
  
  sendTimer = millis();
}

////////////////////////////////////////////////////////////////////////////////
// OSCの受信時のイベントハンドラ
////////////////////////////////////////////////////////////////////////////////
void oscEvent(OscMessage _msg) {
  L.info("[oscEvent]" + _msg.toString());
}

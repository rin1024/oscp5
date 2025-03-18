////////////////////////////////////////////////////////////////////////////////
// GUIのsetup
////////////////////////////////////////////////////////////////////////////////
void setupGui() {
  listener = new AwtGuiListener(this);

  Canvas canvas = (Canvas)surface.getNative();
  JLayeredPane pane = (JLayeredPane)canvas.getParent().getParent();

  final int offsetX = 50;
  final int offsetY = 50;
  final int marginLabelX = 5;
  final int marginLabelY = 25;
  final int marginFormX = 20;
  final int marginFormY = 30;
  final int marginFormYTiny = 5;
  final int formWidth = 150;
  final int formHeight = 30;
  final int formWideWidth = 390;
  final int formDebugAreaWidth = 725;
  final int formDebugAreaHeight = 180;

  int posX = offsetX;
  int posY = offsetY;

  ////////////////////////////////////////
  // 送信先のIPアドレス
  targetIpField = new JTextField();
  targetIpField.addKeyListener(listener);
  targetIpField.setText(DEFAULT_TARGET_IP_ADDRESS);
  targetIpField.setBounds(
    posX, posY, formWidth, formHeight);
  pane.add(targetIpField);

  {
    JLabel l = new JLabel("Target IP Address");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  posX += formWidth + marginFormX;
  //posY = offsetY;

  ////////////////////////////////////////
  // 送信先のポート
  targetPortField = new JTextField();
  targetPortField.addKeyListener(listener);
  targetPortField.setText(DEFAULT_TARGET_PORT + "");
  targetPortField.setBounds(
    posX, posY, formWidth, formHeight);
  pane.add(targetPortField);

  {
    JLabel l = new JLabel("Target Port");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }
  
  posX += formWidth + marginFormX;
  //posY = offsetY;

  ////////////////////////////////////////
  // 送信先のアドレスのprefix
  targetPrefixAddressField = new JTextField();
  targetPrefixAddressField.addKeyListener(listener);
  targetPrefixAddressField.setText(DEFAULT_PREFIX_ADDR + "");
  targetPrefixAddressField.setBounds(
    posX, posY, formWidth, formHeight);
  pane.add(targetPrefixAddressField);

  {
    JLabel l = new JLabel("Prefix Address");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  posX = offsetX;
  posY += formHeight + marginFormY;
  
  ////////////////////////////////////////
  // ラベル色々
  {
    JLabel l = new JLabel("OSC Address");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  posX += formWidth + marginFormX;
  //posY = offsetY + formHeight + marginFormY;

  {
    JLabel l = new JLabel("OSC Format");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  posX += formWidth + marginFormX;
  //posY += formHeight + marginFormY;

  {
    JLabel l = new JLabel("OSC Params");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  // posX = offsetX;
  posY = offsetY + formHeight + marginFormY;

  ////////////////////////////////////////
  for (int i=0; i<NUM_FIELDS; i++) {
    posX = offsetX;
    
    // 添字
    JLabel l = new JLabel(String.format("[%2d]", i));
    l.setBounds(
      posX - marginLabelX * 5, posY, formWidth, formHeight);
    pane.add(l);

    // OSCアドレス
    oscAddrField[i] = new JTextField();
    oscAddrField[i].addKeyListener(listener);
    oscAddrField[i].setText(DEFAULT_PREFIX_ADDR + "_" + i);
    oscAddrField[i].setBounds(
      posX, posY, formWidth, formHeight);
    pane.add(oscAddrField[i]);
    
    posX += formWidth + marginFormX;

    // OSCフォーマット
    oscFormatField[i] = new JTextField();
    oscFormatField[i].addKeyListener(listener);
    oscFormatField[i].setText(DEFAULT_OSC_FORMAT);
    oscFormatField[i].setBounds(
      posX, posY, formWidth, formHeight);
    pane.add(oscFormatField[i]);
    
    posX += formWidth + marginFormX;

    oscParamsField[i] = new JTextField();
    oscParamsField[i].addKeyListener(listener);
    oscParamsField[i].setText(DEFAULT_OSC_PARAMS);
    oscParamsField[i].setBounds(
      posX, posY, formWideWidth, formHeight);
    pane.add(oscParamsField[i]);

    posY += formHeight + marginFormYTiny;
  }

  posX = offsetX;
  posY = offsetY + (formHeight + marginFormYTiny) * 12;

  ////////////////////////////////////////
  // デバッグ表示用エリア
  logText = new JTextArea();
  logText.setLineWrap(true);
  logText.setPreferredSize(new Dimension(formDebugAreaWidth, formDebugAreaHeight));
  logText.setBounds(
    posX, posY, formDebugAreaWidth, formDebugAreaHeight);
  pane.add(logText);

  // set background color
  bgColor = canvas.getBackground().getRGB();
  background(bgColor);
}

////////////////////////////////////////////////////////////////////////////////
// GUI周りの更新と描画
////////////////////////////////////////////////////////////////////////////////
void drawGui() {
  background(bgColor);
  if (sendTimer > 0 && millis() - sendTimer > 10000) {
    clearLogText();
    sendTimer = 0;
  }
}

////////////////////////////////////////////////////////////////////////////////
// set log
////////////////////////////////////////////////////////////////////////////////
void clearLogText() {
  logText.setText("");
}

void showLogText(ArrayList<String> _txtList) {
  String txt = String.join("\n", _txtList);
  logText.setText(txt);
  L.info(txt);
}

void showLogText(String _txt) {
  logText.setText(_txt);
  L.info(_txt);
}

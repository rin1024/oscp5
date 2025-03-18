final int offsetX = 30;
final int offsetY = 50;
final int marginLabelX = 5;
final int marginLabelY = 30;
final int marginFormX = 20;
final int marginFormY = 30;
final int marginFormYTiny = 5;
final int formWidth = 150;
final int formHeight = 30;
final int formWideWidth = 390;
final int formDebugAreaWidth = 725;
final int formDebugAreaHeight = 180;
final int buttonWidth = 80;
final int buttonHeight = 30;

////////////////////////////////////////////////////////////////////////////////
// GUIのsetup
////////////////////////////////////////////////////////////////////////////////
void setupGui() {
  listener = new AwtGuiListener(this);

  Canvas canvas = (Canvas)surface.getNative();
  JLayeredPane pane = (JLayeredPane)canvas.getParent().getParent();

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
  posY = offsetY;

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
  posY = offsetY;

  ////////////////////////////////////////
  // 送信先のアドレスのprefix
  targetPrefixAddressField = new JTextField();
  targetPrefixAddressField.addKeyListener(listener);
  targetPrefixAddressField.setText(DEFAULT_OSC_ADDR + "");
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
  // status表示用エリア
  {
    JLabel l = new JLabel("Status");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  posX = offsetX;
  posY += formHeight + marginFormY;

  ////////////////////////////////////////
  // デバッグ表示用エリア
  logText = new JTextArea();
  logText.setLineWrap(true);
  logText.setPreferredSize(new Dimension(formDebugAreaWidth, formDebugAreaHeight));
  logText.setBounds(
    posX, posY, formDebugAreaWidth, formDebugAreaHeight);
  pane.add(logText);

  {
    JLabel l = new JLabel("Debug");
    l.setBounds(
      posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
    pane.add(l);
  }

  // set background color
  bgColor = canvas.getBackground().getRGB();
  background(bgColor);
}

////////////////////////////////////////////////////////////////////////////////
// GUI周りの更新と描画
////////////////////////////////////////////////////////////////////////////////
void drawGui() {
  background(bgColor);

  int posX = offsetX + 5;
  int posY = offsetY + (formHeight + marginFormY);

  for (int i=0; i<NUM_FIELDS; i++) {
    stroke(255);
    fill (statusList[i] ? color(255) : color(0));
    rect(posX, posY, 15, 15);
    
    posX += marginFormX;
  }

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
  logText.setText(String.join("\n", _txtList));
  L.info(String.join(", ", _txtList));
}

void showLogText(String _txt) {
  logText.setText(_txt);
  L.info(_txt);
}

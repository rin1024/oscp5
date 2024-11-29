import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.StringJoiner ; 
import java.util.Arrays ;
import javax.swing.*;
import javax.swing.event.*;
import jp.ncl.awt.AwtGuiListener;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import oscP5.*;
import processing.awt.*;
import netP5.*;

protected final Logger L = Logger.getLogger(getClass());

////////////////////////////
// general
////////////////////////////
static int W_WIDTH = 800;
static int W_HEIGHT = 380;

JSONObject config;
int bgColor;

////////////////////////////
// OSC
////////////////////////////
int MY_OSC_PORT = 1234;

OscP5 oscP5;
NetAddress targetLocation;

////////////////////////////
// GUI
////////////////////////////
static final int NUM_FIELDS = 12;

String DEFAULT_TARGET_IP_ADDRESS = "192.168.0.104";
int DEFAULT_TARGET_PORT = 10000;
String DEFAULT_OSC_ADDR = "/inst";

AwtGuiListener listener;
JTextField targetIpField;
JTextField targetPortField;
JTextField targetPrefixAddressField;
JTextField oscPrefixAddrField;

JButton sendButton;
JTextArea logText;

////////////////////////////
// Params
////////////////////////////
boolean[] statusList = new boolean[NUM_FIELDS];

long sendTimer = 0;

////////////////////////////////////////////////////////////////////////////////
// settings
////////////////////////////////////////////////////////////////////////////////
void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(W_WIDTH, W_HEIGHT);
}

////////////////////////////////////////////////////////////////////////////////
// setup
////////////////////////////////////////////////////////////////////////////////
void setup() {
  setupConfig();

  // OSCの接続開始
  oscP5 = new OscP5(this, MY_OSC_PORT);

  setupGui();
}

////////////////////////////////////////////////////////////////////////////////
// draw
////////////////////////////////////////////////////////////////////////////////
void draw() {
  drawGui();
}

////////////////////////////////////////////////////////////////////////////////
// keyEvent
////////////////////////////////////////////////////////////////////////////////
void keyReleased() {
  int keyNo = keyCode - '0';
  boolean sendable = false;
  //println(keyNo);
  
  if (keyNo >= 0 && keyNo <= 9) {
    statusList[keyNo] = !statusList[keyNo];
    sendable = true;
  }
  else if (keyNo == -3) {
    keyNo = 10;
    statusList[keyNo] = !statusList[keyNo];
    sendable = true;
  }
  else if (keyNo == 13) {
    keyNo = 11;
    statusList[keyNo] = !statusList[keyNo];
    sendable = true;
  }
  
  if (sendable == true) {
    sendOscToGo(keyNo);
  }
}

////////////////////////////////////////////////////////////////////////////////
// OSCの受信時のイベントハンドラ
////////////////////////////////////////////////////////////////////////////////
void oscEvent(OscMessage _msg) {
  L.info("[oscEvent]" + _msg.toString());
}

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
static int W_HEIGHT = 680;

JSONObject config;
int bgColor;

////////////////////////////
// OSC
////////////////////////////
static int MY_OSC_PORT = 11000;

OscP5 oscP5;

////////////////////////////
// GUI
////////////////////////////
static final int NUM_FIELDS = 10;

String DEFAULT_TARGET_IP_ADDRESS = "192.168.0.111";
int DEFAULT_TARGET_PORT = 9999;
String DEFAULT_PREFIX_ADDR = "/test";
String DEFAULT_OSC_FORMAT = "ssiiss";
String DEFAULT_OSC_PARAMS = "TEST test 1234 5678 ABCD EFGH";

AwtGuiListener listener;
JTextField targetIpField;
JTextField targetPortField;
JTextField targetPrefixAddressField;

JTextField[] oscAddrField = new JTextField[NUM_FIELDS];
JTextField[] oscFormatField = new JTextField[NUM_FIELDS];
JTextField[] oscParamsField = new JTextField[NUM_FIELDS];

JButton sendButton;
JTextArea logText;

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
  if (keyNo >= 0 && keyNo <= 9) {
    sendOsc(keyNo);
  }
}

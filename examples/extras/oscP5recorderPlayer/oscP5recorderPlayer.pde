import oscP5.*;
import netP5.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.Base64;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

protected final Logger L = Logger.getLogger(getClass());
String LOG_FILE_PATH = "record.txt";

final int MY_OSC_PORT = 54445;//10000;

final String TARGET_OSC_IP_ADDRESS = "localhost";
final int TARGET_OSC_PORT = 10000;

OscP5 oscP5;
NetAddress receiver;

int STATUS_STOP = -1;
int STATUS_RECORD = 1;
int STATUS_PLAY = 2;

int recorderStatus = STATUS_STOP;
long countTimer = 0;

long lastReceivedMsgMillis = 0;
FileWriter fos;
BufferedReader reader = null;

OscPacket currentPacket = null;
long currentIndex = -1;
long totalLineCount = -1;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(800, 400);
}

void setup() {
  //textAlign(CENTER);
  textSize(20);

  OscProtocols theProtocol = OscProtocols.UDP;
  oscP5 = new OscP5( this, MY_OSC_PORT );
  //println("ip: " + oscP5.ip());
  
  receiver = new NetAddress(TARGET_OSC_IP_ADDRESS, TARGET_OSC_PORT);
}

void draw() {
  background(0);
  noStroke();

  fill(255);
  text("status: " + (recorderStatus == STATUS_RECORD ? "RECORD" : (recorderStatus == STATUS_PLAY ? "PLAY" : "STOP")), 20, 30);
  text("countTimer: " + (countTimer > 0 ? (millis() - countTimer) : "-"), 20, 60);

  // record
  {
    fill(255 * (millis() - lastReceivedMsgMillis < 250 ? 1 : 0), 0, 0);
    stroke(255);
    rect(20, 80, 20, 20);
  }

  noStroke();

  // play
  {
    fill(255);
    text("totalLineCount: " + currentIndex + " / " + totalLineCount, 20, 150);
    if (currentPacket != null) {
      text("currentPacket: " + currentPacket.toString(), 20, 180);

      if (millis() - countTimer > currentPacket.timeCode) {
        currentPacket.send(oscP5, receiver);
        println("bang");
        
        readNextPacket();
      }
    }
  }

  fill(255);
  text("[Shortcut]\r\n" + 
    "[R] ... record start\r\n" + 
    "[P] ... play start\r\n" + 
    "[S] ... stop\r\n", 20, 300);
}

void keyReleased() {
  if (keyCode == 'R' && recorderStatus != STATUS_RECORD) {
    recordStart();
  } else if (keyCode == 'S') {
    recorderStop();
  } else if (keyCode == 'P') {
    playStart();
  }
}

void oscEvent( OscMessage _msg ) {
  String address = _msg.getAddress();
  String typeTags = _msg.getTypetag();
  ArrayList<Object> params = new ArrayList<Object>();

  print( "Received an osc message" );
  print( ", address pattern: " + address );
  print( ", typeTags: " + typeTags );
  println();

  lastReceivedMsgMillis = millis();

  try {
    long timeCode = millis() - countTimer;
    String newLine = timeCode + "\t" + address + "\t" + typeTags + "\t";

    for (int i=0; i<typeTags.length(); i++) {
      char typeTag = typeTags.charAt(i);
      switch (typeTag) {
      case 'f':
        {
          float val = _msg.floatValue(i);
          params.add(val);
          newLine += val + ":::";
          break;
        }
      case 's':
      case 'c':
        {
          String val = _msg.stringValue(i);
          params.add(val);
          newLine += val + ":::";
          break;
        }
      case 'i':
        {
          int val = _msg.intValue(i);
          params.add(val);
          newLine += val + ":::";
          break;
        }
      case 'b':
        {
          byte[] val = _msg.blobValue(i);
          params.add(val);
          newLine += val + ":::";
          break;
        }
      default:
        {
          println("unknown typetag: " + typeTag);
          break;
        }
      }
    }

    fos.write(newLine + "\r\n");
  }
  catch (Exception e) {
    println(e);
  }
}

void recordStart() {
  println("RECORD START");

  try {
    fos = new FileWriter(dataPath(LOG_FILE_PATH));
  }
  catch (Exception e) {
    println(e);
  }

  countTimer = millis();
  recorderStatus = STATUS_RECORD;
}

void recorderStop() {
  println("STOP");

  if (recorderStatus == STATUS_RECORD) {
    try {
      fos.flush();
      fos.close();
    }
    catch (Exception e) {
      println(e);
    }
  } else if (recorderStatus == STATUS_PLAY) {
    try {
      reader.close();
      reader = null;

      currentPacket = null;
    }
    catch (Exception e) {
      println(e);
    }
  }

  currentIndex = 0;
  countTimer = 0;
  recorderStatus = STATUS_STOP;
}

void playStart() {
  println("PLAY START");

  Path path = Paths.get(dataPath(LOG_FILE_PATH));
  Charset charset = StandardCharsets.UTF_8;

  try {
    if (reader == null) {
      reader = Files.newBufferedReader(path, charset);
      totalLineCount = Files.lines(path).count();

      readNextPacket();
    }
  }
  catch (Exception e) {
    println(e);
  }

  countTimer = millis();
  recorderStatus = STATUS_PLAY;
}

void readNextPacket() {
  try {
    String line = reader.readLine();
    if (line != null) {
      currentIndex++;

      println("[" + currentIndex + "]" + line);
      String[] stringPacket = line.split("\t");
      ArrayList<Object> params = new ArrayList<Object>(Arrays.asList(stringPacket[3].split(":::")));
      for (int i=0;i<params.size();i++) {
        Object param = params.get(i);
        if (isInt(param.toString())) {
          params.set(i, Integer.parseInt(param.toString()));
        }
        else if (isDouble(param.toString())) {
          params.set(i, Float.parseFloat(param.toString()));
        }
      }

      // 処理
      currentPacket = new OscPacket(
        Long.parseLong(stringPacket[0]),
        stringPacket[1],
        stringPacket[2],
        params
        );
    } else {
      println("END");

      if (reader != null) {
        reader.close();
      }
      reader = null;

      currentPacket = null;
      currentIndex = 0;
      countTimer = 0;
    }
  }
  catch (Exception e) {
    println(e);
  }
}

boolean isDouble(String _source) {
  try {
    Double.parseDouble(_source);
    return true;
  }
  catch (NumberFormatException e) {
  }
  return false;
}

boolean isInt(String _source) {
  try {
    Integer.parseInt(_source);
    return true;
  }
  catch (NumberFormatException e) {
  }
  return false;
}

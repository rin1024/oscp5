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
import java.io.BufferedWriter;
import java.util.Base64;
import java.util.Arrays;
import org.apache.log4j.PropertyConfigurator;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import processing.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

protected final Logger L = Logger.getLogger(getClass());
String LOG_FILE_PREFIX = "record";

int MY_OSC_PORT = 54445;
String TARGET_OSC_IP_ADDRESS = "localhost";
int TARGET_OSC_PORT = 10000;

JSONObject config;

OscP5 oscP5;
NetAddress receiver;

int STATUS_STOP = -1;
int STATUS_RECORD = 1;
int STATUS_PLAY = 2;
int STATUS_PAUSE = 4;
int STATUS_END = 3;

int recorderStatus = STATUS_STOP;
long countTimer = 0;
long pauseOffset = 0; // pause時の時間オフセット
String currentPlaybackFile = null; // 現在再生中のファイルパス

long lastReceivedMsgMillis = 0;
BufferedWriter fos;
BufferedReader reader = null;

OscPacket currentPacket = null;
long currentIndex = -1;
long totalLineCount = -1;

// GUI
JTextField myOscPortField;
JTextField targetIpField;
JTextField targetPortField;
JButton saveConfigButton;
int bgColor;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(800, 500);
}

void setup() {
  //textAlign(CENTER);
  textSize(20);

  setupConfig();
  setupGui();

  OscProtocols theProtocol = OscProtocols.UDP;
  oscP5 = new OscP5( this, MY_OSC_PORT );
  //println("ip: " + oscP5.ip());
  
  receiver = new NetAddress(TARGET_OSC_IP_ADDRESS, TARGET_OSC_PORT);
}

void draw() {
  background(bgColor);
  noStroke();

  // record
  {
    fill(255 * (millis() - lastReceivedMsgMillis < 250 ? 1 : 0), 0, 0);
    stroke(255);
    rect(15, 17, 15, 15);
  }

  noStroke();

  fill(255);
  String statusText = "STOP";
  if (recorderStatus == STATUS_RECORD) {
    statusText = "RECORD";
  } else if (recorderStatus == STATUS_PLAY) {
    statusText = "PLAY";
  } else if (recorderStatus == STATUS_PAUSE) {
    statusText = "PAUSE";
  } else if (recorderStatus == STATUS_END) {
    statusText = "END";
  }
  text("status: " + statusText, 40, 30);
  text("countTimer: " + String.format("%06d", countTimer > 0 ? (millis() - countTimer) : 0), 200, 30);

  // play
  {
    fill(255);
    text("totalLineCount: " + currentIndex + " / " + totalLineCount, 20, 150);
    if (currentPacket != null) {
      text("currentPacket: " + currentPacket.toString(), 20, 180);

      // pause中は送信しない
      if (recorderStatus == STATUS_PLAY) {
        // 複数のパケットが同じフレーム内で送信されるべき場合に対応
        long currentTime = millis() - countTimer + pauseOffset;
        while (currentPacket != null && currentTime >= currentPacket.timeCode) {
          currentPacket.send(oscP5, receiver);
          readNextPacket();
          if (currentPacket != null) {
            currentTime = millis() - countTimer + pauseOffset;
          }
        }
      }
    }
  }

  fill(255);
  text("[Shortcut]\r\n" + 
    "[R] ... record start\r\n" + 
    "[P] ... play start\r\n" + 
    "[Space] ... pause/resume\r\n" + 
    "[←] ... -500ms\r\n" + 
    "[→] ... +500ms\r\n" + 
    "[S] ... stop\r\n", 20, height - 120);
}

void keyReleased() {
  if (keyCode == 'R' && recorderStatus != STATUS_RECORD) {
    recordStart();
  } else if (keyCode == 'S') {
    recorderStop();
  } else if (keyCode == 'P') {
    playStart();
  } else if (keyCode == ' ') { // Space key
    togglePause();
  } else if (keyCode == LEFT) {
    seekTime(-500);
  } else if (keyCode == RIGHT) {
    seekTime(500);
  }
}

void oscEvent( OscMessage _msg ) {
  String address = _msg.getAddress();
  String typeTags = _msg.getTypetag();
  ArrayList<Object> params = new ArrayList<Object>();

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
          String base64Val = Base64.getEncoder().encodeToString(val);
          newLine += base64Val + ":::";
          break;
        }
      default:
        {
          // unknown typetag - ログ出力を削除してパフォーマンス向上
          break;
        }
      }
    }

    fos.write(newLine);
    fos.newLine();
    currentIndex++; // レコーディング時のカウンタをインクリメント
    // リアルタイム性を保つため、定期的にflush（毎回は遅延の原因になるため、一定間隔で）
    if (currentIndex % 10 == 0) {
      fos.flush();
    }
  }
  catch (Exception e) {
    println("Error in oscEvent: " + e);
  }
}

String getLogFilePath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
  String dateStr = sdf.format(new Date());
  String logsDir = sketchPath("records");
  
  // logsディレクトリが存在しない場合は作成
  try {
    Path logsPath = Paths.get(logsDir);
    if (!Files.exists(logsPath)) {
      Files.createDirectories(logsPath);
    }
  } catch (Exception e) {
    println("Failed to create logs directory: " + e);
  }
  
  return logsDir + "/" + LOG_FILE_PREFIX + "_" + dateStr + ".txt";
}

void recordStart() {
  println("RECORD START");

  try {
    String logFilePath = getLogFilePath();
    FileWriter fw = new FileWriter(logFilePath);
    fos = new BufferedWriter(fw, 8192); // 8KBバッファでパフォーマンス向上
    println("Recording to: " + logFilePath);
  }
  catch (Exception e) {
    println(e);
  }

  countTimer = millis();
  recorderStatus = STATUS_RECORD;
  currentIndex = 0; // レコーディング時のカウンタをリセット
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
  } else if (recorderStatus == STATUS_PLAY || recorderStatus == STATUS_PAUSE) {
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
  pauseOffset = 0;
  recorderStatus = STATUS_STOP;
  currentPlaybackFile = null;
}

void playStart() {
  println("PLAY START");

  // ファイル選択ダイアログを表示
  JFileChooser fileChooser = new JFileChooser();
  fileChooser.setDialogTitle("Select Record File to Play");
  
  // recordsフォルダをデフォルトの場所に設定
  String recordsDir = sketchPath("records");
  File recordsFolder = new File(recordsDir);
  if (recordsFolder.exists()) {
    fileChooser.setCurrentDirectory(recordsFolder);
  }
  
  // .txtファイルのみ選択可能にする
  FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
  fileChooser.setFileFilter(filter);
  
  int result = fileChooser.showOpenDialog(null);
  
  if (result == JFileChooser.APPROVE_OPTION) {
    File selectedFile = fileChooser.getSelectedFile();
    String logFilePath = selectedFile.getAbsolutePath();
    Path path = Paths.get(logFilePath);
    Charset charset = StandardCharsets.UTF_8;

    try {
      if (reader == null || !logFilePath.equals(currentPlaybackFile)) {
        if (Files.exists(path)) {
          if (reader != null) {
            reader.close();
          }
          reader = Files.newBufferedReader(path, charset);
          totalLineCount = Files.lines(path).count();
          currentPlaybackFile = logFilePath;
          println("Playing from: " + logFilePath);
          readNextPacket();
          
          countTimer = millis();
          pauseOffset = 0;
          recorderStatus = STATUS_PLAY;
        } else {
          println("Log file not found: " + logFilePath);
          return;
        }
      } else {
        // 同じファイルが既に開かれている場合は再開
        recorderStatus = STATUS_PLAY;
        countTimer = millis();
      }
    }
    catch (Exception e) {
      println("Failed to open file: " + e);
    }
  } else {
    println("File selection cancelled");
  }
}

void readNextPacket() {
  try {
    String line = reader.readLine();
    if (line != null) {
      currentIndex++;

      // デバッグ出力を削除してパフォーマンス向上（必要に応じてコメントアウト）
      // println("[" + currentIndex + "]" + line);
      String[] stringPacket = line.split("\t");
      ArrayList<Object> params = new ArrayList<Object>(Arrays.asList(stringPacket[3].split(":::")));
      String typeTags = stringPacket[2];
      for (int i=0;i<params.size() && i<typeTags.length();i++) {
        Object param = params.get(i);
        char typeTag = typeTags.charAt(i);
        if (typeTag == 'b') {
          // Base64デコードしてbyte[]に戻す
          try {
            byte[] decodedBytes = Base64.getDecoder().decode(param.toString());
            params.set(i, decodedBytes);
          } catch (Exception e) {
            println("Failed to decode Base64: " + e);
          }
        } else if (typeTag == 'i' && isInt(param.toString())) {
          params.set(i, Integer.parseInt(param.toString()));
        } else if (typeTag == 'f' && isDouble(param.toString())) {
          params.set(i, Float.parseFloat(param.toString()));
        }
        // 's'と'c'の場合は文字列のまま
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
      recorderStatus = STATUS_END;
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

////////////////////////////////////////////////////////////////////////////////
// Pause/Resume機能
////////////////////////////////////////////////////////////////////////////////
void togglePause() {
  if (recorderStatus == STATUS_PLAY) {
    // pause
    pauseOffset += millis() - countTimer;
    recorderStatus = STATUS_PAUSE;
    println("PAUSE at " + pauseOffset + "ms");
  } else if (recorderStatus == STATUS_PAUSE) {
    // resume
    countTimer = millis();
    recorderStatus = STATUS_PLAY;
    println("RESUME from " + pauseOffset + "ms");
  }
}

////////////////////////////////////////////////////////////////////////////////
// 時間シーク機能
////////////////////////////////////////////////////////////////////////////////
void seekTime(long deltaMs) {
  if (recorderStatus == STATUS_PLAY || recorderStatus == STATUS_PAUSE) {
    if (currentPlaybackFile == null || reader == null) {
      return;
    }
    
    long currentTime = (recorderStatus == STATUS_PAUSE) ? pauseOffset : (millis() - countTimer + pauseOffset);
    long newTime = Math.max(0, currentTime + deltaMs);
    
    println("Seeking from " + currentTime + "ms to " + newTime + "ms");
    
    try {
      // ファイルを最初から読み直して、新しい時間位置を見つける
      reader.close();
      Path path = Paths.get(currentPlaybackFile);
      Charset charset = StandardCharsets.UTF_8;
      reader = Files.newBufferedReader(path, charset);
      
      currentIndex = 0;
      currentPacket = null;
      OscPacket lastPacket = null;
      long lastTimeCode = 0;
      
      // 指定時間までのパケットを読み込む
      String line;
      while ((line = reader.readLine()) != null) {
        currentIndex++;
        String[] stringPacket = line.split("\t");
        long timeCode = Long.parseLong(stringPacket[0]);
        
        if (timeCode > newTime) {
          // このパケットはまだ送信すべきでない
          currentPacket = lastPacket;
          break;
        }
        
        // パケットを解析
        ArrayList<Object> params = new ArrayList<Object>(Arrays.asList(stringPacket[3].split(":::")));
        String typeTags = stringPacket[2];
        for (int i=0;i<params.size() && i<typeTags.length();i++) {
          Object param = params.get(i);
          char typeTag = typeTags.charAt(i);
          if (typeTag == 'b') {
            try {
              byte[] decodedBytes = Base64.getDecoder().decode(param.toString());
              params.set(i, decodedBytes);
            } catch (Exception e) {
              println("Failed to decode Base64: " + e);
            }
          } else if (typeTag == 'i' && isInt(param.toString())) {
            params.set(i, Integer.parseInt(param.toString()));
          } else if (typeTag == 'f' && isDouble(param.toString())) {
            params.set(i, Float.parseFloat(param.toString()));
          }
        }
        
        lastPacket = new OscPacket(timeCode, stringPacket[1], stringPacket[2], params);
        lastTimeCode = timeCode;
      }
      
      // 最後まで読み込んだ場合
      if (currentPacket == null && lastPacket != null) {
        currentPacket = lastPacket;
      }
      
      // 時間を更新
      pauseOffset = newTime;
      countTimer = millis();
      recorderStatus = STATUS_PLAY;
      
      println("Seeked to " + newTime + "ms, packet index: " + currentIndex);
    } catch (Exception e) {
      println("Failed to seek: " + e);
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// 設定保存とOSC接続の再初期化
////////////////////////////////////////////////////////////////////////////////
void saveConfigAndReinitOsc() {
  try {
    int newMyOscPort = Integer.parseInt(myOscPortField.getText());
    String newTargetIpAddress = targetIpField.getText();
    int newTargetPort = Integer.parseInt(targetPortField.getText());
    
    saveConfig(newMyOscPort, newTargetIpAddress, newTargetPort);
    
    // OSC接続を再初期化
    if (oscP5 != null) {
      oscP5.stop();
    }
    oscP5 = new OscP5(this, MY_OSC_PORT);
    receiver = new NetAddress(TARGET_OSC_IP_ADDRESS, TARGET_OSC_PORT);
    
    println("Config saved and OSC reinitialized");
  } catch (Exception e) {
    println("Failed to save config: " + e);
  }
}

////////////////////////////////////////////////////////////////////////////////
// GUIのsetup
////////////////////////////////////////////////////////////////////////////////
void setupGui() {
  try {
    Canvas canvas = (Canvas)surface.getNative();
    JLayeredPane pane = (JLayeredPane)canvas.getParent().getParent();

    final int offsetX = 20;
    final int offsetY = 80;
    final int marginLabelX = 5;
    final int marginLabelY = 25;
    final int marginFormX = 20;
    final int formWidth = 150;
    final int formHeight = 30;
    final int buttonWidth = 100;
    final int buttonHeight = 30;

    int posX = offsetX;
    int posY = offsetY;

    // My OSC Port
    myOscPortField = new JTextField();
    myOscPortField.setText(String.valueOf(MY_OSC_PORT));
    myOscPortField.setBounds(posX, posY, formWidth, formHeight);
    myOscPortField.setBackground(new Color(255, 255, 255));
    myOscPortField.setForeground(new Color(0, 0, 0));
    myOscPortField.setOpaque(true);
    pane.add(myOscPortField);

    {
      JLabel l = new JLabel("My OSC Port");
      l.setBounds(posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
      l.setBackground(new Color(0, 0, 0));
      l.setForeground(new Color(255, 255, 255));
      l.setOpaque(true);
      pane.add(l);
    }

    posX += formWidth + marginFormX;

    // Target IP Address
    targetIpField = new JTextField();
    targetIpField.setText(TARGET_OSC_IP_ADDRESS);
    targetIpField.setBounds(posX, posY, formWidth, formHeight);
    targetIpField.setBackground(new Color(255, 255, 255));
    targetIpField.setForeground(new Color(0, 0, 0));
    targetIpField.setOpaque(true);
    pane.add(targetIpField);

    {
      JLabel l = new JLabel("Target IP Address");
      l.setBounds(posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
      l.setBackground(new Color(0, 0, 0));
      l.setForeground(new Color(255, 255, 255));
      l.setOpaque(true);
      pane.add(l);
    }

    posX += formWidth + marginFormX;

    // Target Port
    targetPortField = new JTextField();
    targetPortField.setText(String.valueOf(TARGET_OSC_PORT));
    targetPortField.setBounds(posX, posY, formWidth, formHeight);
    targetPortField.setBackground(new Color(255, 255, 255));
    targetPortField.setForeground(new Color(0, 0, 0));
    targetPortField.setOpaque(true);
    pane.add(targetPortField);

    {
      JLabel l = new JLabel("Target Port");
      l.setBounds(posX + marginLabelX, posY - marginLabelY, formWidth, formHeight);
      l.setBackground(new Color(0, 0, 0));
      l.setForeground(new Color(255, 255, 255));
      l.setOpaque(true);
      pane.add(l);
    }

    posX += formWidth + marginFormX;

    // Save Button
    saveConfigButton = new JButton("Save Config");
    saveConfigButton.setBackground(new Color(0, 0, 0));
    saveConfigButton.setForeground(new Color(0, 0, 0));
    saveConfigButton.setOpaque(true);
    saveConfigButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveConfigAndReinitOsc();
      }
    });
    saveConfigButton.setBounds(posX, posY, buttonWidth, buttonHeight);
    pane.add(saveConfigButton);

    // set background color
    bgColor = color(0);//canvas.getBackground().getRGB();
    background(bgColor);
  } catch (Exception e) {
    println("Failed to setup GUI: " + e);
    bgColor = 0;
  }
}

import oscP5.*;
import netP5.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

protected final Logger L = Logger.getLogger(getClass());

final int MY_PORT = 10000;

OscP5 oscP5;

HashMap<String, GraphMonitor> graphs = new HashMap<String, GraphMonitor>();

int X_OFFSET = 50;
int Y_OFFSET = 80;
int X_WIDTH =  320;
int Y_HEIGHT = 240;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(1920, 1080, P3D);
}

void setup() {
  oscP5 = new OscP5( this, MY_PORT );
}

void draw() {
  background(0);
  
  fill(255);
  textAlign(LEFT);
  text("myPort: " + MY_PORT + ", numGraphs: " + graphs.size(), 20, 20);
  
  for (String graphName : graphs.keySet()) {
    GraphMonitor graph = graphs.get(graphName);
    if (graph != null) {
      graph.graphDraw(this.g);
    }
  }
}

void oscEvent(OscMessage _msg) {
  //_msg.print();
  String senderIP = _msg.getHostAddress();
  String address = _msg.getAddress();
  String typeTags = _msg.getTypetag();

  print("Received an osc message");
  //print(" from: " + senderIP);
  print(", address pattern: " + address );
  print(", typeTags: " + typeTags );
  println();

  try {
    String newLine = address + "\t" + typeTags + "\t";

    for (int i=0; i<typeTags.length(); i++) {
      char typeTag = typeTags.charAt(i);
      switch (typeTag) {
      case 'f':
      case 'd':
        {
          float val = _msg.floatValue(i);
          println(val);

          String graphKey = senderIP + "" + address;
          GraphMonitor graph = graphs.get(graphKey);
          if (graph == null) {
            graph = new GraphMonitor(this);
            graph.setup(graphKey, X_OFFSET, Y_OFFSET + (Y_HEIGHT + 20) * graphs.size(), X_WIDTH, Y_HEIGHT);
            graphs.put(graphKey, graph);
          }
          
          graph.addGraph(val);

          break;
        }
      default:
        {
          println("unknown typetag: " + typeTag);
          break;
        }
      }
    }
  }
  catch (Exception e) {
    println(e);
  }
}

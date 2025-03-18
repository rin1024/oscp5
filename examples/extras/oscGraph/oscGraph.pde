import oscP5.*;
import netP5.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

protected final Logger L = Logger.getLogger(getClass());

OscP5 oscP5;

GraphMonitor tGraph;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(1920, 1080, P3D);
}

void setup() {
  String theRemoteAddress = "localhost";
  int theRemotePort = 54445;//10000;
  OscProtocols theProtocol = OscProtocols.UDP;
  //oscP5 = new OscP5( this , theRemoteAddress, theRemotePort, theProtocol );
  oscP5 = new OscP5( this, theRemotePort );

  String TITLE = "graphTitle";
  int X_OFFSET = 100;
  int Y_OFFSET = 50;
  int X_WIDTH = 1000;
  int Y_HEIGHT = 400;
  tGraph = new GraphMonitor(this);
  tGraph.setup(TITLE, X_OFFSET, Y_OFFSET, X_WIDTH, Y_HEIGHT);
}

void draw() {
  background(250);
  
  tGraph.graphDraw(this.g);
  //tGraph.setOffset(mouseX, mouseY);
}

void oscEvent( OscMessage _msg ) {
  String address = _msg.getAddress();
  String typeTags = _msg.getTypetag();

  print( "Received an osc message" );
  print( ", address pattern: " + address );
  print( ", typeTags: " + typeTags );
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
          println("float or double: " + val);
          tGraph.addGraph(val);

          break;
        }
      case 's':
      case 'c':
        {
          String val = _msg.stringValue(i);
          println("str or char: " + val);

          break;
        }
      case 'i':
        {
          int val = _msg.intValue(i);
          println("int: " + val);

          break;
        }
      case 'b':
        {
          byte[] val = _msg.blobValue(i);
          println("byte: " + val);

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

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

// https://garchiving.com/real-time-graph-by-proccesing/
float a, b, c;
int k;
float w = 2.0;

GraphMonitor testGraph;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(1920, 1080, P3D);
}

void setup() {
  //frameRate(100);
  //smooth();
  
  String theRemoteAddress = "localhost";
  int theRemotePort = 54445;//10000;
  OscProtocols theProtocol = OscProtocols.UDP;
  //oscP5 = new OscP5( this , theRemoteAddress, theRemotePort, theProtocol );
  oscP5 = new OscP5( this, theRemotePort );

  testGraph = new GraphMonitor("graphTitle", 100, 50, 1000, 400);
}

void draw() {
  background(250);
  //testGraph.addGraph(a, b, c);
  testGraph.graphDraw();

  /*a = sin(radians(k));
  b = cos(radians(k) * 10);
  c = sin(radians(k)) * w;
  k++;*/
}

/*void keyPressed() {
  if (keyCode == UP) {
    w = w + 0.1;
  }
  if (keyCode == DOWN) {
    w = w - 0.1;
  }
}*/

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
          println(val);
          testGraph.addGraph(val, 0f, 0f);

          break;
        }
      case 's':
      case 'c':
        {
          String val = _msg.stringValue(i);
          println(val);

          break;
        }
      case 'i':
        {
          int val = _msg.intValue(i);
          println(val);
          //testGraph.addGraph(val, 0f, 0f);

          break;
        }
      case 'b':
        {
          byte[] val = _msg.blobValue(i);
          println(val);

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

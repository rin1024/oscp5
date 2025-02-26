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

float r, g, b;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(400, 400);
}

void setup() {
  String theRemoteAddress = "localhost";
  int theRemotePort = 10000;
  OscProtocols theProtocol = OscProtocols.UDP;
  //oscP5 = new OscP5( this , theRemoteAddress, theRemotePort, theProtocol );
  oscP5 = new OscP5( this, theRemotePort );

  //println("ip: " + oscP5.ip());
}

void draw() {
  background(r, g, b);
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
        {
          float val = _msg.floatValue(i);
          println(val);

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
  
  // EXT
  String colorCode = _msg.stringValue(1);
  switch (colorCode) {
    case "R": {
      r = 255;
      g = 1;
      b = 1;
      break;
    }
    case "G": {
      r = 1;
      g = 255;
      b = 1;
      break;
    }
    case "B": {
      r = 1;
      g = 1;
      b = 255;
      break;
    }
    default: {
      r = 0;
      g = 0;
      b = 0;
      break;
    }
  }
}

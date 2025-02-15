/**
 * OscP5 send
 * this example shows how to send an OSC message
 * on mouse-pressed.
 *
 * by Andreas Schlegel, 2013
 * www.sojamo.de/libraries/oscp5
 *
 */
import oscP5.*;
import netP5.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import java.util.Map;

final Logger L = Logger.getLogger(getClass());

OscP5 osc;
NetAddress receiver;

float r, g, b;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(400, 400);
}

void setup() {
  /* create a new instance of OscP5, the second parameter indicates the listening port */
  osc = new OscP5( this, 12000 );

  /* create a NetAddress which requires the receiver's IP address and port number */
  receiver = new NetAddress( "127.0.0.1", 8091 );

  //println("ip: " + osc.ip());
  println("[NetInfo]");
  NetInfo.print();
  println();

  Map<String, Map> networkInterfaces = NetInfo.getNetworkInterfaces();
  println("[networkInterfaces]");
  for (String key : networkInterfaces.keySet()) {
    System.out.println(key + ": " + networkInterfaces.get(key));
  }
}

void draw() {
  background( r, g, b );
}

void keyReleased() {
  OscMessage msg = new OscMessage("/colors");
  msg.add("color");
  msg.add((char)(keyCode));
  osc.send(receiver, msg);
  println(msg);
  
  switch (keyCode) {
    case 'R': {
      r = 255;
      g = 1;
      b = 1;
      break;
    }
    case 'G': {
      r = 1;
      g = 255;
      b = 1;
      break;
    }
    case 'B': {
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
  };
}

/*
void mousePressed() {
  // send an OSC message to NetAddress addr
  //osc.send( receiver , "/test" , random( 255 ) , random( 255 ) , random( 255 ) );

  OscMessage msg = new OscMessage("/sound/seek");
  msg.add("aaaa");
  msg.add(1234);
  osc.send(receiver, msg);
  println(msg);
}
*/

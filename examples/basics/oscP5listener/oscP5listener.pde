import oscP5.*;
import netP5.*;
import org.apache.log4j.Logger;

OscP5 osc;
NetAddress receiver;

void setup() {
  size( 400, 400 );

  osc = new OscP5( this, 12000 );
  receiver = new NetAddress( "127.0.0.1", 8001 );

  osc.addListener(new AdvancedOscEventLisener());
}

void draw() {
  background(0);
}

void mousePressed() {
  osc.send( receiver, "/get/currentInfo", 1, 2, 3 );
}

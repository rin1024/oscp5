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

protected final Logger L = Logger.getLogger(getClass());

OscP5 osc;
NetAddress receiver;

float r,g,b;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(400,400);
}

void setup() {
  /* create a new instance of OscP5, the second parameter indicates the listening port */
  osc = new OscP5( this , 12000 );
  
  /* create a NetAddress which requires the receiver's IP address and port number */
  receiver = new NetAddress( "127.0.0.1" , 12000 );

  println("ip: " + osc.ip());
}

void draw() {
  background( r , g , b );
}

void mousePressed() {
  /* send an OSC message to NetAddress addr */
  //osc.send( receiver , "/test" , random( 255 ) , random( 255 ) , random( 255 ) );
  
    OscMessage msg = new OscMessage("/sound/seek");
    msg.add("aaaa");
    msg.add(1234);
    osc.send(receiver, msg);
        
  /* breakdown of parameters
   * 1) receiver's NetAddress
   * 2) the address pattern of the OSC-Message 
   * 3-5) the arguments of the OSC-Message, here we add 3 arguments each of type 
   * float which will result in a 3 characters long typetag fff
   *
   * arguments here can be of type int, float, double, String, boolean, blob (byte-array)
   * see OSC specifications 1.0 http://opensoundcontrol.org/spec-1_0  
  */
  println(msg);
}

/* oscEvent(OscMessage) will wait for incoming OSC messages. */
void oscEvent( OscMessage m ) {
  
  /* check if an incoming OSC message matches a particular address pattern */
  if( m.getAddress().equals( "/test") ) {
    r = m.floatValue( 0 );
    g = m.floatValue( 1 );
    b = m.floatValue( 2 );
  }
  
}

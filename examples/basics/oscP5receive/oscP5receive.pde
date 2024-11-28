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
NetAddress receiver;

void settings() {
  System.setProperty("logging.dir", dataPath("../log/"));
  PropertyConfigurator.configure(dataPath("log4j.properties"));

  size(400,400);
}

void setup() {
  oscP5 = new OscP5( this , 54445 );

  println("ip: " + oscP5.ip());
}

void draw() {
  background(255);
}

void oscEvent( OscMessage _msg ) {
  byte[] data = _msg.blobValue(0);
  print( "Received an osc message" );
  print( ", address pattern: " + _msg.getAddress( ) );
  print( ", typetag: " + _msg.getTypetag( ) );
  print(", data: " + data);
  println();
  
  Charset charset = StandardCharsets.UTF_8;
  print("### received an osc message: '" + new String(data, charset) + "'"); 
  println();
}

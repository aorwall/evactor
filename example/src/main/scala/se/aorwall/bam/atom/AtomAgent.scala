package se.aorwall.bam.atom
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder

object AtomAgent extends App {

   val camelContext = new DefaultCamelContext();
   
   camelContext.addRoutes(new RouteBuilder() {
		def configure() {
			from("atom://https://github.com/aorwall/bam/commits/master.atom")
			.to("log:hej");
		}
   });
   
   camelContext.start()
  
  
}
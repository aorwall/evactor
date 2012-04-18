package se.aorwall.bam.api

import unfiltered.netty._
import java.net.URLDecoder

trait NettyPlan extends cycle.Plan 
					 with cycle.ThreadPool 
					 with ServerErrorResponse {
  
  def decode(name: String) = {
    URLDecoder.decode(name, "UTF-8")
  }
  
}

package se.aorwall.bam.api

import unfiltered.netty._

trait NettyPlan extends cycle.Plan 
					 with cycle.ThreadPool 
					 with ServerErrorResponse
					 
package se.aorwall.bam.api

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import unfiltered.response.BadRequest
import unfiltered.request.Path
import unfiltered.request.Seg
import scala.io.Source
import unfiltered.response.ResponseString
import unfiltered.request.Params

class BasePlan (system: ActorSystem) extends NettyPlan with Logging {

  val kpiApi = new KpiEventAPI(system)
  val dataApi = new DataEventAPI(system)
  val requestApi = new RequestEventAPI(system)
  val logApi = new LogEventAPI(system)
  val alertApi = new AlertEventAPI(system)
  
  
  def intent = {
   
    case req @ Path(Seg("kpi" :: path)) => try {
      val Params(params) = req
      kpiApi.doRequest(path, params)
    } catch { case e => warn("error while calling kpi event api", e); BadRequest }
    case req @ Path(Seg("data" :: path)) => try {
      val Params(params) = req
      dataApi.doRequest(path, params)
    } catch { case e => warn("error while calling data event api", e); BadRequest }
    case req @ Path(Seg("request" :: path)) => try {
      val Params(params) = req
      requestApi.doRequest(path, params)
    } catch { case e => warn("error while calling request event api", e); BadRequest }
    case req @ Path(Seg("log" :: path)) => try {
      val Params(params) = req
      logApi.doRequest(path, params)
    } catch { case e => warn("error while calling log event api", e); BadRequest }
    case req @ Path(Seg("alert" :: path)) => try {
      val Params(params) = req
      alertApi.doRequest(path, params)
    } catch { case e => warn("error while calling alert event api", e); BadRequest }
    case _ => ResponseString("Couldn't handle request")
      
  }
  
}
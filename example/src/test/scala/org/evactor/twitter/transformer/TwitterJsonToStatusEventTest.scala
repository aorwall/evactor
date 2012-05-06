/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.twitter.transformer

import org.junit.runner.RunWith
import akka.actor.Actor
import Actor._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.evactor.twitter.StatusEvent

@RunWith(classOf[JUnitRunner])
class TwitterJsonToStatusEventSpec(_system: ActorSystem) 
  extends TestKit(_system) with WordSpec with MustMatchers {
  
  def this() = this(ActorSystem("TwitterJsonToStatusEventSpec"))
  
  val twitterStatus = """
  {
    "favorited": false,
    "text": "test",
    "created_at": "Fri May 04 10:27:24 +0000 2012",
    "in_reply_to_status_id_str": null,
    "coordinates": null,
    "in_reply_to_user_id_str": null,
    "id_str": "198358157259571200",
    "in_reply_to_user_id": null,
    "in_reply_to_status_id": null,
    "retweeted": false,
    "in_reply_to_screen_name": null,
    "truncated": false,
    "contributors": null,
    "retweet_count": 0,
    "entities": {
        "user_mentions": [
            
        ],
        "urls": [
            
        ],
        "hashtags": [
            
        ]
    },
    "geo": null,
    "user": {
        "default_profile_image": true,
        "profile_use_background_image": true,
        "created_at": "Wed Feb 15 06:44:50 +0000 2012",
        "friends_count": 1,
        "screen_name": "foo",
        "follow_request_sent": null,
        "following": null,
        "time_zone": null,
        "favourites_count": 2,
        "profile_text_color": "333333",
        "followers_count": 560,
        "url": null,
        "verified": false,
        "notifications": null,
        "profile_link_color": "0084B4",
        "description": "",
        "id_str": "492874603",
        "location": "",
        "listed_count": 3,
        "profile_background_color": "C0DEED",
        "profile_background_tile": false,
        "default_profile": true,
        "contributors_enabled": false,
        "geo_enabled": false,
        "profile_sidebar_fill_color": "DDEEF6",
        "protected": false,
        "is_translator": false,
        "statuses_count": 4282,
        "name": "Asghar Ali",
        "lang": "en",
        "profile_sidebar_border_color": "C0DEED",
        "id": 492874603,
        "show_all_inline_media": false,
        "utc_offset": null
    },
    "id": 198358157259571200,
    "place": null
  }
  """
    
  "TwitterJsonToStatusEvent" must {

    "transform a json string representing a Twitter status to a StatusEvent object" in {
       val transformerRef = TestActorRef(new TwitterJsonToStatusEvent(testActor))
       val transformer = transformerRef.underlyingActor
       
       transformer.transform(twitterStatus)
       expectMsg(new StatusEvent("198358157259571200", 1336127244000L, "foo", "test", List(), List()))
    }
  }
  
  
}
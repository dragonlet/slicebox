/*
 * Copyright 2015 Lars Edenbrandt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.nimsa.sbx.metadata

import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.Config
import MetaDataProtocol._
import akka.actor.ActorSystem
import se.nimsa.sbx.app.GeneralProtocol._

class MetaDataServiceMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case msg: AddDataset  => 2 // low
      case msg: DeleteImage => 1 // low but higher than FileReceived
      case _                => 0 // normal
    })

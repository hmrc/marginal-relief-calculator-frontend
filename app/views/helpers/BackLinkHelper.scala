/*
 * Copyright 2023 HM Revenue & Customs
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

package views.helpers

import filters.BackLinkFilter
import models.{ CheckMode, Mode, NormalMode }
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

object BackLinkHelper {
  def backLinkOrDefault(defaultPath: String, mode: Mode)(implicit request: RequestHeader): String =
    mode match {
      case CheckMode =>
        controllers.routes.CheckYourAnswersController.onPageLoad().path()
      case NormalMode =>
        (request.session.get(BackLinkFilter.visitedLinks) match {
          case Some(value) =>
            val backLinks = Json.parse(value).as[List[String]]
            val tail = if (backLinks.nonEmpty) backLinks.tail else List()
            tail.headOption.getOrElse(defaultPath)
          case None => defaultPath
        }) + "?back=true"
    }
}

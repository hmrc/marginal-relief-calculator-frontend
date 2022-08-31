/*
 * Copyright 2022 HM Revenue & Customs
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

package filters

import akka.stream.Materializer
import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.http.HeaderNames
import play.api.mvc.{ Filter, RequestHeader, Result, Results }

import java.util.Base64
import scala.concurrent.Future

class BasicAuthFilter @Inject() (appConfig: FrontendAppConfig, override val mat: Materializer) extends Filter {

  private val unauthorized: Result =
    Results.Unauthorized.withHeaders(
      HeaderNames.WWW_AUTHENTICATE -> s"""Basic realm="${appConfig.basicAuthRealm.getOrElse("")}""""
    )

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (!rh.path.contains("/ping/ping") && appConfig.authEnabled) {
      rh.headers.get(HeaderNames.AUTHORIZATION) map { authHeader =>
        val (user, pass) = decodeBasicAuth(authHeader)
        if (appConfig.basicAuthUser.contains(user) && appConfig.basicAuthPassword.contains(pass)) {
          f(rh)
        } else {
          Future.successful(unauthorized)
        }
      } getOrElse Future.successful(unauthorized)
    } else {
      f(rh)
    }

  private[this] def decodeBasicAuth(authHeader: String): (String, String) = {
    val authBasicValue = authHeader.replaceFirst("Basic ", "")
    val decoded = Base64.getDecoder.decode(authBasicValue)
    val Array(user, password) = new String(decoded).split(":")
    (user, password)
  }
}

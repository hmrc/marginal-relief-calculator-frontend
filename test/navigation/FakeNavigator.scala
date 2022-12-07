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

package navigation

import connectors.MarginalReliefCalculatorConnector
import play.api.mvc.Call
import pages._
import models.{ Mode, UserAnswers }
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FakeNavigator(
  desiredRoute: Call,
  connector: MarginalReliefCalculatorConnector,
  sessionRepository: SessionRepository
) extends Navigator(connector, sessionRepository) {

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Call] =
    Future.successful(desiredRoute)
}

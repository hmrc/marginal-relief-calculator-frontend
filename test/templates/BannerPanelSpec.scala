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

package templates
import base.SpecBase
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout }
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.Panel
import views.html.templates.BannerPanel

class BannerPanelSpec extends SpecBase {

  "Banner Panel render" - {
    "Should render when content not empty" in {
      val bannerPanel = new BannerPanel()
      val panel = s"""<div class="govuk-panel govuk-panel--confirmation">
                            <h1 class="govuk-panel__body govuk-!-static-margin-bottom-3">
                             Marginal Relief for your accounting period is
                            </h1>
                            <div class="govuk-panel__title govuk-!-margin-0">
                              £2000
                            </div>
                        </div>"""

      val result = bannerPanel(
        Panel(
          title = Text("Marginal Relief for your accounting period is"),
          content = Text("£2000")
        )
      )

      contentAsString(result).filterAndTrim mustEqual panel.filterAndTrim
    }
    "Should render when content empty" in {
      val bannerPanel = new BannerPanel()
      val panel = s"""<div class="govuk-panel govuk-panel--confirmation">
                            <h1 class="govuk-panel__body govuk-!-static-margin-bottom-3">
                             Marginal Relief for your accounting period is
                            </h1>
                        </div>"""

      val result = bannerPanel(
        Panel(
          title = Text("Marginal Relief for your accounting period is")
        )
      )

      contentAsString(result).filterAndTrim mustEqual panel.filterAndTrim
    }
  }
}

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

package templates
import base.SpecBase
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout, running }
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.Panel
import views.html.templates.BannerPanel

class BannerPanelSpec extends SpecBase {

  "BannerPanel renders view" in {
    val application = applicationBuilder(None).build()

    running(application) {
      val bannerPanel = new BannerPanel()
      val panel = new Panel(title = Text("Marginal Relief for your accounting period is"), content = Text("£2000"));

      val result = bannerPanel(
        Panel(
          title = Text("Marginal Relief for your accounting period is"),
          content = Text("£2000")
        )
      )

      val view = application.injector.instanceOf[BannerPanel]

      contentAsString(result).filterAndTrim mustEqual view
        .render(panel)
        .toString
        .filterAndTrim
    }
  }
}

package controllers

import base.SpecBase
import com.fasterxml.jackson.databind.cfg.ConfigOverride
import config.FrontendAppConfig
import filters.BasicAuthFilterSpec.frontendAppConfig
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status
import play.api.test.Helpers.{redirectLocation, status}
import play.test.Helpers.fakeRequest


class ConfigOverride(config: Configuration) extends FrontendAppConfig(config: Configuration) {
  override val exitSurveyUrl: String = "/marginal-relief-calculator"
}

class FeedbackSurveyControllerSpec extends SpecBase with MockitoSugar {
  val mockController =  new FeedbackSurveyController(identify = ???, appConfig = ???, controllerComponents = ???){
    override implicit val appConfig = ConfigOverride
  }

  "Feedback Survey Controller" should {

    "Redirect to the ExitSurvey and clear a session" in {
      val result = mockController.redirectToExitSurvey()(fakeRequest)

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.exitSurveyUrl)
    }
  }

}

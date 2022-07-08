package pages

import models.Distribution
import pages.behaviours.PageBehaviours

class DistributionSpec extends PageBehaviours {

  "DistributionPage" - {

    beRetrievable[Distribution](DistributionPage)

    beSettable[Distribution](DistributionPage)

    beRemovable[Distribution](DistributionPage)
  }
}

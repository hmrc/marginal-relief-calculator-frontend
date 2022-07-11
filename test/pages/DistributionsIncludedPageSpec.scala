package pages

import models.DistributionsIncluded
import pages.behaviours.PageBehaviours

class DistributionsIncludedSpec extends PageBehaviours {

  "DistributionsIncludedPage" - {

    beRetrievable[DistributionsIncluded](DistributionsIncludedPage)

    beSettable[DistributionsIncluded](DistributionsIncludedPage)

    beRemovable[DistributionsIncluded](DistributionsIncludedPage)
  }
}

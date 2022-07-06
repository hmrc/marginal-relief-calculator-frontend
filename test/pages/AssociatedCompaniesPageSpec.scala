package pages

import models.AssociatedCompanies
import pages.behaviours.PageBehaviours

class AssociatedCompaniesSpec extends PageBehaviours {

  "AssociatedCompaniesPage" - {

    beRetrievable[AssociatedCompanies](AssociatedCompaniesPage)

    beSettable[AssociatedCompanies](AssociatedCompaniesPage)

    beRemovable[AssociatedCompanies](AssociatedCompaniesPage)
  }
}

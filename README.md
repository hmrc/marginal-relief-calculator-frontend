
# Marginal Relief Calculator (frontend)

This repository contains the necessary code to display user journey for Marginal Relief Calculator. It is dependendent on the [marginal-relief-calculator-backend](https://github.com/hmrc/marginal-relief-calculator-backend) service for calculator logic and marginal relief calculations.

The codebase uses the [scaffolding service](https://github.com/hmrc/hmrc-frontend-scaffold.g8) for generating pages in user journey. To add a new page, run the scaffolding sbt command with the appropriate template and run the migration script to add routes e.g `sbt g8Scaffold intPage`

## Running Marginal Relief Calculator (frontend) locally with Service Manager

```
sm --start MARGINAL_RELIEF_CALCULATOR
```

This starts Marginal Relief Calculator (frontend) and all its dependencies

## Starting the calculation journey (QA env)

The entry point for calculation is the [accounting period page](https://www.qa.tax.service.gov.uk/marginal-relief-calculator/accounting-period)

The subsequent pages in the journey ask user input for 

1. Taxable profit
2. Exempt distributions
3. Associated companies

Check your answers page shows a summary of the user entered data and allows any changes if required (via the change link). Once confirmed, marginal relief caclulation is triggered and the results page displays the result of the computation.

### Running the tests

    sbt test

### Running the tests with coverage

    sbt clean fmt coverage test it:test coverageReport

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

# Marginal Relief Calculator (frontend)

This repository contains the necessary code to display user journey for Marginal Relief Calculator.

The codebase uses the [scaffolding service](https://github.com/hmrc/hmrc-frontend-scaffold.g8) for generating pages in user journey. To add a new page, run the scaffolding sbt command with the appropriate template and run the migration script to add routes e.g `sbt g8Scaffold intPage`

## Running in DEV mode

To start the service locally using service manager, use `sm2 --start MARGINAL_RELIEF_CALCULATOR_ALL`
For a functioning frontend, it also needs following services to be running locally. Make sure `DATASTREAM` is started correctly using
```
sm2 --start DATASTREAM
```

### From source code on your local machine
Prior to starting the service from source, make sure the instance running in service manager is stopped. This can be done by running `sm2 --stop MARGINAL_RELIEF_CALCULATOR_FRONTEND`.

To run the service locally from source code, you need the following installed: `Java 11`, `Mongo`, `sbt`

```$ sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes ```

To debug the locally running application from IDE, use `jvm-debug` sbt option and run
```$ sbt run -jvm-debug 5005 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes ```

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


# agent-client-enrolments

## Summary


Agents and Traders require the capability to have their accounts and associated access suspended and terminated, the service is to fulfil this functionality.

## How to build and test

`sbt clean coverage test it/test coverageReport`

The test coverage report can be found in `$WORKSPACE/agent-client-enrolments/target/scala-3.7.4/scoverage-report/index.html`

### Automated testing
This service is tested by the following automated test repositories:
- [agent-client-enrolments-performance-tests](https://github.com/hmrc/agent-client-enrolments-performance-tests)

## Public API

| Path                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| [DELETE /agent-client-enrolments/agents/:ARN?terminationDate={termination Long}]() | Calling the endpoint will cause the agent to be deleted |
| [DELETE /agent-client-enrolments/relationships/:arn/service/:service/client/:clientIdType/:clientId]() | Following a VAT trader becoming insolvent, the relationship between the Trader and the Agent needs to be broken, so the Agent can no longer transact on behalf of the Insolvent Trader. This endpoint is to react to a trigger from ETMP and remove any agent-client relationships in EACD/Agent Services for in Insolvent client. As per Insolvency SDD.
 |


### DELETE /agent-client-enrolments/agents/:ARN?terminationDate={termination Long}

 - terminationDate is optional, in milliseconds. Defaults to the current time

Responds with:

| Status        | Message       |
|:-------------:|---------------|
| 200      | OK Request received and the attempt at deletion will be processed |
| 400      | Payload incorrect or insufficient for processing.|
| 401      | Unauthorised - the provided bearer token is either expired or not valid|
| 500      | Service error |

More details about this end point: https://confluence.tools.tax.service.gov.uk/display/TM/SI+-+Enrolment+Orchestrator


### DELETE /agent-client-enrolments/relationships/:arn/service/:service/client/:clientIdType/:clientId

Responds with:

| Status | Message                                                                                                                         |
|:------:|---------------------------------------------------------------------------------------------------------------------------------|
|  204   | Valid payload received. The attempt at deletion will be processed. Repeated calling with the same payload will always yield 204 |
|  400   | Invalid payload or payload data insufficient for processing                                                                     |
|  401   | Unauthorised - the provided bearer token is either expired or not valid                                                         |
|  50X   | Service error                                                                                                                   |

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

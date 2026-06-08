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

package uk.gov.hmrc.enrolmentsorchestrator.controllers

import org.scalatest.concurrent.Eventually
import play.api.Logger
import play.api.libs.ws.WSBodyReadables.readableAsString
import uk.gov.hmrc.enrolmentsorchestrator.connectors.{AgentClientRelationshipsConnector, EnrolmentsStoreConnector}
import uk.gov.hmrc.enrolmentsorchestrator.helpers._
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.filters.DefaultLoggingFilter

class AgentControllerISpec extends TestSetupHelper with AgentClientRelationshipsStubs with EnrolmentStoreStubs
  with AgentStatusChangeStubs with AuthStubs with LogCapturing with Eventually {

  "DELETE /enrolments-orchestrator/agents/:arn" should {

    "return 204" when {
      "Request received and the attempt at deletion will be processed" in {

        stubAuthorised
        agentStatusChangeReturnOK
        startESProxyWireMockServerFullHappyPath

        withCaptureOfLoggingFrom(Logger(classOf[DefaultLoggingFilter])) { logEvents =>
          await(
            wsClient
              .url(resource(s"$es9DeleteBaseUrl/$testARN"))
              .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
              .delete()
          ).status                                                                               shouldBe 204
          logEvents.length                                                                       shouldBe 1
          logEvents.head.toString.contains("DELETE /enrolments-orchestrator/agents/AARN123 204") shouldBe true
        }
      }

      """Request received but no groupId found by the arn. A logger.info about "may not actually exist" will fired""" in {

        stubAuthorised
        agentStatusChangeReturnOK
        startESProxyWireMockServerReturn204

        withCaptureOfLoggingFrom(Logger(classOf[EnrolmentsStoreService])) { logEvents =>
          await(
            wsClient
              .url(resource(s"$es9DeleteBaseUrl/$testARN"))
              .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
              .delete()
          ).status         shouldBe 204
          logEvents.length shouldBe 1
          logEvents.head.toString.contains(
            "For enrolmentKey: HMRC-AS-AGENT~AgentReferenceNumber~AARN123 200 was not returned by Enrolments-Store, " +
              "ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
              "or there is nothing to return, the response is 204 with body "
          ) shouldBe true
        }
      }
    }

    "return 401" when {

      """Request received but basic auth token not supplied. A logger.info about "response is 401" will fired""" in {
        stubAuthorised
        withCaptureOfLoggingFrom(Logger(classOf[DefaultLoggingFilter])) { logEvents =>
          val response = await(wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN")).delete())
          response.status                         shouldBe 401
          response.body.shouldBe("BasicAuthentication failed")
          logEvents.length                        shouldBe 1
          logEvents.head.toString.contains("401") shouldBe true
        }
      }

      """Request received but AgentStatusChange service return 401. A logger.info about "response is 401" will fired""" in {

        stubAuthorised
        agentStatusChangeReturn401

        withCaptureOfLoggingFrom(Logger(classOf[DefaultLoggingFilter])) { logEvents =>
          val response = await(
            wsClient
              .url(resource(s"$es9DeleteBaseUrl/$testARN"))
              .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
              .delete()
          )
          response.status                         shouldBe 401
          logEvents.length                        shouldBe 1
          logEvents.head.toString.contains("401") shouldBe true
        }
      }
    }

    "return 500" when {
      "An exception occurred by external services such as Connection refused" in {
        val response = await(
          wsClient
            .url(resource(s"$es9DeleteBaseUrl/$testARN"))
            .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
            .delete()
        )
        response.status shouldBe 500
      }
    }

  }

  "DELETE /enrolments-orchestrator/relationships/:arn/service/:service/client/:clientIdType/:clientId" should {
    "return 204" in {
      testEndpointToRemoveInsolventTraders("enrolments-orchestrator", 204)
    }
    "return 204 if ACR call returns error" in {
      testEndpointToRemoveInsolventTraders("enrolments-orchestrator", 404)
    }
  }

  "DELETE /agent-client-enrolments/relationships/:arn/service/:service/client/:clientIdType/:clientId" should {
    "return 204" in {
      testEndpointToRemoveInsolventTraders("agent-client-enrolments", 204)
    }
    "return 204 if ACR call returns error" in {
      testEndpointToRemoveInsolventTraders("enrolments-orchestrator", 404)
    }
  }

  private def testEndpointToRemoveInsolventTraders(endpointService: String, cleanUpInvitationResponseStatus: Int): Unit = {
    stubAuthorised
    startCleanUpInvitationStatus(cleanUpInvitationResponseStatus)
    startDeleteEnrolmentsForGroup

    val logger1 = Logger(classOf[EnrolmentsStoreConnector])
    val logger2 = Logger(classOf[AgentClientRelationshipsConnector])
    withCaptureOfLoggingFrom(logger1, logger2) { logEvents =>
      await(
        wsClient
          .url(resource(s"/$endpointService/relationships/ZARN1234567/service/HMRC-MTD-VAT/client/VRN/123456789"))
          .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
          .delete()
      ).status shouldBe 204

      eventually {
        logEvents.length shouldBe 3
        logEvents.map(_.getMessage) shouldBe List(
          s"PUT agent-client-relationships/cleanup-invitation-status for ARN ***567, clientId ***789, service HMRC-MTD-VAT returned $cleanUpInvitationResponseStatus",
          "[GG-5898] GET /enrolments/***789/groups returned 200",
          "[GG-5898] DELETE /groups/:groupId/enrolments/***789 returned 204"
        )
      }
    }
  }
}

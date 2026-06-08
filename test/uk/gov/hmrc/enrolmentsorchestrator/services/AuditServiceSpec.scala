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

package uk.gov.hmrc.enrolmentsorchestrator.services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends UnitSpec with MockitoSugar {

  "The auditing service" should {
    "send the correct audit event for an agent delete request" in new Setup {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(using any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditDeleteRequest(arn, timestamp)(using requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any, any)

      captor.getValue.auditSource shouldBe "agent-client-enrolments"
      captor.getValue.auditType   shouldBe "AgentDeleteRequest"
      captor.getValue.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate"      -> timestamp
      )
      captor.getValue.tags shouldBe Map(
        "clientIP"                   -> clientIp,
        "path"                       -> requestWithHeaders.path,
        HeaderNames.xSessionId       -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId       -> requestId,
        HeaderNames.deviceID         -> deviceId,
        "clientPort"                 -> clientPort,
        "transactionName"            -> "Agent Client Enrolments - Agent Delete Request"
      )
    }

    "send the correct audit event for a successful agent delete response" in new Setup {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(using any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditSuccessfulAgentDeleteResponse(arn, timestamp, 200)(using requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any, any)

      captor.getValue.auditSource shouldBe "agent-client-enrolments"
      captor.getValue.auditType   shouldBe "AgentDeleteResponse"
      captor.getValue.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate"      -> timestamp,
        "statusCode"           -> 200,
        "success"              -> true
      )
      captor.getValue.tags shouldBe Map(
        "clientIP"                   -> clientIp,
        "path"                       -> requestWithHeaders.path,
        HeaderNames.xSessionId       -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId       -> requestId,
        HeaderNames.deviceID         -> deviceId,
        "clientPort"                 -> clientPort,
        "transactionName"            -> "Agent Client Enrolments - Agent Delete Response"
      )
    }

    "send the correct audit event for a failed agent delete response" in new Setup {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(using any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditFailedAgentDeleteResponse(arn, timestamp, 400, "bad stuff happened")(using requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any, any)

      captor.getValue.auditSource shouldBe "agent-client-enrolments"
      captor.getValue.auditType   shouldBe "AgentDeleteResponse"
      captor.getValue.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate"      -> timestamp,
        "statusCode"           -> 400,
        "success"              -> false,
        "failureReason"        -> "bad stuff happened"
      )
      captor.getValue.tags shouldBe Map(
        "clientIP"                   -> clientIp,
        "path"                       -> requestWithHeaders.path,
        HeaderNames.xSessionId       -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId       -> requestId,
        HeaderNames.deviceID         -> deviceId,
        "clientPort"                 -> clientPort,
        "transactionName"            -> "Agent Client Enrolments - Agent Delete Response"
      )
    }

    "send the correct audit event for a successful agent client relationship delete response" in new Setup {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val arn = "agent ref no"
      val service = "service"
      val clientIdType = "clientIdType"
      val clientId = "clientId"
      val failureReason = "OK"

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(using any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditClientDeleteResponse(arn, service, clientIdType, clientId, true, 200, failureReason)(using requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any, any)

      captor.getValue.auditSource shouldBe "agent-client-enrolments"
      captor.getValue.auditType   shouldBe "AgentClientDeleteRequest"
      captor.getValue.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "service"              -> service,
        "clientIdType"         -> clientIdType,
        "clientId"             -> clientId,
        "success"              -> true,
        "responseCode"         -> 200,
        "failureReason"        -> failureReason
      )
      captor.getValue.tags shouldBe Map(
        "clientIP"                   -> clientIp,
        "path"                       -> requestWithHeaders.path,
        HeaderNames.xSessionId       -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId       -> requestId,
        HeaderNames.deviceID         -> deviceId,
        "clientPort"                 -> clientPort,
        "transactionName" -> "Agent Client Enrolments - Agent Client Relationship Delete Request; example: insolvent trader needs decoupling from an Agent"
      )
    }

    "send the correct audit event for a failed agent client relationship delete response" in new Setup {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val arn = "agent ref no"
      val service = "service"
      val clientIdType = "clientIdType"
      val clientId = "clientId"
      val failureReason = "error"

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(using any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditClientDeleteResponse(arn, service, clientIdType, clientId, false, 500, failureReason)(using requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any, any)

      captor.getValue.auditSource shouldBe "agent-client-enrolments"
      captor.getValue.auditType   shouldBe "AgentClientDeleteRequest"
      captor.getValue.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "service"              -> service,
        "clientIdType"         -> clientIdType,
        "clientId"             -> clientId,
        "success"              -> false,
        "responseCode"         -> 500,
        "failureReason"        -> failureReason
      )
      captor.getValue.tags shouldBe Map(
        "clientIP"                   -> clientIp,
        "path"                       -> requestWithHeaders.path,
        HeaderNames.xSessionId       -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId       -> requestId,
        HeaderNames.deviceID         -> deviceId,
        "clientPort"                 -> clientPort,
        "transactionName" -> "Agent Client Enrolments - Agent Client Relationship Delete Request; example: insolvent trader needs decoupling from an Agent"
      )
    }

  }

  trait Setup {
    val userIdentifier = "somebody"
    val clientIp = "192.168.0.1"
    val clientPort = "443"
    val clientReputation = "totally reputable"
    val requestId = "requestId"
    val deviceId = "deviceId"
    val sessionId = "sessionId"
    val trustId = "someTrustId"

    val requestWithHeaders: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      .withHeaders(HeaderNames.trueClientIp -> clientIp)
      .withHeaders(HeaderNames.trueClientPort -> clientPort)
      .withHeaders(HeaderNames.akamaiReputation -> clientReputation)
      .withHeaders(HeaderNames.xRequestId -> requestId)
      .withHeaders(HeaderNames.deviceID -> deviceId)
      .withHeaders(HeaderNames.xSessionId -> sessionId)

    val mockAuditConnector: AuditConnector = mock[AuditConnector]

    val auditService = new AuditService(mockAuditConnector)(using ExecutionContext.global)
  }
}

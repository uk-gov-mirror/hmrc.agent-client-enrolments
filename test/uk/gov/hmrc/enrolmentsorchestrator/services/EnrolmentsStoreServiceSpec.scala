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

import org.mockito.ArgumentMatchers.{any, contains}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.connectors.{AgentClientRelationshipsConnector, EnrolmentsStoreConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.enrolmentsorchestrator.models.{DelegatedGroupIds, PrincipalGroupIds}
import uk.gov.hmrc.enrolmentsorchestrator.models.EnrolmentGroupIds._
import uk.gov.hmrc.enrolmentsorchestrator.{LogCapturing, UnitSpec}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentsStoreServiceSpec extends UnitSpec with LogCapturing with MockitoSugar {

  val mockEnrolmentsStoreConnector: EnrolmentsStoreConnector = mock[EnrolmentsStoreConnector]
  val mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnector]

  val mockAgentClientRelationshipsConnector: AgentClientRelationshipsConnector = mock[AgentClientRelationshipsConnector]

  val enrolmentsStoreService =
    new EnrolmentsStoreService(mockEnrolmentsStoreConnector, mockTaxEnrolmentConnector, mockAgentClientRelationshipsConnector)

  val enrolmentKey = "enrolmentKey"
  val groupId = "groupId"

  "EnrolmentsStoreService" should {

    "return 200 HttpResponse if EnrolmentsStoreConnector returns 200 and TaxEnrolmentConnector returns 204" in {
      withCaptureOfLoggingFrom(Logger(classOf[EnrolmentsStoreService])) { logEvents =>

        val enrolmentsStoreHttpResponse = HttpResponse(200, json = Json.toJson(PrincipalGroupIds(List(groupId))), Map.empty)
        val taxEnrolmentHttpResponse = HttpResponse(204, "")

        when(mockEnrolmentsStoreConnector.es1GetPrincipalGroups(contains(enrolmentKey))(using any))
          .thenReturn(Future.successful(enrolmentsStoreHttpResponse))
        when(mockTaxEnrolmentConnector.es9DeallocateGroup(contains(groupId), contains(enrolmentKey))(using any, any))
          .thenReturn(Future.successful(taxEnrolmentHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe taxEnrolmentHttpResponse

        logEvents.length shouldBe 0

      }
    }

    "return HttpResponse from EnrolmentsStoreConnector if EnrolmentsStoreConnector returns not ok(200) and log the response" in {
      withCaptureOfLoggingFrom(Logger(classOf[EnrolmentsStoreService])) { logEvents =>

        val testHttpResponse = HttpResponse(204, "")
        val enrolmentKey = "enrolmentKey"

        when(mockEnrolmentsStoreConnector.es1GetPrincipalGroups(contains(enrolmentKey))(using any))
          .thenReturn(Future.successful(testHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe testHttpResponse

        logEvents.length shouldBe 1
        logEvents.collectFirst { case logEvent =>
          logEvent.getMessage shouldBe "For enrolmentKey: enrolmentKey 200 was not returned by Enrolments-Store, " +
            "ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
            "or there is nothing to return, the response is 204 with body "
        }

      }
    }

    "return HttpResponse from TaxEnrolmentConnector if EnrolmentsStoreConnector returns 200 but TaxEnrolmentConnector not returns 204 and log the response" in {
      withCaptureOfLoggingFrom(Logger(classOf[EnrolmentsStoreService])) { logEvents =>

        val enrolmentsStoreHttpResponse = HttpResponse(200, json = Json.toJson(PrincipalGroupIds(List(groupId))), Map.empty)
        val taxEnrolmentHttpResponse = HttpResponse(400, "")

        when(mockEnrolmentsStoreConnector.es1GetPrincipalGroups(contains(enrolmentKey))(using any))
          .thenReturn(Future.successful(enrolmentsStoreHttpResponse))
        when(mockTaxEnrolmentConnector.es9DeallocateGroup(contains(groupId), contains(enrolmentKey))(using any, any))
          .thenReturn(Future.successful(taxEnrolmentHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe taxEnrolmentHttpResponse

        logEvents.length shouldBe 1
        logEvents.collectFirst { case logEvent =>
          logEvent.getMessage shouldBe s"For enrolmentKey: $enrolmentKey and groupId: $groupId 204 was not returned by Tax-Enrolments, " +
            s"the response is 400 with body "
        }

      }
    }
  }

  "deleteEnrolments" should {
    "return ok when all downstreams return ok " in {
      when(mockAgentClientRelationshipsConnector.cleanupInvitationStatus(any, any, any)(using any))
        .thenReturn(Future.successful(HttpResponse(204, "")))
      when(mockEnrolmentsStoreConnector.es1GetDelegatedGroups(any)(using any, any)).thenReturn(Future.successful(DelegatedGroupIds(Nil)))
      when(mockEnrolmentsStoreConnector.es9DeallocateDelegatedEnrolment(any, any)(using any)).thenReturn(Future.successful(HttpResponse(204, "")))
      await(enrolmentsStoreService.deleteEnrolments("ZARN1234567", "HMRC-MTD-VAT", "VRN", "123456789")) shouldBe ((): Unit)
    }

    "return ok when downstream AgentClientAuthorisationConnector fails " in {
      when(mockAgentClientRelationshipsConnector.cleanupInvitationStatus(any, any, any)(using any)).thenReturn(Future.failed(new Throwable))
      when(mockEnrolmentsStoreConnector.es1GetDelegatedGroups(any)(using any, any)).thenReturn(Future.successful(DelegatedGroupIds(Nil)))
      when(mockEnrolmentsStoreConnector.es9DeallocateDelegatedEnrolment(any, any)(using any)).thenReturn(Future.successful(HttpResponse(204, "")))
      await(enrolmentsStoreService.deleteEnrolments("ZARN1234567", "HMRC-MTD-VAT", "VRN", "123456789")) shouldBe ((): Unit)
    }

    "return ok when downstream AgentClientAuthorisationConnector returns 404 " in {
      when(mockAgentClientRelationshipsConnector.cleanupInvitationStatus(any, any, any)(using any))
        .thenReturn(Future.successful(HttpResponse(404, "")))
      when(mockEnrolmentsStoreConnector.es1GetDelegatedGroups(any)(using any, any)).thenReturn(Future.successful(DelegatedGroupIds(Nil)))
      when(mockEnrolmentsStoreConnector.es9DeallocateDelegatedEnrolment(any, any)(using any)).thenReturn(Future.successful(HttpResponse(204, "")))
      await(enrolmentsStoreService.deleteEnrolments("ZARN1234567", "HMRC-MTD-VAT", "VRN", "123456789")) shouldBe ((): Unit)
    }

    "return ok when downstream EnrolmentsStoreConnector fails " in {
      when(mockEnrolmentsStoreConnector.es1GetDelegatedGroups(any)(using any, any)).thenReturn(Future.successful(DelegatedGroupIds(Nil)))
      when(mockEnrolmentsStoreConnector.es1GetDelegatedGroups(any)(using any, any)).thenReturn(Future.successful(DelegatedGroupIds(Nil)))
      when(mockEnrolmentsStoreConnector.es9DeallocateDelegatedEnrolment(any, any)(using any)).thenReturn(Future.failed(new Throwable))
      await(enrolmentsStoreService.deleteEnrolments("ZARN1234567", "HMRC-MTD-VAT", "VRN", "123456789")) shouldBe ((): Unit)
    }
  }

}

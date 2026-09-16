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

import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.enrolmentsorchestrator.connectors.*
import uk.gov.hmrc.enrolmentsorchestrator.connectors.ConnectorUtils.hashString
import uk.gov.hmrc.enrolmentsorchestrator.models.EnrolmentGroupIds.*
import uk.gov.hmrc.enrolmentsorchestrator.models.PrincipalGroupIds
import uk.gov.hmrc.enrolmentsorchestrator.utilities.RequestAwareLogging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EnrolmentsStoreService @Inject() (
  enrolmentsStoreConnector: EnrolmentsStoreConnector,
  taxEnrolmentConnector: TaxEnrolmentConnector,
  agentClientRelationshipsConnector: AgentClientRelationshipsConnector
)(using ec: ExecutionContext)
    extends RequestAwareLogging {

  def terminationByEnrolmentKey(enrolmentKey: String)(using requestHeader: RequestHeader): Future[HttpResponse] = {
    enrolmentsStoreConnector.es1GetPrincipalGroups(enrolmentKey).flatMap { response =>
      response.status match {
        case 200 =>
          val groupId = Json.parse(response.body).validate[PrincipalGroupIds].get.principalGroupIds.head
          taxEnrolmentConnector
            .es9DeallocateGroup(groupId, enrolmentKey)
            .map { teResponse =>
              if (teResponse.status != NO_CONTENT)
                logger.warn(
                  s"For enrolmentKey: $enrolmentKey and groupId: $groupId $NO_CONTENT was not returned by Tax-Enrolments, " +
                    s"the response is ${teResponse.status} with body ${teResponse.body}"
                )
              teResponse
            }
            .recover {
              case UpstreamErrorResponse.Upstream4xxResponse(e) =>
                logger.warn(
                  s"For enrolmentKey: $enrolmentKey and groupId: $groupId 204 was not returned by Tax-Enrolments, " +
                    s"the response is ${e.statusCode} with body ${e.message}"
                )
                throw e
              case ex: Exception => throw ex
            }
        case _ =>
          logger.warn(
            s"For enrolmentKey: $enrolmentKey 200 was not returned by Enrolments-Store," +
              s" ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
              s"or there is nothing to return, the response is ${response.status} with body ${response.body}"
          )
          Future.successful(response)
      }
    }
  }

  def deleteEnrolments(arn: String, service: String, clientIdType: String, clientId: String)(using
    requestHeader: RequestHeader
  ): Future[Unit] = {
    val enrolmentKey = s"$service~$clientIdType~$clientId"

    (for {
      _        <- agentClientRelationshipsConnector.cleanupInvitationStatus(arn, service, clientId)
      groupIds <- enrolmentsStoreConnector.es1GetDelegatedGroups(enrolmentKey)
      _ = groupIds.delegatedGroupIds.map(groupId => enrolmentsStoreConnector.es9DeallocateDelegatedEnrolment(groupId, enrolmentKey))
    } yield ())
      .recover { case _ =>
        logger.error(s"Delete insolvent traders failed for enrolmentKey: ${hashString(enrolmentKey)}")
      }

    Future.successful(())
  }
}

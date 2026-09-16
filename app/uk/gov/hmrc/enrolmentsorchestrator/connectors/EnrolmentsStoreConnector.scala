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

package uk.gov.hmrc.enrolmentsorchestrator.connectors

import play.api.http.HttpVerbs
import play.api.mvc.RequestHeader
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.ConnectorUtils.hashString
import uk.gov.hmrc.enrolmentsorchestrator.models.DelegatedGroupIds
import uk.gov.hmrc.enrolmentsorchestrator.utilities.RequestAwareLogging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import uk.gov.hmrc.enrolmentsorchestrator.utilities.RequestSupport.hc

@Singleton()
class EnrolmentsStoreConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(using ec: ExecutionContext) extends RequestAwareLogging {

  lazy val enrolmentsStoreBaseUrl: String = appConfig.enrolmentsStoreBaseUrl

  // Query Groups who have an allocated Enrolment
  def es1GetPrincipalGroups(enrolmentKey: String)(using requestHeader: RequestHeader): Future[HttpResponse] = {
    val requestUrl = s"$enrolmentsStoreBaseUrl/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=principal"
    httpClient.get(url"$requestUrl").execute[HttpResponse]
  }

  def assignEnrolment(credId: String, enrolmentKey: String)(using hc: HeaderCarrier): Future[Unit] = {
    val requestUrl = s"$enrolmentsStoreBaseUrl/enrolment-store-proxy/enrolment-store/users/$credId/enrolments/$enrolmentKey"
    httpClient.post(url"$requestUrl").execute[HttpResponse].map(_ => ())
  }

  def es1GetDelegatedGroups(
    enrolmentKey: String
  )(using
    rds: HttpReads[DelegatedGroupIds],
    requestHeader: RequestHeader
  ): Future[DelegatedGroupIds] = {
    val requestUrl = s"$enrolmentsStoreBaseUrl/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=delegated"
    httpClient
      .get(url"$requestUrl")
      .execute[HttpResponse]
      .andThen {
        case Success(response) => logger.info(s"[GG-5898] GET /enrolments/${hashString(enrolmentKey)}/groups returned ${response.status}")
        case Failure(_)        => logger.error(s"[GG-5898] GET /enrolments/${hashString(enrolmentKey)}/groups failed")
      }
      .map(rds.read(HttpVerbs.POST, requestUrl, _))
  }

  def es9DeallocateDelegatedEnrolment(groupId: String, enrolmentKey: String)(using
    requestHeader: RequestHeader
  ): Future[HttpResponse] = {
    val requestUrl =
      s"$enrolmentsStoreBaseUrl/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey?keepAgentAllocations=false"
    httpClient
      .delete(url"$requestUrl")
      .execute[HttpResponse]
      .andThen {
        case Success(response) => logger.info(s"[GG-5898] DELETE /groups/:groupId/enrolments/${hashString(enrolmentKey)} returned ${response.status}")
        case Failure(_)        => logger.error(s"[GG-5898] DELETE /groups/:groupId/enrolments/${hashString(enrolmentKey)} failed")
      }
  }

}

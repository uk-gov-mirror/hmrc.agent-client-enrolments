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

import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.ConnectorUtils.hashString
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AgentClientRelationshipsConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {
  lazy val baseUrl: String = appConfig.agentClientRelationshipsBaseUrl

  def cleanupInvitationStatus(arn: String, service: String, clientId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient
      .put(url"$baseUrl/agent-client-relationships/cleanup-invitation-status")
      .withBody(Json.toJson(Map("arn" -> arn, "clientId" -> clientId, "service" -> service)))
      .execute[HttpResponse]
      .map { response =>
        logger.info(
          s"PUT agent-client-relationships/cleanup-invitation-status for ARN ${hashString(arn)}, clientId ${hashString(clientId)}, service $service returned ${response.status}"
        )
        response
      }
  }
}

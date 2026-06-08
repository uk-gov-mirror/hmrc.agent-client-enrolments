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

import play.api.mvc._
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AgentStatusChangeConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.BasicAuthentication
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, AuthService, EnrolmentsStoreService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AgentController @Inject() (
  appConfig: AppConfig,
  auditService: AuditService,
  authService: AuthService,
  enrolmentsStoreService: EnrolmentsStoreService,
  agentStatusChangeConnector: AgentStatusChangeConnector,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  // more details about this end point: https://confluence.tools.tax.service.gov.uk/display/CIP/SI+-+Agent+Client+Enrolments
  def deleteByARN(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>
    val tDate = terminationDate.getOrElse(Instant.now.toEpochMilli)
    val enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"

    auditService.auditDeleteRequest(arn, tDate)

    validateBasicAuth(request.headers, appConfig.expectedAuth)
      .fold {
        auditService.auditFailedAgentDeleteResponse(arn, tDate, 401, "BasicAuthentication failed")
        Future.successful(Unauthorized(s"BasicAuthentication failed"))
      }(basicAuth =>
        callAgentStatusChangeToTerminate(arn, tDate)(
          continueES9(basicAuth, arn, tDate, enrolmentKey)
        )
      )

  }

  private def callAgentStatusChangeToTerminate(arn: String, tDate: Long)(
    continueES9: => Future[Result]
  )(implicit request: Request[?]): Future[Result] = {
    agentStatusChangeConnector
      .agentStatusChangeToTerminate(arn)
      .flatMap { agentStatusChangeRes =>
        if (agentStatusChangeRes.status == 200) continueES9
        else {
          auditService.auditFailedAgentDeleteResponse(
            arn,
            tDate,
            agentStatusChangeRes.status,
            agentStatusChangeRes.body
          )
          Future.failed(UpstreamErrorResponse(agentStatusChangeRes.body, agentStatusChangeRes.status))
        }
      }
      .recover { case ex => handleRecover(ex, arn, tDate, request) }
  }

  private def continueES9(basicAuth: BasicAuthentication, arn: String, tDate: Long, enrolmentKey: String)(implicit
    request: Request[?]
  ): Future[Result] = {
    authService.createBearerToken(basicAuth).flatMap { bearerToken =>
      implicit val newHeaderCarrier: HeaderCarrier = HeaderCarrier(authorization = bearerToken)
      enrolmentsStoreService
        .terminationByEnrolmentKey(enrolmentKey)(using newHeaderCarrier)
        .map { res =>
          if (res.status == 204) {
            auditService.auditSuccessfulAgentDeleteResponse(arn, tDate, res.status)(using request)
            Status(NO_CONTENT)
          } else {
            auditService.auditFailedAgentDeleteResponse(arn, tDate, res.status, res.body)(using request)
            new Status(res.status)(res.body)
          }
        }
        .recover { case ex => handleRecover(ex, arn, tDate, request) }
    }
  }

  private def handleRecover(exception: Throwable, arn: String, tDate: Long, request: Request[?]): Result = {
    exception match {
      case UpstreamErrorResponse(message, code, _, _) if code != 404 =>
        auditService.auditFailedAgentDeleteResponse(arn, tDate, code, message)(using request)
        new Status(code)(s"$message")
      case _ =>
        auditService.auditFailedAgentDeleteResponse(arn, tDate, 500, "Internal service error")(using request)
        InternalServerError("Internal service error")
    }
  }

  private def validateBasicAuth(headers: Headers, expectedAuth: BasicAuthentication): Option[BasicAuthentication] = {
    authService
      .getBasicAuth(headers)
      .filter(_ == expectedAuth)
  }

  def deleteInsolventTraders(arn: String, service: String, clientIdType: String, clientId: String): Action[AnyContent] = Action.async {
    implicit request =>
      validateBasicAuth(request.headers, appConfig.expectedAuth)
        .fold {
          auditService.auditClientDeleteResponse(arn,
                                                 service,
                                                 clientIdType,
                                                 clientId,
                                                 false,
                                                 401,
                                                 "Unauthorised - the provided bearer token is either expired or not valid"
                                                )
          Future.successful(Unauthorized)
        }(basicAuth =>
          (for {
            bearerToken <- authService.createBearerToken(basicAuth)
            newHeaderCarrier: HeaderCarrier = HeaderCarrier(authorization = bearerToken)
            _ <- enrolmentsStoreService.deleteEnrolments(arn, service, clientIdType, clientId)(using newHeaderCarrier)
            _ = auditService.auditClientDeleteResponse(arn,
                                                       service,
                                                       clientIdType,
                                                       clientId,
                                                       true,
                                                       204,
                                                       "No Content Request received and the attempt at deletion will be processed"
                                                      )
          } yield Status(NO_CONTENT))
            .recover { case _ =>
              auditService.auditClientDeleteResponse(arn, service, clientIdType, clientId, false, 500, "Internal Server Error")
              InternalServerError
            }
        )
  }
}

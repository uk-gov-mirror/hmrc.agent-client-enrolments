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

import play.api.{Configuration, Logging}
import uk.gov.hmrc.enrolmentsorchestrator.connectors.EnrolmentsStoreConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataCorrectionService @Inject() (
  enrolmentStore: EnrolmentsStoreConnector,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends Logging {

  private val config = configuration.get[Configuration]("oneOffDataCorrection")
  private val enabled = config.get[Boolean]("enabled")
  private val corrections = config.get[Seq[Configuration]]("data")

  if (enabled) {
    applyCorrections()
  } else {
    logger.info("[GG-5119] data correction task disabled")
  }

  private def applyCorrections(): Future[Unit] = Future
    .sequence {
      corrections.map { correction =>
        val credId = correction.get[String]("credId")
        val enrolmentKey = correction.get[String]("enrolmentKey")

        logger.info(s"[GG-5119] Applying enrolment $enrolmentKey to cred $credId")
        enrolmentStore
          .assignEnrolment(credId, enrolmentKey)(using HeaderCarrier())
          .map { _ =>
            logger.info(s"[GG-5119] Successfully applied enrolment $enrolmentKey to cred $credId")
          }
          .recover { case e =>
            logger.error(s"[GG-5119] Failed to apply enrolment $enrolmentKey to cred $credId", e)
          }
      }
    }
    .map(_ => ())

}

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

package uk.gov.hmrc.enrolmentsorchestrator

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AuthConnector
import uk.gov.hmrc.enrolmentsorchestrator.services.AuthService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

trait AuthHelper extends MockitoSugar {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authService: AuthService = new AuthService(mockAuthConnector)

  val testHttpResponse = HttpResponse(200, "", headers = Map(AUTHORIZATION -> Seq("BEARER AUTHORIZATION")))
  when(mockAuthConnector.createBearerToken(any)(using any, any))
    .thenReturn(Future.successful(testHttpResponse))

}

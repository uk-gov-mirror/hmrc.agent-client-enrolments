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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsStoreConnectorSpec extends UnitSpec with MockitoSugar {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockAppConfig: AppConfig = mock[AppConfig]

  val connector = new EnrolmentsStoreConnector(mockHttpClient, mockAppConfig)

  "EnrolmentsStoreConnector" should {
    "connect to EnrolmentsStore and return HttpResponse" in {
      val testHttpResponse = HttpResponse(200, "")
      val enrolmentKey = "enrolmentKey"
      when(mockAppConfig.enrolmentsStoreBaseUrl).thenReturn("http://localhost:1111")
      when(mockHttpClient.get(any[URL])(using any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any[HttpReads[HttpResponse]], any[ExecutionContext])).thenReturn(Future.successful(testHttpResponse))
      await(connector.es1GetPrincipalGroups(enrolmentKey)) shouldBe testHttpResponse
    }
  }

}

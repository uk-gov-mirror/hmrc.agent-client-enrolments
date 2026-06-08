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
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.JsValue
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthConnectorSpec extends UnitSpec with MockitoSugar {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockAppConfig: AppConfig = mock[AppConfig]

  val connector = new AuthConnector(mockHttpClient, mockAppConfig)

  "AuthConnector" should {
    "connect to auth to create session" in {
      val testHttpResponse = HttpResponse(200, "", headers = Map(AUTHORIZATION -> Seq(AUTHORIZATION)))
      when(mockAppConfig.authBaseUrl).thenReturn("http://localhost:1111")
      when(mockHttpClient.post(any[URL])(using any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody[JsValue](any[JsValue])(using any, any, any[ExecutionContext])).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any[HttpReads[HttpResponse]], any[ExecutionContext])).thenReturn(Future.successful(testHttpResponse))
      await(connector.createBearerToken("applicationName")).header(AUTHORIZATION) shouldBe Some(AUTHORIZATION)
    }
  }
}

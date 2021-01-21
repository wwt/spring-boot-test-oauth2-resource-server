package com.example.demo

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class CustomerControllerTests {
    companion object {
        private val rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
        private val subject = UUID.randomUUID().toString()
        private var jwsBuilder = JWSBuilder().subject(subject)

        @BeforeAll
        @JvmStatic
        fun setUp() {
            rsaJsonWebKey.apply {
                keyId = "k1"
                algorithm = AlgorithmIdentifiers.RSA_USING_SHA256
                use = "sig"
            }

            jwsBuilder.rsaJsonWebKey(rsaJsonWebKey)
        }
    }

    @Value("\${wiremock.server.baseUrl}")
    private lateinit var wireMockServerBaseUrl: String

    @BeforeEach
    fun init() {
        jwsBuilder.issuer(wireMockServerBaseUrl)

        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonWebKeySet(rsaJsonWebKey).toJson())
                )
        )
    }

    @Test
    fun `should be unauthorized without bearer token`(@Autowired restTemplate: TestRestTemplate) {
        val response = restTemplate.getForEntity<List<Customer>>("/customers")

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should be able to fetch customers with valid bearer token`(@Autowired restTemplate: TestRestTemplate) {
        val token = jwsBuilder.build().compactSerialization
        val request = RequestEntity
            .get("/customers")
            .headers { it.setBearerAuth(token) }
            .build()

        val customers = restTemplate.exchange<List<Customer>>(
            url = "/customers",
            requestEntity = request,
            method = HttpMethod.GET
        ).body

        assertThat(customers)
            .isNotNull
            .containsExactly(Customer("always right"))
    }
}
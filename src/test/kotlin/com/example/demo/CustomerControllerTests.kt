package com.example.demo

import com.github.tomakehurst.wiremock.client.WireMock
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.CollectionUtils.toMultiValueMap
import java.net.URI
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

        inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
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
    fun getCustomersUnauthorized(@Autowired restTemplate: TestRestTemplate) {
        val response = restTemplate.exchange("/customers", HttpMethod.GET, null, List::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun postAndGetCustomers(@Autowired restTemplate: TestRestTemplate) {
        val headers = jwsBuilder.build().let {
            toMultiValueMap(mapOf("Authorization" to listOf(String.format("Bearer %s", it.compactSerialization))))
        }

        val request = RequestEntity<Any>(headers, HttpMethod.GET, URI.create("/customers"))
        val customers = restTemplate.exchange(request, typeRef<List<Customer>>()).let {
            it.body!!
        }

        assertEquals("always right", customers[0].name)
    }
}
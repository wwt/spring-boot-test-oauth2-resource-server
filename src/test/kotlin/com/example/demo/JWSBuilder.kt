package com.example.demo

import org.jose4j.jwk.RsaJsonWebKey
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import java.util.UUID

data class JWSBuilder(
    var rsaJsonWebKey: RsaJsonWebKey? = null,
    var issuer: String? = null,
    var subject: String? = null) {

    fun rsaJsonWebKey(rsaJsonWebKey: RsaJsonWebKey) = apply { this.rsaJsonWebKey = rsaJsonWebKey }
    fun issuer(issuer: String) = apply { this.issuer = issuer }
    fun subject(subject: String) = apply { this.subject = subject }

    fun build(): JsonWebSignature {
        // Create the Claims, which will be the content of the JWT
        val claims = JwtClaims()
        claims.setJwtId(UUID.randomUUID().toString()) // a unique identifier for the token
        claims.setExpirationTimeMinutesInTheFuture(10F) // time when the token will expire (10 minutes from now)
        claims.setIssuedAtToNow() // when the token was issued/created (now)
        claims.setAudience("https://host/api") // to whom this token is intended to be sent
        claims.setIssuer(issuer) // who creates the token and signs it
        claims.setSubject(subject) // the subject/principal is whom the token is about
        claims.setClaim("azp", "example-client-id") // Authorized party  (the party to which this token was issued)
        claims.setClaim("scope", "openid profile email")

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS so we create a JsonWebSignature object.
        val jws = JsonWebSignature()

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson())

        // The JWT is signed using the private key
        jws.setKey(rsaJsonWebKey?.getPrivateKey())

        // Set the Key ID (kid) header because it's just the polite thing to do.
        // We only have one key in this example but a using a Key ID helps
        // facilitate a smooth key rollover process
        jws.setKeyIdHeaderValue(rsaJsonWebKey?.getKeyId())

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256)

        // set the type header
        jws.setHeader("typ", "JWT")

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        return jws
    }
}
package com.example.demo

import org.jose4j.jwk.RsaJsonWebKey
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import java.util.UUID

data class JWSBuilder(
    var rsaJsonWebKey: RsaJsonWebKey? = null,
    var claimsIssuer: String? = null,
    var claimsSubject: String? = null) {

    fun rsaJsonWebKey(rsaJsonWebKey: RsaJsonWebKey) = apply { this.rsaJsonWebKey = rsaJsonWebKey }
    fun issuer(issuer: String) = apply { this.claimsIssuer = issuer }
    fun subject(subject: String) = apply { this.claimsSubject = subject }

    fun build(): JsonWebSignature {
        // The JWT Claims Set represents a JSON object whose members are the claims conveyed by the JWT.
        val claims = JwtClaims().apply {
            jwtId = UUID.randomUUID().toString() // unique identifier for the JWT
            issuer = claimsIssuer // identifies the principal that issued the JWT
            subject = claimsSubject // identifies the principal that is the subject of the JWT
            setAudience("https://host/api") // identifies the recipients that the JWT is intended for
            setExpirationTimeMinutesInTheFuture(10F) // identifies the expiration time on or after which the JWT MUST NOT be accepted for processing
            setIssuedAtToNow() // identifies the time at which the JWT was issued
            setClaim("azp", "example-client-id") // Authorized party - the party to which the ID Token was issued
            setClaim("scope", "openid profile email") // Scope Values
        }

        val jws = JsonWebSignature().apply {
            payload = claims.toJson()
            key = rsaJsonWebKey?.getPrivateKey() // the key to sign the JWS with
            algorithmHeaderValue = rsaJsonWebKey?.algorithm // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
            keyIdHeaderValue = rsaJsonWebKey?.getKeyId() // a hint indicating which key was used to secure the JWS
            setHeader("typ", "JWT") // the media type of this JWS
        }

        return jws
    }
}

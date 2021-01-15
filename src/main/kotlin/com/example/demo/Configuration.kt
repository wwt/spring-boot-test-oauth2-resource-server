package com.example.demo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration(proxyBeanMethods = false)
class Configuration {
    @Bean
    fun securityConfigurer(): WebSecurityConfigurerAdapter {
        return object: WebSecurityConfigurerAdapter() {
            override fun configure(http: HttpSecurity) {
                http.authorizeRequests { authz ->
                    authz
                        .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                }
                    .oauth2ResourceServer { oauth2 -> oauth2.jwt() }
            }
        }
    }
}
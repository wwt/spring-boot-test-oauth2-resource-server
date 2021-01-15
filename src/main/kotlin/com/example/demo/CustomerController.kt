package com.example.demo

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers")
class CustomerController {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCustomers(): List<Customer> = listOf(Customer("always right"))
}

data class Customer(
    val name: String
)
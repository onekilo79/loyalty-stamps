package com.flux.test

import com.flux.test.model.*
import com.flux.test.repo.AccountRepo
import com.flux.test.service.LoyaltyService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.kotlintest.IsolationMode
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.mockito.Mock
import java.util.UUID

class LoyaltySpec : StringSpec() {


    val implementation: ImplementMe = LoyaltyService(schemes, AccountRepo())

    init {

        "Flatten Items"{
            val response = LoyaltyService.flattenItems(listOfItems)
            response shouldHaveSize 3
            response shouldBe listOf(200L, 200L, 100L)

        }
        "How many stamps Acquire"{
            val response = LoyaltyService.acquiredStamps(listOfItems)
            response shouldBe 3
        }
        "CurrentStampCount should be zero for new account"{
            val accountSchema = AccountSchema(accountId, mutableMapOf(), mutableMapOf(), mutableListOf())
            val response = LoyaltyService.getCurrentStampCount(accountSchema, schemeId)
            response shouldBe 0
        }
        "CurrentStampCount should be 1"{
            val accountSchema = AccountSchema(accountId, mutableMapOf(schemeId to 1), mutableMapOf(), mutableListOf())
            val response = LoyaltyService.getCurrentStampCount(accountSchema, schemeId)
            response shouldBe 1
        }
        "ApplicableItems"{
            val receipt = Receipt(merchantId = merchantId, accountId = accountId, items = listOf(item1, Item("2", 100, 1)))
            val response = LoyaltyService.applicableItems(receipt, scheme)

            response shouldHaveSize 1
            response shouldBe listOf(item1)
        }

        "Applies a stamp" {
            val receipt = Receipt(merchantId = merchantId, accountId = accountId, items = listOf(item1))

            val response = implementation.apply(receipt)

            response shouldHaveSize (1)
            response.first().stampsGiven shouldBe 1
            response.first().currentStampCount shouldBe 1
            response.first().paymentsGiven shouldHaveSize 0
        }

        "Triggers a redemption once" {
            val receipt =
                    Receipt(merchantId = merchantId, accountId = accountId, items = 1.rangeTo(5).map { item1 })
            val response = implementation.apply(receipt)

            response shouldHaveSize (1)
            response.first().stampsGiven shouldBe 4
            response.first().currentStampCount shouldBe 0
            response.first().paymentsGiven shouldHaveSize 1
            response.first().paymentsGiven.first() shouldBe 100
        }

        "Triggers a redemption twice" {
            val receipt =
                    Receipt(merchantId = merchantId, accountId = accountId, items = 1.rangeTo(10).map { item1 })
            val response = implementation.apply(receipt)

            response shouldHaveSize (1)
            response.first().stampsGiven shouldBe 8
            response.first().currentStampCount shouldBe 0
            response.first().paymentsGiven shouldHaveSize 2
            response.first().paymentsGiven.first() shouldBe 100
            response.first().paymentsGiven[1] shouldBe 100
        }

        "Stores the current state for an account" {
            val receipt = Receipt(merchantId = merchantId, accountId = accountId, items = listOf(item1))

            implementation.apply(receipt)
            val response = implementation.state(accountId)

            response shouldHaveSize (1)
            response.first().currentStampCount shouldBe 1
            response.first().payments shouldHaveSize 0
        }
    }

    override fun isolationMode() = IsolationMode.InstancePerTest

    companion object {
        private val accountId: AccountId = UUID.randomUUID()
        private val merchantId: MerchantId = UUID.randomUUID()

        private val schemeId: SchemeId = UUID.randomUUID()
        private val scheme: Scheme = Scheme(schemeId, merchantId, 4, listOf("1"))
        private val schemes = listOf(scheme)

        private val item1: Item = Item("1", 100, 1)

        private val listOfItems = listOf(Item("1", 200, 2), Item("2", 100, 1))
    }

}
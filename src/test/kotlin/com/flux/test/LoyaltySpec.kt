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


//It does feel like there is too many to test for a single class which could make this confusion.

//If i had more time i would look to abstraic out the util funcs with the core funcs
class LoyaltySpec : StringSpec() {


    //I would have also perferd if i blocked the repos as these are more intagration tests then unit tests
    val implementation: ImplementMe = LoyaltyService(schemes, AccountRepo())

    //I have not done much Kotlin but is there a easier way to select a test within the IDE
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
            val receipt = Receipt(merchantId = merchantId, accountId = accountId, items = listOf(item1, Item("10", 100, 1)))
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

        "Trigger a rederedemptionm and check current state" {
            val receipt = Receipt(merchantId = merchantId, accountId = accountId, items = 1.rangeTo(11).map { item1 })
            implementation.apply(receipt)
            val response = implementation.state(accountId)

            response shouldHaveSize (1)
            response.first().currentStampCount shouldBe 1
            response.first().payments shouldHaveSize 2
            response.first().payments shouldBe listOf(100L, 100L)
        }

        "Trigger a redemption and choose cheapest" {
            val receipt =
                    Receipt(merchantId = merchantId, accountId = accountId, items = 1.rangeTo(5).map { item1 }.plus(item2))
            val response = implementation.apply(receipt)

            response shouldHaveSize (1)
            response.first().stampsGiven shouldBe 5
            response.first().currentStampCount shouldBe 1
            response.first().paymentsGiven shouldHaveSize 1
            response.first().paymentsGiven.first() shouldBe 10

        }
        "Should not Trigger a redemption when sku is not appart of deal" {
            val receipt =
                    Receipt(merchantId = merchantId2, accountId = accountId, items = 1.rangeTo(4).map { item3 }.plus(Item("10", 25, 1)))
            val response = implementation.apply(receipt)

            response shouldHaveSize (1)
            response.first().stampsGiven shouldBe 4
            response.first().currentStampCount shouldBe 4
            response.first().paymentsGiven shouldHaveSize 0

        }

        "Should apply to both schemes" {
            val receipt = Receipt(merchantId = merchantId3, accountId = accountId, items = listOf(item4))
            val response = implementation.apply(receipt)

            response shouldHaveSize (2)
            response.first().stampsGiven shouldBe 1
            response.first().currentStampCount shouldBe 1
            response.first().paymentsGiven shouldHaveSize 0

            response[1].stampsGiven shouldBe 1
            response[1].currentStampCount shouldBe 1
            response[1].paymentsGiven shouldHaveSize 0
        }
    }

    override fun isolationMode() = IsolationMode.InstancePerTest

    //Too much going on and would be confsion for anyone picking this up
    companion object {
        private val accountId: AccountId = UUID.randomUUID()
        private val merchantId: MerchantId = UUID.randomUUID()
        private val merchantId2: MerchantId = UUID.randomUUID()
        private val merchantId3: MerchantId = UUID.randomUUID()

        private val schemeId: SchemeId = UUID.randomUUID()
        private val schemeId2: SchemeId = UUID.randomUUID()
        private val scheme: Scheme = Scheme(schemeId, merchantId, 4, listOf("1", "2", "4"))
        private val scheme2: Scheme = Scheme(schemeId2, merchantId2, 4, listOf("3"))
        private val scheme3: Scheme = Scheme(UUID.randomUUID(), merchantId3, 4, listOf("4"))
        private val scheme4: Scheme = Scheme(UUID.randomUUID(), merchantId3, 4, listOf("4"))
        private val schemes = listOf(scheme, scheme2, scheme3, scheme4)

        private val item1: Item = Item("1", 100, 1)
        private val item2: Item = Item("2", 10, 1)
        private val item3: Item = Item("3", 25, 1)
        private val item4: Item = Item("4", 25, 1)

        private val listOfItems = listOf(Item("1", 200, 2), Item("2", 100, 1))
    }

}
package com.flux.test.service

import com.flux.test.ImplementMe
import com.flux.test.model.*
import com.flux.test.repo.AccountRepo
import com.flux.test.repo.SchemeRepo
import kotlin.math.floor

class LoyaltyService(
        override var schemes: List<Scheme>,
        val accountRepo: AccountRepo,
        val schemeRepo: SchemeRepo) : ImplementMe {


    override fun apply(receipt: Receipt): List<ApplyResponse> {
        schemeRepo.getMercgantsById(receipt.merchantId)
                .map { scheme ->
                    {
                        val applicableItems = receipt.items.filter { scheme.skus.contains(it.sku) }
                        val account = accountRepo.findAccount(receipt.accountId)
                                ?: accountRepo.createAccount(receipt.accountId)
                        val currentStampCount = account.schemeStamp[scheme.id] ?: 0
                        val acquiredStamps = applicableItems.map { it.quantity }.sum()
                        val newStampCount = currentStampCount + acquiredStamps
                        accountRepo.updateStampCountByScheme(account.id, scheme.id, newStampCount % scheme.maxStamps)
                        if (newStampCount >= scheme.maxStamps) {
                            val amountOfFreeItems = floor((newStampCount / scheme.maxStamps).toDouble()).toInt()
                            //Unsure is it the lowest of the basket or what contributs to a stamp
                            val itemsByPrice = applicableItems.sortedBy { it.price }
                            val itemsToPay = flattenItems(applicableItems).subList(amountOfFreeItems, itemsByPrice.size)
                            ApplyResponse(scheme.id, newStampCount, acquiredStamps, itemsToPay)
                        }
                    }

                }

    }

    private fun flattenItems(items: List<Item>): List<Long> {
        return items.flatMap { item ->
            (1..item.quantity).map { item.price }

        }
        override fun state(accountId: AccountId): List<StateResponse> {

        }

        fun findAccount() {

        }
    }
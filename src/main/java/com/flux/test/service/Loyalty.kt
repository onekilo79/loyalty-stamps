package com.flux.test.service

import com.flux.test.ImplementMe
import com.flux.test.model.*
import com.flux.test.repo.AccountRepo
import com.flux.test.repo.SchemeRepo
import kotlin.math.floor

class LoyaltyService(
        override var schemes: List<Scheme>,
        val accountRepo: AccountRepo,
        val schemeRepo: SchemeRepo = SchemeRepo(schemes)) : ImplementMe {


    override fun apply(receipt: Receipt): List<ApplyResponse> {
        return schemeRepo.getMercgantsById(receipt.merchantId)
                .map { applyResponse(receipt, it) }

    }

    private fun applyResponse(receipt: Receipt, scheme: Scheme): ApplyResponse {
        val applicableItems = applicableItems(receipt, scheme)
        val account = accountRepo.findOrCreateAccount(receipt.accountId)
        val currentStampCount = getCurrentStampCount(account, scheme.id)
        val acquiredStamps = acquiredStamps(applicableItems)
        val newStampCount = currentStampCount + acquiredStamps

        return if (newStampCount >= scheme.maxStamps) {
            val amountOfFreeItems = freeItemAcquired(newStampCount, scheme.maxStamps)
            val itemsByPrice = applicableItems.sortedBy { it.price }
            val redemptionStampsCount = calcRedementionOfStamps(newStampCount, scheme.maxStamps, amountOfFreeItems)
            val stampsGiven = acquiredStamps - amountOfFreeItems
            accountRepo.updateStampCountByScheme(account.id, scheme.id,redemptionStampsCount)
            ApplyResponse(scheme.id, redemptionStampsCount, stampsGiven, itemsWhichAreFree(itemsByPrice, amountOfFreeItems).map { it.price })
        } else {
            accountRepo.updateStampCountByScheme(account.id, scheme.id, newStampCount)
            ApplyResponse(scheme.id, newStampCount, acquiredStamps, listOf())
        }
    }

    override fun state(accountId: AccountId): List<StateResponse> {
        return accountRepo.findOrCreateAccount(accountId).schemeStamp.map { StateResponse(it.key, it.value, listOf() )}
    }

    companion object {

        fun itemsWhichAreFree(itemsByPrice: List<Item>, amountOfFreeItems: Int) = (itemsByPrice).subList(0, amountOfFreeItems)

        fun calcRedementionOfStamps(newStampCount: Int, maxStamps: Int, amountOfFreeItems: Int) = (newStampCount % maxStamps) - amountOfFreeItems

        fun freeItemAcquired(newStampCount: Int, maxStamps: Int) = floor((newStampCount / maxStamps).toDouble()).toInt()

        fun acquiredStamps(applicableItems: List<Item>) = applicableItems.map { it.quantity }.sum()

        fun getCurrentStampCount(account: AccountSchema, schemeId: SchemeId) = account.schemeStamp[schemeId] ?: 0

        fun applicableItems(receipt: Receipt, scheme: Scheme) = receipt.items.filter { scheme.skus.contains(it.sku) }

        fun flattenItems(items: List<Item>): List<Long> {
            return items.flatMap { item ->
                (1..item.quantity).map { item.price }

            }
        }
    }
}
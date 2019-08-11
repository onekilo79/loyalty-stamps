package com.flux.test.service

import com.flux.test.ImplementMe
import com.flux.test.model.*
import com.flux.test.repo.AccountRepo
import com.flux.test.repo.SchemeRepo
import kotlin.math.floor

//In Java Spring world, normally i would be autowiring hte below.
//Also the scheme would be within the schemeRepo
class LoyaltyService(
        override var schemes: List<Scheme>,
        val accountRepo: AccountRepo,
        val schemeRepo: SchemeRepo = SchemeRepo(schemes)) : ImplementMe {


    override fun apply(receipt: Receipt): List<ApplyResponse> {
        return schemeRepo.getMercgantsById(receipt.merchantId)
                .map { applyResponse(receipt, it) }

    }

    //It does feel this could be refactored as there is alot going on which will make this hard for others to follow
    private fun applyResponse(receipt: Receipt, scheme: Scheme): ApplyResponse {
        val applicableItems = applicableItems(receipt, scheme)
        val account = accountRepo.findOrCreateAccount(receipt.accountId)
        val acquiredStamps = acquiredStamps(applicableItems)
        val newStampCount = getCurrentStampCount(account, scheme.id) + acquiredStamps

        accountRepo.recordReceipt(receipt)
        return if (newStampCount > scheme.maxStamps) {
            val amountOfFreeItems = freeItemAcquired(newStampCount, scheme.maxStamps)
            val redemptionStampsCount = calcRedementionOfStamps(newStampCount, scheme.maxStamps, amountOfFreeItems)
            val stampsGiven = acquiredStamps - amountOfFreeItems

            val freeItems = itemsWhichAreFree(applicableItems, amountOfFreeItems)
            accountRepo.updateStampCountByScheme(account.id, scheme.id,redemptionStampsCount)
            accountRepo.addRedemption(account.id, scheme.id, freeItems)
            ApplyResponse(scheme.id, redemptionStampsCount, stampsGiven, freeItems.map { it.price })
        } else {
            accountRepo.updateStampCountByScheme(account.id, scheme.id, newStampCount)
            ApplyResponse(scheme.id, newStampCount, acquiredStamps, listOf())
        }
    }

    override fun state(accountId: AccountId): List<StateResponse> {
        val account = accountRepo.findOrCreateAccount(accountId)
        return account.schemeStamp.keys
                .map{ scheme -> StateResponse(scheme, account.schemeStamp[scheme]?: 0, (account.schemeRedemptionHistory[scheme]?: mutableListOf()).map { it.price } ) }
    }

    companion object {

        fun itemsWhichAreFree(itemsByPrice: List<Item>, amountOfFreeItems: Int) = (itemsByPrice).sortedBy { it.price }.subList(0, amountOfFreeItems)

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
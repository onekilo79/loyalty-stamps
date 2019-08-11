package com.flux.test.repo

import com.flux.test.model.*

class AccountRepo{
    private val accountSchema: MutableList<AccountSchema> = mutableListOf()


    fun findOrCreateAccount(accountId: AccountId): AccountSchema {
        return accountSchema.find { it.id == accountId } ?: createAccount(accountId)
    }

    fun createAccount(accountId: AccountId): AccountSchema{
        val newAccount = AccountSchema(accountId, mutableMapOf(), mutableMapOf(), mutableListOf())
        accountSchema.add(newAccount)
        return newAccount
    }

    fun updateStampCountByScheme(accountId: AccountId, schemeId: SchemeId, amount: Int){
        findOrCreateAccount(accountId).schemeStamp[schemeId] = amount
    }

    fun addRedemption(accountId: AccountId, schemeId: SchemeId, items: List<Item>){
        findOrCreateAccount(accountId).schemeRedemptionHistory[schemeId]?.addAll(items)
    }

    fun recordReceipt(receipt: Receipt){
        findOrCreateAccount(receipt.accountId).purchaseHistory.add(receipt)
    }
}
package com.flux.test.repo

import com.flux.test.model.*

class AccountRepo{
    val accountSchema: MutableList<AccountSchema> = mutableListOf()

    fun findAccount(accountId: AccountId): AccountSchema? {
        return accountSchema.find { it.id == accountId }
    }

    fun createAccount(accountId: AccountId): AccountSchema{
        val newAccount = AccountSchema(accountId, mutableMapOf())
        accountSchema.add(newAccount)
        return newAccount
    }

    fun updateStampCountByScheme(accountId: AccountId, schemeId: SchemeId, amount: Int){
        findAccount(accountId)?.schemeStamp?.put(schemeId, amount)
    }
}
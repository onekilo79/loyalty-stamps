package com.flux.test.model

import java.util.UUID

data class Scheme(
        val id: SchemeId,
        val merchantId: MerchantId,
        val maxStamps: Int,
        val skus: List<String>
)

//These should not really be mutable.
data class AccountSchema(
        val id: AccountId,
        val schemeStamp: MutableMap<SchemeId, Int>,
        val schemeRedemptionHistory: MutableMap<SchemeId, MutableList<Item>>,
        val purchaseHistory: MutableList<Receipt>
)

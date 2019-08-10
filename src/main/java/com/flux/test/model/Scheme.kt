package com.flux.test.model

import java.util.UUID

data class Scheme(
        val id: SchemeId,
        val merchantId: MerchantId,
        val maxStamps: Int,
        val skus: List<String>
)


//This modal would happy fit a nosql db and treat the below as a single collection
data class AccountSchema(
        val id: AccountId,
        val schemeStamp: MutableMap<SchemeId, Int>
)
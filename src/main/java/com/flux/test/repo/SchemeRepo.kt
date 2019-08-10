package com.flux.test.repo

import com.flux.test.model.MerchantId
import com.flux.test.model.Scheme

class SchemeRepo(var scheme: List<Scheme>) {

    fun getMercgantsById(merchantId: MerchantId): List<Scheme> {
        return scheme.filter { it.merchantId == merchantId }
    }
}
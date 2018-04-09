/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import io.pergasus.api.PhoenixUtils
import timber.log.Timber


/**
 * Utility class for setting up Google Pay Wallet
 */
class WalletPaymentSetup(private val activity: Activity, private val checkOut: Button) {
    //Init payment client as test environment
    private val paymentsClient: PaymentsClient = Wallet.getPaymentsClient(activity, Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build())

    init {
        isReadyToPay()
    }

    private fun isReadyToPay() {
        //Setup payment request
        val payRequest = IsReadyToPayRequest.newBuilder()
                //Add allowed payment methods
                .addAllowedPaymentMethods(mutableListOf(
                        //Wallet payment methods
                        WalletConstants.PAYMENT_METHOD_CARD,
                        WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD
                ))
                //Add allowed card networks
                .addAllowedCardNetworks(mutableListOf(
                        //Wallet card networks
                        WalletConstants.CARD_NETWORK_AMEX,
                        WalletConstants.CARD_NETWORK_DISCOVER,
                        WalletConstants.CARD_NETWORK_VISA,
                        WalletConstants.CARD_NETWORK_MASTERCARD
                ))
                .build()

        val task = paymentsClient.isReadyToPay(payRequest)
        task.addOnCompleteListener(activity, { paymentTask ->
            try {
                val result = paymentTask.getResult(ApiException::class.java)
                if (result) {
                    // Show Google as payment option.
                    Toast.makeText(activity.applicationContext,
                            "Guess what! Google Payment option is enabled",
                            Toast.LENGTH_SHORT).show()
                } else {
                    // Hide Google as payment option.
                    Toast.makeText(activity.applicationContext,
                            "Oops! Google Payment option has been disabled",
                            Toast.LENGTH_SHORT).show()
                }
                checkOut.isEnabled = true
            } catch (e: ApiException) {
                Timber.e(e)
                checkOut.isEnabled = true
            }
        })
    }

    /**
     * Creates payment data request for token
     * @param price as string
     */
    fun createPaymentDataRequest(price: String): PaymentDataRequest? {
        //Setup data for payment request
        val request = PaymentDataRequest.newBuilder()
                .setTransactionInfo(TransactionInfo.newBuilder()
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode(PhoenixUtils.DEF_CURRENCY)
                        .setTotalPrice(price)
                        .build())
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .setCardRequirements(CardRequirements.newBuilder()
                        .addAllowedCardNetworks(mutableListOf(
                                WalletConstants.CARD_NETWORK_AMEX,
                                WalletConstants.CARD_NETWORK_DISCOVER,
                                WalletConstants.CARD_NETWORK_VISA,
                                WalletConstants.CARD_NETWORK_MASTERCARD
                        ))
                        .build())
        //Setup payment method tokenization parameters
        val params: PaymentMethodTokenizationParameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter(GATEWAY, "example")
                .addParameter(GATEWAY_MERCHANT_ID, "exampleGatewayMerchantId")
                .build()
        request.setPaymentMethodTokenizationParameters(params)
        return request.build()
    }

    /**
     * @return paymentsClient
     */
    fun getPaymentClient(): PaymentsClient = paymentsClient

    companion object {
        private const val GATEWAY = "gateway"
        private const val GATEWAY_MERCHANT_ID = "gatewayMerchantId"
    }

}

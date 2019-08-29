package com.crowdin.platform.realtimeupdate

import com.crowdin.platform.data.DataManager
import com.crowdin.platform.data.remote.api.DistributionInfoResponse
import com.crowdin.platform.transformer.ViewTransformerManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

internal class RealTimeUpdateManager(private val sourceLanguage: String,
                                     private val dataManager: DataManager?,
                                     private val viewTransformerManager: ViewTransformerManager,
                                     private val organizationName: String?) {

    companion object {
        const val NORMAL_CLOSURE_STATUS = 0x3E9
        const val PLURAL_NONE = "none"
        private const val BASE_WS_URL = "wss://ws-lb.crowdin.com/"
        private const val BASE_WS_URL_ENTERPRISE = "wss://enterprise.crowdin.com/wsproxy"
    }

    private var socket: WebSocket? = null

    fun openConnection() {
        dataManager ?: return
        val distributionData = dataManager.getData(DataManager.DISTRIBUTION_DATA,
                DistributionInfoResponse.DistributionData::class.java) as DistributionInfoResponse.DistributionData?
        distributionData ?: return

        createConnection(distributionData)
    }

    fun closeConnection() {
        socket?.close(NORMAL_CLOSURE_STATUS, null)
        viewTransformerManager.setOnViewsChangeListener(null)
    }

    private fun createConnection(distributionData: DistributionInfoResponse.DistributionData) {
        val mappingData = dataManager?.getMapping(sourceLanguage) ?: return
        val client = OkHttpClient().newBuilder().build()
        val baseUrl = if (organizationName == null) {
            BASE_WS_URL
        } else {
            BASE_WS_URL_ENTERPRISE
        }

        val request = Request.Builder()
                .url(baseUrl)
                .build()

        val listener = EchoWebSocketListener(mappingData, distributionData, viewTransformerManager)
        socket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }
}
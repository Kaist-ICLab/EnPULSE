package kaist.iclab.tracker.sync.internet

import kaist.iclab.tracker.sync.core.DataChannel
import kaist.iclab.tracker.sync.core.DataChannelReceiver
import kaist.iclab.tracker.sync.core.DataReceiver
import kaist.iclab.tracker.sync.core.DataSender
import okhttp3.Response

/**
 * A DataChannel that uses internet to transfer data.
 * Suitable for communication between server and clients, such as a mobile phone or a smartwatch.
 * If you are using HTTP connection, you have to allow cleartextTraffic in the Android Manifest. Please refer to test-sync module.
 *
 * One distinct characteristics of InternetDataChannel is that send() can be used in 2 ways: for transferring data and for making a request.
 * In both cases, you can retrieve the response.
 *
 * This class now uses only the sender pattern for HTTP requests.
 */
class InternetDataChannel() : DataChannel<Response>() {

    override val sender: DataSender<Response> = InternetSender()
    override val receiver: DataReceiver = object : DataChannelReceiver() {
        // No-op receiver - InternetDataChannel is send-only for now
    }

    /**
     * Send data with specific HTTP method
     */
    fun send(key: String, value: String, method: InternetMethod): Response {
        return (sender as InternetSender).send(key, value, method)
    }
}

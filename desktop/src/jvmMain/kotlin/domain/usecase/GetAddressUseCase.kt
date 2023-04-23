package domain.usecase

import java.net.InetAddress
import java.net.NetworkInterface

class GetAddressUseCase {
    companion object {
        const val LOOP_BACK_ADDRESS = "127.0.0.1"
    }

    operator fun invoke(): String {
        val address = InetAddress.getLocalHost().hostAddress
        return if (address.isLoopBackAddress()) {
            InetAddress.getLocalHost().hostAddress
        } else {
            getHostAddress()
        }
    }

    private fun getHostAddress(): String {
        val networkInterface = NetworkInterface.getNetworkInterfaces()
        var localIpAddress = ""
        while (networkInterface.hasMoreElements()) {
            val element = networkInterface.nextElement()
            val elementNetworkInterface = element.inetAddresses

            while (elementNetworkInterface.hasMoreElements()) {
                val address = elementNetworkInterface.nextElement().hostAddress
                if (address.isLoopBackAddress() && !address.contains(":")) {
                    localIpAddress = address
                }
            }
        }

        return localIpAddress
    }

    private fun String.isLoopBackAddress(): Boolean {
        return this != LOOP_BACK_ADDRESS
    }
}
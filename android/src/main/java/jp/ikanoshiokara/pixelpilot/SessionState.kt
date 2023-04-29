package jp.ikanoshiokara.pixelpilot

sealed class SessionState(val stateMessage: String) {
    object WAIT: SessionState("WAIT")
    object CONNECT: SessionState("CONNECT")
}

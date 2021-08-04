package at.rueckgr.kotlin.rocketbot.websocket

data class SendMessageMessage(val msg: String = "method", val method: String = "sendMessage", val id: String, val params: List<Map<String, Any>>)

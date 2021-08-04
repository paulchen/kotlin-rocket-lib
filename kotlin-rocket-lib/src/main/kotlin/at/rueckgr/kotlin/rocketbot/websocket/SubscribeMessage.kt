package at.rueckgr.kotlin.rocketbot.websocket

data class SubscribeMessage(val msg: String = "sub", val id: String, val name: String, val params: Array<Any?>)

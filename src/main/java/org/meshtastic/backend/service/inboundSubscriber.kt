package org.meshtastic.backend.service

import com.geeksville.mesh.MQTTProtos
import com.geeksville.mesh.MeshProtos
import com.geeksville.mesh.Portnums
import com.google.protobuf.ByteString
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.meshtastic.backend.mqtt.MQTTSubscriber
import org.springframework.stereotype.Component
import org.meshtastic.backend.model.ChannelDB

@Component
class inboundSubscriber(
	private val configuration: Configuration,
	private val channels: ChannelDB
	) :
    MQTTSubscriber("${configuration.inboundRoot}#") {
    private val logger = KotlinLogging.logger {}
   var counter: Int = 1 
    override fun onTopicReceived(msg: MqttMessage) {
	logger.debug(String(msg.payload))
	 val p = MeshProtos.MeshPacket.newBuilder().apply {
                id = counter
                from = (-9999999999).toInt() 
                to = 0xffffffff.toInt() // ugly way of saying broadcast
		hopLimit = 3
		channel = 177
                rxTime = (System.currentTimeMillis() / 1000).toInt()
                rxSnr = 1.5f
                decoded = MeshProtos.Data.newBuilder().apply {
                    portnum = Portnums.PortNum.TEXT_MESSAGE_APP
                    payload = ByteString.copyFromUtf8(String(msg.payload))
                }.build()
	}.build()
	counter = counter + 1
	var e =  MQTTProtos.ServiceEnvelope.newBuilder().apply{
		packet = p
		channelId = "LongSlow"
		gatewayId = "!%08x".format(p.from)
		}.build()
        val nodeId = "!%08x".format(e.packet.from)
        val Topic = "msh/1/c/LongSlow/${nodeId}"
//        mqtt.publish(Topic, e.toByteArray())

            val ch = channels.getById(e.channelId)
            if (ch == null)
                logger.warn("Topic $topic, channel not found")
            else {
                val psk = ch.psk.toByteArray()
                val encrypted = ByteString.copyFrom(encrypt(psk, p.from, p.id, p.decoded.toByteArray()))
		val encodedPacket = p.toBuilder().clearDecoded().setEncrypted(encrypted).build()
		var j =  MQTTProtos.ServiceEnvelope.newBuilder().apply{
                	packet = encodedPacket
                	channelId = "LongSlow"
                	gatewayId = "!%08x".format(p.from)
                }.build()

 		e.toBuilder().setPacket(encodedPacket).build()
 		mqtt.publish(Topic, j.toByteArray())		
		}
	
	}	
}

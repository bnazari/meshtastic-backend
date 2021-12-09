package org.meshtastic.backend.service

import com.geeksville.mesh.MQTTProtos
import com.geeksville.mesh.MeshProtos
import com.geeksville.mesh.Portnums
import com.google.protobuf.ByteString
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.meshtastic.backend.mqtt.MQTTSubscriber
import org.springframework.stereotype.Component

@Component
class inboundSubscriber(private val configuration: Configuration) :
    MQTTSubscriber("${configuration.inboundRoot}#") {
    private val logger = KotlinLogging.logger {}
    override fun onTopicReceived(msg: MqttMessage) {
	logger.debug(String(msg.payload))
	 val p = MeshProtos.MeshPacket.newBuilder().apply {
                id = (100000000).toInt()
                from = (99999999).toInt() 
                to = 0xffffffff.toInt() // ugly way of saying broadcast
                rxTime = (System.currentTimeMillis() / 1000).toInt()
                rxSnr = 1.5f
                decoded = MeshProtos.Data.newBuilder().apply {
                    portnum = Portnums.PortNum.TEXT_MESSAGE_APP
                    payload = ByteString.copyFromUtf8(String(msg.payload))
                }.build()
	}.build()
	var e =  MQTTProtos.ServiceEnvelope.newBuilder().apply{
		packet = p
		}.build()	
	
//	logger.info(String(e.toByteArray()))
	val Topic = "msh/1/c/LongSlow/!99999999"
	mqtt.publish(Topic, e.toByteArray())
	}	
}

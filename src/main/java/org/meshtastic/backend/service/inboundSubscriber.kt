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
    override fun onTopicReceived(msg: MqttMessage) {
        logger.debug(String(msg.payload))
         val p = MeshProtos.MeshPacket.newBuilder().apply {
                id = (100000000).toInt()
                from = (-9999999).toInt()
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
                channelId = "LongSlow"
                gatewayId = "!99999999"
                }.build()
        if(e.hasPacket()){
                logger.info("Has packet")
        }
        if(e.packet.payloadVariantCase == MeshProtos.MeshPacket.PayloadVariantCase.DECODED){
                logger.info("Decrypted")
        }
        val nodeId = "!%08x".format(e.packet.from)
        val Topic = "msh/dev/clear/LongSlow/${nodeId}/1"
        mqtt.publish(Topic, e.toByteArray())

            val ch = channels.getById(e.channelId)
            if (ch == null)
                logger.warn("Topic $topic, channel not found")
            else {
                logger.info("Let's encrypt")
                val psk = ch.psk.toByteArray()
                val encrypted = encrypt(psk, p.from, p.id, p.decoded.toByteArray())
                val encoded = MeshProtos.Data.parseFrom(encrypted)
//              val encodedPacket = p.toBuilder().clearDecoded().setEncrypted(encrypted).build()
//              e.toBuilder().setPacket(encodedPacket).build()
                }

        }
}

package studio.guitarlab.platform.separation

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import studio.guitarlab.core.separation.*

class RemoteManifestCodecTest {
    @Test fun validManifestParsesWithoutAndroidJsonRuntime() {
        val job=UUID.randomUUID().toString();val stems=RemoteResultManifest.STEMS.joinToString(","){"{\"name\":\"$it\",\"path\":\"remote/v1/users/u/jobs/$job/output/$it.wav\",\"bytes\":48,\"sha256\":\"${"a".repeat(64)}\"}"}
        val json="""{"schemaVersion":1,"jobId":"$job","projectId":"p","inputSha256":"${"b".repeat(64)}","engine":"demucs.cpp","engineRevision":"rc5","model":"htdemucs_6s","modelSha256":"${RemoteResultManifest.MODEL_SHA256}","sampleRate":44100,"channels":2,"frames":1,"duration":0.001,"stems":[$stems]}"""
        val manifest=RemoteManifestCodec.decode(json.toByteArray());manifest.validateFor(RemoteJobIdentity(job,"p","source","b".repeat(64)));assertEquals(6,manifest.stems.size)
    }
    @Test fun malformedAndOversizedManifestFailClosed(){assertThrows(Exception::class.java){RemoteManifestCodec.decode("{}".toByteArray())};assertThrows(IllegalArgumentException::class.java){RemoteManifestCodec.decode(ByteArray(65537))}}
    @Test fun wavParserRejectsWrongAndTruncatedContainers(){assertThrows(IllegalArgumentException::class.java){WavStructure.read(ByteArray(44))};val wav=wav(44100,2,4);val parsed=WavStructure.read(wav);assertEquals(44100,parsed.sampleRate);assertEquals(2,parsed.channels);assertEquals(4,parsed.frames);assertThrows(IllegalArgumentException::class.java){WavStructure.read(wav.copyOf(45))}}
    private fun wav(rate:Int,channels:Int,frames:Int):ByteArray { val data=frames*channels*2;val b=ByteBuffer.allocate(44+data).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".toByteArray()).putInt(36+data).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate*channels*2).putShort((channels*2).toShort()).putShort(16).put("data".toByteArray()).putInt(data);repeat(data){b.put(0)};return b.array() }
}

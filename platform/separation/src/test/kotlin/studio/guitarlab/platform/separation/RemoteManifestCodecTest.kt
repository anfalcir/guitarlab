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
    @Test fun wavParserRejectsWrongAndTruncatedContainers(){assertThrows(IllegalArgumentException::class.java){WavStructure.read(ByteArray(44).inputStream())};val wav=wav(44100,2,4);val parsed=WavStructure.read(wav.inputStream());assertEquals(44100,parsed.sampleRate);assertEquals(2,parsed.channels);assertEquals(4,parsed.frames);assertThrows(IllegalArgumentException::class.java){WavStructure.read(wav.copyOf(45).inputStream())}}
    @Test fun preparedReferenceV2ManifestParsesAndValidates() {
        val job = UUID.randomUUID().toString()
        val sha = "a".repeat(64)
        val json = """{"schemaVersion":2,"jobId":"$job","uid":"u","projectId":"p","inputSha256":"${"b".repeat(64)}","engine":"demucs-pytorch","engineRevision":"rc20-official-demucs-pytorch","model":"htdemucs_6s","modelSha256":"${RemoteResultManifest.OFFICIAL_MODEL_SHA256}","sampleRate":44100,"channels":2,"frames":441,"duration":0.01,"startedAt":"2026-09-23T00:00:00Z","completedAt":"2026-09-23T00:00:01Z","wallTimeMs":1000,"vCPU":8,"blasThreads":8,"demucsThreads":0,"inferenceStrategy":"pytorch-cpu-s1-o0.5-t8","device":"cpu","shifts":1,"overlap":0.5,"demucsVersion":"4.1.0","pytorchVersion":"2.14.0+cpu","modelBytes":54996327,"referenceRecipe":{"version":"prepared-reference-v2","targetPeakDbfs":-1.0,"sharedGainDb":-0.5,"backingStems":["drums","bass","other","vocals","piano"],"guitarStem":"guitar"},"deliverables":[{"name":"backing","role":"REFERENCE_BACKING","path":"remote/v1/users/u/jobs/$job/output/prepared/backing.wav","bytes":128,"sha256":"$sha","sampleRate":44100,"channels":2,"frames":441,"encoding":"FLOAT32_LE"},{"name":"guitar","role":"REFERENCE_GUITAR","path":"remote/v1/users/u/jobs/$job/output/prepared/guitar.wav","bytes":128,"sha256":"$sha","sampleRate":44100,"channels":2,"frames":441,"encoding":"FLOAT32_LE"}]}"""
        val manifest = RemoteManifestCodec.decode(json.toByteArray())
        manifest.validateFor(RemoteJobIdentity(job, "p", "source", "b".repeat(64)), "u")
        assertEquals("u", manifest.uid)
        assertEquals(2, manifest.schemaVersion)
        assertEquals(listOf("backing", "guitar"), manifest.deliverables.map { it.name })
        assertTrue(manifest.stems.isEmpty())
        assertEquals("cpu", manifest.device)
        assertEquals(1, manifest.shifts)
        assertEquals(RemoteResultManifest.OFFICIAL_MODEL_BYTES, manifest.modelBytes)
        val legacyEngine = manifest.copy(engine = "demucs.cpp", modelSha256 = RemoteResultManifest.MODEL_SHA256)
        assertThrows(IllegalArgumentException::class.java) {
            legacyEngine.validateFor(RemoteJobIdentity(job, "p", "source", "b".repeat(64)), "u")
        }
        assertThrows(IllegalArgumentException::class.java) {
            manifest.validateFor(RemoteJobIdentity(job, "p", "source", "b".repeat(64)), "foreign")
        }
    }

    private fun wav(rate:Int,channels:Int,frames:Int):ByteArray { val data=frames*channels*2;val b=ByteBuffer.allocate(44+data).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".toByteArray()).putInt(36+data).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate*channels*2).putShort((channels*2).toShort()).putShort(16).put("data".toByteArray()).putInt(data);repeat(data){b.put(0)};return b.array() }
}

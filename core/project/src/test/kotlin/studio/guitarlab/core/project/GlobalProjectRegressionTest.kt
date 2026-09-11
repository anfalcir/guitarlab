package studio.guitarlab.core.project

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.*

class GlobalProjectRegressionTest {
    @Test fun everyPersistedFieldSurvivesJsonRoundTrip() {
        val project = GuitarProject(
            schemaVersion = CURRENT_PROJECT_SCHEMA_VERSION, id = "p", name = "Complete",
            template = ProjectTemplate.GUITAR, createdAtEpochMs = 11, updatedAtEpochMs = 22,
            sampleRate = SampleRateConfig(SampleRateMode.FIXED, 48_000), masterGainDb = -2.5f,
            groups = listOf(TrackGroup("g", "Group", true, 0)),
            tracks = listOf(AudioTrack("t", "Track", "g", BuiltInRoles.GUITAR, RoleSource.USER, ChannelLayout.STEREO, .25f, -3f, true, true, true, 0, 7)),
            clips = listOf(AudioClip(
                "c", "t", "Clip", "managed://media/source/a.flac", 123, 17, 456, -4f, true,
                "media/source/a.flac", "media/proxy/a.wav", "content://original/a.flac", "FLAC",
                44_100, 2, 24, "PCM", 1_000, 48_000, 1_088, 31, 47,
            )),
            customRoles = listOf(TrackRoleDefinition("custom.rhythm", "Rhythm", false, ChannelLayout.MONO, -.5f)),
        )
        assertEquals(project, ProjectCodec().decode(ProjectCodec().encode(project)))
    }

    @Test fun migrationMatrixDefaultsMissingFieldsAndIgnoresFutureFields() {
        val fixtures = listOf(
            """{"schemaVersion":1,"id":"pre-m5","name":"Pre M5","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1}""",
            """{"schemaVersion":1,"id":"m5","name":"M5","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1,"tracks":[{"id":"t","name":"T","order":0,"armed":true}]}""",
            """{"schemaVersion":1,"id":"m6","name":"M6","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1,"masterGainDb":-3.0,"futureRoot":"ignored"}""",
            """{"schemaVersion":1,"id":"pre-m7","name":"Pre M7","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1,"tracks":[{"id":"t","name":"T","order":0}],"clips":[{"id":"c","trackId":"t","name":"C","sourceUri":"x","startFrame":0,"lengthFrames":10,"unknownClip":42}]}""",
        )
        fixtures.forEach { json ->
            val decoded = ProjectCodec().decode(json)
            assertTrue(ProjectValidator.validate(decoded).isEmpty(), decoded.id)
            assertEquals(decoded, ProjectCodec().decode(ProjectCodec().encode(decoded)))
        }
        assertEquals(0f, ProjectCodec().decode(fixtures[0]).masterGainDb)
        assertEquals(0L, ProjectCodec().decode(fixtures[3]).clips.single().fadeInFrames)
        assertEquals(null, ProjectCodec().decode(fixtures[3]).clips.single().managedEditProxyPath)
    }

    @Test fun twentyFourTrackHundredTwentyClipProjectRoundTripsBundleAndRepository() {
        val root = createTempDirectory("guitarlab-stress-root-").toFile()
        val sourceRoot = createTempDirectory("guitarlab-stress-source-").toFile()
        try {
            val tracks = List(24) { AudioTrack("t$it", "Track $it", pan = (it % 3 - 1).toFloat(), gainDb = -(it % 7).toFloat(), order = it) }
            val clips = List(120) { index ->
                val relative = "media/source/s${index % 12}.wav"
                AudioClip("c$index", "t${index % 24}", "Clip $index", "managed://$relative", index * 997L, index % 17L, 480,
                    gainDb = -(index % 5).toFloat(), muted = index % 19 == 0, managedSourcePath = relative,
                    sourceTotalFrames = 1_000, fadeInFrames = (index % 20).toLong(), fadeOutFrames = (index % 15).toLong())
            }
            repeat(12) { index -> File(sourceRoot, "media/source").mkdirs(); File(sourceRoot, "media/source/s$index.wav").writeBytes(ByteArray(64) { (it + index).toByte() }) }
            val project = GuitarProject(id = "large", name = "Large", template = ProjectTemplate.BLANK,
                createdAtEpochMs = 1, updatedAtEpochMs = 2, sampleRate = SampleRateConfig(SampleRateMode.FIXED, 48_000),
                tracks = tracks, clips = clips)
            assertTrue(ProjectValidator.validate(project).isEmpty())
            val repository = FileProjectRepository(root)
            assertEquals(project, repository.load(repository.save(project).id))
            val bytes = ByteArrayOutputStream().also { ProjectBundleWriter().write(project, sourceRoot, it) }.toByteArray()
            val restored = ProjectBundleReader(root).read(bytes.inputStream(), 99)
            assertNotEquals(project.id, restored.id)
            assertEquals(project.copy(id = restored.id, createdAtEpochMs = 99, updatedAtEpochMs = 99), restored)
            assertTrue(bytes.size < 100_000)
        } finally { root.deleteRecursively(); sourceRoot.deleteRecursively() }
    }

    @Test fun deterministicFuzzMaintainsTimelineSourceAndFiniteMixerInvariants() {
        val random = Random(0x47554C)
        repeat(2_000) { index ->
            val total = random.nextLong(1, 1_000_000)
            val sourceStart = random.nextLong(0, total)
            val length = random.nextLong(1, total - sourceStart + 1)
            val start = random.nextLong(0, Long.MAX_VALUE - length)
            val pan = random.nextDouble(-1.0, 1.0).toFloat()
            val gain = random.nextDouble(-60.0, 12.0).toFloat()
            val project = GuitarProject(id = "p$index", name = "P", template = ProjectTemplate.BLANK,
                createdAtEpochMs = 1, updatedAtEpochMs = 1,
                tracks = listOf(AudioTrack("t", "T", pan = pan, gainDb = gain, order = 0)),
                clips = listOf(AudioClip("c", "t", "C", "x", start, sourceStart, length,
                    sourceTotalFrames = total, fadeInFrames = random.nextLong(0, length + 1), fadeOutFrames = random.nextLong(0, length + 1))))
            assertTrue(ProjectValidator.validate(project).isEmpty(), "iteration=$index")
            val decoded = ProjectCodec().decode(ProjectCodec().encode(project))
            assertEquals(project, decoded)
            assertTrue(decoded.clips.single().startFrame >= 0)
            assertTrue(decoded.clips.single().sourceStartFrame + length <= total)
        }
    }

    @Test fun malformedJsonAndRepositoryCorruptionDoNotCrashListingOrOverwriteGoodProject() {
        assertFailsWith<Throwable> { ProjectCodec().decode("") }
        assertFailsWith<Throwable> { ProjectCodec().decode("{broken") }
        val root = createTempDirectory("guitarlab-corrupt-repo-").toFile()
        try {
            val repository = FileProjectRepository(root)
            val good = GuitarProject(id = "good", name = "Good", template = ProjectTemplate.BLANK, createdAtEpochMs = 1, updatedAtEpochMs = 1)
            repository.save(good)
            File(root, "projects/bad").mkdirs()
            File(root, "projects/bad/project.json").writeText("{broken")
            assertEquals(listOf(good), repository.list())
            assertEquals(good, repository.load("good"))
        } finally { root.deleteRecursively() }
    }
}

package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.ProjectValidator
import studio.guitarlab.core.model.RecordingTake

class LegacyRecordingStereoMaintenanceTest {
    @Test
    fun legacyManagedRecordingIsRecoveredAsTakeWithoutPromotingOrdinaryImports() {
        val legacy = projectWithLegacyRecording()
        val ordinary = legacy.clips.single().copy(
            id = "ordinary",
            name = "Imported guitar",
            sourceUri = "managed://media/source/ordinary.wav",
            managedSourcePath = "media/source/ordinary.wav",
        )
        val recovered = LegacyRecordingTakeRecoveryPolicy.recover(legacy.copy(clips = legacy.clips + ordinary))

        assertEquals(1, recovered.takes.size)
        val take = recovered.takes.single()
        assertEquals("recording", take.id)
        assertEquals(1_790_203_123_119L, take.createdAtEpochMs)
        assertTrue(take.active)
        assertEquals("recording", recovered.clips.first { it.id == "recording" }.takeId)
        assertNull(recovered.clips.first { it.id == "ordinary" }.takeId)
        assertTrue(ProjectValidator.validate(recovered).isEmpty())
    }

    @Test
    fun projectCodecRecoversLegacyRecordingMetadataOnEveryRead() {
        val codec = ProjectCodec()
        val restored = codec.decode(codec.encode(projectWithLegacyRecording()))

        assertEquals("recording", restored.clips.single().takeId)
        assertEquals("recording", restored.takes.single().id)
        assertTrue(restored.takes.single().active)
    }

    @Test
    fun trimmedLegacyStereoRecordingSeparatesWithoutTrimBoundsAndKeepsValidTakeLineage() {
        val recovered = LegacyRecordingTakeRecoveryPolicy.recover(projectWithLegacyRecording())
        val separated = StereoSeparationProjectPolicy.separate(
            project = recovered,
            sourceClipId = "recording",
            leftTrackId = "left",
            rightTrackId = "right",
            leftProxyPath = "media/proxy/recording-L.wav",
            rightProxyPath = "media/proxy/recording-R.wav",
            splitTotalFrames = 8_790_012L,
            splitSampleRateHz = 44_100,
            ids = StereoSeparationIds("left-clip", "right-clip", "left-take", "right-take"),
            nowEpochMs = 20L,
        )

        assertTrue(ProjectValidator.validate(separated).isEmpty())
        assertEquals(2, separated.clips.size)
        assertEquals(2, separated.takes.size)
        separated.clips.forEach { clip ->
            assertEquals(9_615L, clip.sourceStartFrame)
            assertEquals(8_780_397L, clip.lengthFrames)
            assertEquals(8_790_012L, clip.editingTotalFrames)
            assertEquals(44_100, clip.editingSampleRateHz)
            assertEquals(1, clip.sourceChannelCount)
        }
        assertEquals(setOf("left", "right"), separated.takes.map { it.trackId }.toSet())
        assertEquals(setOf("left-take", "right-take"), separated.clips.mapNotNull { it.takeId }.toSet())
    }

    @Test
    fun modernStereoTakeCopiesFineAdjustmentAndDeactivatesConflictingDestinationTake() {
        val base = LegacyRecordingTakeRecoveryPolicy.recover(projectWithLegacyRecording())
        val sourceTake = base.takes.single().copy(favorite = true, note = "keeper", fineAdjustmentFrames = 441L)
        val existingRightClip = AudioClip(
            id = "existing-right",
            trackId = "right",
            name = "Older",
            sourceUri = "managed://media/source/existing.wav",
            managedSourcePath = "media/source/existing.wav",
            startFrame = 0L,
            lengthFrames = 100L,
            sourceSampleRateHz = 44_100,
            sourceChannelCount = 1,
            takeId = "existing-right-take",
        )
        val withRightTake = base.copy(
            clips = base.clips + existingRightClip,
            takes = listOf(sourceTake, RecordingTake("existing-right-take", "right", "existing-right", "Older", 1L, true)),
        )
        val separated = StereoSeparationProjectPolicy.separate(
            withRightTake, "recording", "left", "right",
            "media/proxy/L.wav", "media/proxy/R.wav", 8_790_012L, 44_100,
            StereoSeparationIds("lc", "rc", "lt", "rt"), 30L,
        )

        assertTrue(ProjectValidator.validate(separated).isEmpty())
        assertEquals(441L, separated.takes.first { it.id == "lt" }.fineAdjustmentFrames)
        assertEquals(441L, separated.takes.first { it.id == "rt" }.fineAdjustmentFrames)
        assertTrue(separated.takes.first { it.id == "lt" }.active)
        assertTrue(separated.takes.first { it.id == "rt" }.active)
        assertTrue(!separated.takes.first { it.id == "existing-right-take" }.active)
    }

    private fun projectWithLegacyRecording(): GuitarProject {
        val tracks = listOf(
            AudioTrack(
                id = "left",
                name = "Minha Guitarra E",
                roleId = BuiltInRoles.RECORDED_GUITAR_L,
                channelLayout = ChannelLayout.MONO,
                order = 0,
            ),
            AudioTrack(
                id = "right",
                name = "Minha Guitarra D",
                roleId = BuiltInRoles.RECORDED_GUITAR_R,
                channelLayout = ChannelLayout.MONO,
                order = 1,
            ),
        )
        val clip = AudioClip(
            id = "recording",
            trackId = "left",
            name = "Take 1",
            sourceUri = "managed://media/source/recording-take-1790203123119.wav",
            managedSourcePath = "media/source/recording-take-1790203123119.wav",
            startFrame = 0L,
            sourceStartFrame = 9_615L,
            lengthFrames = 8_780_397L,
            sourceFormat = "WAV",
            sourceSampleRateHz = 44_100,
            sourceChannelCount = 2,
            sourceBitsPerSample = 32,
            sourceEncoding = "FLOAT32_LE",
            sourceTotalFrames = 8_790_012L,
        )
        return GuitarProject(
            id = "legacy-recording-project",
            name = "Legacy recording",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 10L,
            tracks = tracks,
            clips = listOf(clip),
        )
    }
}

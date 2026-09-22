package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.RecordingTake

class TakeManagementPolicyTest {
    @Test fun deleteActiveRemovesWholeLineageAndChoosesFavoriteFallback() {
        val project = project().copy(
            clips = listOf(clip("root-a", "take-a"), clip("split-a", "take-a"), clip("b", "take-b"), clip("c", "take-c")),
            takes = listOf(
                RecordingTake("take-a", "track", "root-a", "A", 3L, true),
                RecordingTake("take-b", "track", "b", "B", 2L, false, favorite = true),
                RecordingTake("take-c", "track", "c", "C", 4L, false),
            ),
        )
        val result = TakeManagementPolicy.delete(project, "take-a", 9L)
        assertTrue(result.clips.none { it.takeId == "take-a" })
        assertEquals("take-b", result.takes.single { it.active }.id)
        assertEquals(9L, result.updatedAtEpochMs)
    }

    @Test fun deleteInactivePreservesExistingActive() {
        val project = project().copy(
            clips = listOf(clip("a", "a"), clip("b", "b")),
            takes = listOf(RecordingTake("a", "track", "a", "A", 1L, true), RecordingTake("b", "track", "b", "B", 2L, false)),
        )
        val result = TakeManagementPolicy.delete(project, "b", 3L)
        assertEquals(listOf("a"), result.takes.filter { it.active }.map { it.id })
        assertTrue(result.clips.none { it.takeId == "b" })
    }

    @Test fun finalTakeDeletionLeavesTrackValid() {
        val project = project().copy(clips=listOf(clip("a","a")), takes=listOf(RecordingTake("a","track","a","A",1L,true)))
        val result = TakeManagementPolicy.delete(project, "a", 2L)
        assertTrue(result.takes.isEmpty())
        assertTrue(result.clips.isEmpty())
        assertEquals(1, result.tracks.size)
    }

    @Test fun metadataIsBoundedAndOrderedDeterministically() {
        var project = project().copy(
            takes=listOf(
                RecordingTake("a","track","a","A",1L,true),
                RecordingTake("b","track","b","B",3L,false,favorite=true),
                RecordingTake("c","track","c","C",4L,false),
            )
        )
        project = TakeManagementPolicy.rename(project,"a"," Principal ",2L)
        project = TakeManagementPolicy.setNote(project,"a"," boa dinâmica ",3L)
        project = TakeManagementPolicy.setFavorite(project,"a",true,4L)
        assertEquals("Principal", project.takes.first { it.id=="a" }.name)
        assertEquals("boa dinâmica", project.takes.first { it.id=="a" }.note)
        assertTrue(project.takes.first { it.id=="a" }.favorite)
        assertEquals(listOf("a","b","c"), TakeManagementPolicy.orderedForTrack(project,"track").map { it.id })
    }

    @Test fun normalizeLegacyMultiActiveProducesExactlyOneActive() {
        val project = project().copy(takes=listOf(
            RecordingTake("a","track","a","A",1L,true), RecordingTake("b","track","b","B",2L,true,favorite=true)
        ))
        val normalized = TakeManagementPolicy.normalizeTrack(project,"track")
        assertEquals(1, normalized.takes.count { it.active })
        assertEquals("b", normalized.takes.single { it.active }.id)
    }

    @Test fun takeFineAdjustmentMovesWholeLineageByDeltaWithoutDrift() {
        val project = project().copy(
            clips = listOf(
                clip("root", "take").copy(startFrame = 2_000L, sourceStartFrame = 30L, lengthFrames = 70L),
                clip("split", "take").copy(startFrame = 4_000L, sourceStartFrame = 60L, lengthFrames = 40L),
            ),
            takes = listOf(RecordingTake("take", "track", "root", "Take", 1L, true)),
        )
        val advanced = TakeManagementPolicy.setFineAdjustmentFrames(project, "take", 480L, 2L)
        assertEquals(listOf(1_520L, 3_520L), advanced.clips.map { it.startFrame })
        assertEquals(listOf(30L, 60L), advanced.clips.map { it.sourceStartFrame })
        assertEquals(listOf(70L, 40L), advanced.clips.map { it.lengthFrames })
        assertEquals(480L, advanced.takes.single().fineAdjustmentFrames)

        val reduced = TakeManagementPolicy.setFineAdjustmentFrames(advanced, "take", 240L, 3L)
        assertEquals(listOf(1_760L, 3_760L), reduced.clips.map { it.startFrame })
        val neutral = TakeManagementPolicy.setFineAdjustmentFrames(reduced, "take", 0L, 4L)
        assertEquals(project.clips.map { it.startFrame }, neutral.clips.map { it.startFrame })
        assertEquals(0L, neutral.takes.single().fineAdjustmentFrames)
    }

    @Test fun takeFineAdjustmentPreservesCreativeRelativeMovesAndOtherTakes() {
        val project = project().copy(
            clips = listOf(
                clip("a1", "a").copy(startFrame = 3_000L),
                clip("a2", "a").copy(startFrame = 5_500L),
                clip("b", "b").copy(startFrame = 7_000L),
            ),
            takes = listOf(
                RecordingTake("a", "track", "a1", "A", 1L, true),
                RecordingTake("b", "track", "b", "B", 2L, false),
            ),
        )
        val changed = TakeManagementPolicy.setFineAdjustmentFrames(project, "a", -960L, 3L)
        assertEquals(3_960L, changed.clips.first { it.id == "a1" }.startFrame)
        assertEquals(6_460L, changed.clips.first { it.id == "a2" }.startFrame)
        assertEquals(7_000L, changed.clips.first { it.id == "b" }.startFrame)
        assertEquals(2_500L, changed.clips.first { it.id == "a2" }.startFrame - changed.clips.first { it.id == "a1" }.startFrame)
    }

    @Test fun takeFineAdjustmentFailsClosedInsteadOfTrimmingAtTimelineZero() {
        val project = project().copy(
            clips = listOf(clip("a", "a").copy(startFrame = 100L, sourceStartFrame = 20L)),
            takes = listOf(RecordingTake("a", "track", "a", "A", 1L, true)),
        )
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            TakeManagementPolicy.setFineAdjustmentFrames(project, "a", 101L, 2L)
        }
        assertEquals(100L, project.clips.single().startFrame)
        assertEquals(20L, project.clips.single().sourceStartFrame)
        assertEquals(0L, project.takes.single().fineAdjustmentFrames)
    }

    private fun project() = GuitarProject(
        id="p", name="P", template=ProjectTemplate.GUITAR, createdAtEpochMs=1L, updatedAtEpochMs=1L,
        tracks=listOf(AudioTrack(id="track",name="Guitar",channelLayout=ChannelLayout.MONO,order=0)),
    )
    private fun clip(id:String,takeId:String)=AudioClip(
        id=id, trackId="track", name=id, sourceUri="managed://media/source/shared.wav", managedSourcePath="media/source/shared.wav",
        startFrame=0L, sourceStartFrame=0L, lengthFrames=100L, sourceSampleRateHz=48_000, sourceChannelCount=1, takeId=takeId,
    )
}

package studio.guitarlab.core.project

import kotlin.test.*
import studio.guitarlab.core.model.*

class PracticeWorkflowPolicyTest {
    @Test fun `comparison exposes deterministic visual state for guitar tracks`() {
        assertEquals(GuitarAuditionTrackState.INCLUDED, GuitarAuditionPolicy.trackState(BuiltInRoles.REFERENCE_GUITAR, GuitarAuditionMode.REFERENCE))
        assertEquals(GuitarAuditionTrackState.EXCLUDED, GuitarAuditionPolicy.trackState(BuiltInRoles.RECORDED_GUITAR, GuitarAuditionMode.REFERENCE))
        assertEquals(GuitarAuditionTrackState.INCLUDED, GuitarAuditionPolicy.trackState(BuiltInRoles.RECORDED_GUITAR, GuitarAuditionMode.BOTH))
        assertEquals(GuitarAuditionTrackState.UNAFFECTED, GuitarAuditionPolicy.trackState(BuiltInRoles.BACKING, GuitarAuditionMode.MY_GUITAR))
        assertEquals(GuitarAuditionTrackState.UNAFFECTED, GuitarAuditionPolicy.trackState(BuiltInRoles.REFERENCE_GUITAR, GuitarAuditionMode.MIXER))
    }

    private fun project() = GuitarProject(
        id="p", name="P", template=ProjectTemplate.GUITAR, createdAtEpochMs=1, updatedAtEpochMs=1,
        tracks=listOf(AudioTrack("t","G",roleId=BuiltInRoles.RECORDED_GUITAR,order=0), AudioTrack("r","R",roleId=BuiltInRoles.REFERENCE_GUITAR,order=1)),
        clips=listOf(
            AudioClip("c1","t","one","x",0,lengthFrames=10,takeId="one"),
            AudioClip("c2","t","two","x",0,lengthFrames=10,takeId="two"),
            AudioClip("base","r","base","x",0,lengthFrames=10),
        ),
        takes=listOf(RecordingTake("one","t","c1","one",1,false), RecordingTake("two","t","c2","two",2,true)),
    )

    @Test fun activeTakeAndAuditionAreDeterministic() {
        val p=project()
        assertEquals(setOf("c2","base"), ActiveTakePolicy.audibleClips(p).map { it.id }.toSet())
        assertEquals("one", ActiveTakePolicy.activate(p,"one",3).takes.single { it.active }.id)
        assertFalse(GuitarAuditionPolicy.roleAudible(BuiltInRoles.RECORDED_GUITAR,GuitarAuditionMode.REFERENCE))
        assertFalse(GuitarAuditionPolicy.roleAudible(BuiltInRoles.REFERENCE_GUITAR,GuitarAuditionMode.MY_GUITAR))
    }

    @Test fun punchAccountsForPreRollAndLatency() {
        val plan=PunchRecordingPolicy.plan(PunchRegion(10_000,20_000,3_000,1_000),250)
        assertEquals(7_000,plan.captureStartFrame); assertEquals(14_250,plan.automaticStopAfterFrames)
        assertEquals(3_250,plan.keptSourceStartFrame); assertEquals(10_000,plan.keptLengthFrames)
    }

    @Test fun recordingChoiceIsTransientAndOnlyLoopChoiceCreatesPunch() {
        val normal = PracticeRecordingStartPolicy.plan(
            PracticeRecordingMode.CURRENT_PLAYHEAD,
            currentPlayheadFrame = 12_000,
            loopEnabled = false,
            loopStartFrame = 10_000,
            loopEndFrame = 20_000,
            sampleRateHz = 48_000,
        )
        assertEquals(12_000L, normal.sessionStartFrame)
        assertFalse(normal.loopEnabled)
        assertNull(normal.punchRegion)

        val fromStart = PracticeRecordingStartPolicy.plan(
            PracticeRecordingMode.FROM_PROJECT_START,
            currentPlayheadFrame = 12_000,
            loopEnabled = true,
            loopStartFrame = 10_000,
            loopEndFrame = 20_000,
            sampleRateHz = 48_000,
        )
        assertEquals(0L, fromStart.sessionStartFrame)
        assertFalse(fromStart.loopEnabled)
        assertNull(fromStart.punchRegion)

        val punch = PracticeRecordingStartPolicy.plan(
            PracticeRecordingMode.LOOP_PUNCH,
            currentPlayheadFrame = 3_000,
            loopEnabled = true,
            loopStartFrame = 192_000,
            loopEndFrame = 384_000,
            sampleRateHz = 48_000,
        )
        assertEquals(48_000L, punch.sessionStartFrame)
        assertTrue(punch.loopEnabled)
        assertEquals(PunchRegion(192_000, 384_000, 144_000, 48_000), punch.punchRegion)
    }

    @Test fun loopPunchRequiresAnActiveValidLoop() {
        assertFailsWith<IllegalArgumentException> {
            PracticeRecordingStartPolicy.plan(
                PracticeRecordingMode.LOOP_PUNCH,
                currentPlayheadFrame = 0,
                loopEnabled = false,
                loopStartFrame = 10,
                loopEndFrame = 20,
                sampleRateHz = 48_000,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeRecordingStartPolicy.plan(
                PracticeRecordingMode.LOOP_PUNCH,
                currentPlayheadFrame = 0,
                loopEnabled = true,
                loopStartFrame = 20,
                loopEndFrame = 20,
                sampleRateHz = 48_000,
            )
        }
    }

    @Test fun sectionPreviewMatchesAcceptedBoundariesAndClearRemovesAllSections() {
        val suggestions = listOf(SectionBoundarySuggestion(100, .8f), SectionBoundarySuggestion(250, .6f))
        val preview = PracticeWorkflowEditor.previewSuggestedSections(suggestions, 400)
        assertEquals(listOf(0L to 100L, 100L to 250L, 250L to 400L), preview.map { it.startFrame to it.endFrame })
        assertEquals(listOf("Seção 1", "Seção 2", "Seção 3"), preview.map { it.name })

        val accepted = PracticeWorkflowEditor.acceptSuggestedSections(project(), suggestions, 400, now = 10)
        assertEquals(preview.map { it.startFrame to it.endFrame }, accepted.sections.map { it.startFrame to it.endFrame })
        assertEquals(SectionOrigin.AUTOMATIC, accepted.sections.first().origin)

        val cleared = PracticeWorkflowEditor.clearSections(accepted, now = 11)
        assertTrue(cleared.sections.isEmpty())
        assertEquals(11L, cleared.updatedAtEpochMs)
    }

    @Test fun levelAdviceProtectsHeadroom() {
        val result=TrackLevelAdvisor.analyze(floatArrayOf(0.5f,-0.5f,0.5f,-0.5f))
        assertTrue(result.recommendedGainDb <= 0.1f); assertFalse(result.silent)
    }

    @Test fun sectionAnalysisRejectsTinyTimeline() {
        assertTrue(SectionBoundaryAnalyzer.suggest(List(20){0.5f},1).isEmpty())
    }
}

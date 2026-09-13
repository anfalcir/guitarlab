package studio.guitarlab.core.project

import kotlin.test.*
import studio.guitarlab.core.model.*

class PracticeWorkflowPolicyTest {
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

    @Test fun levelAdviceProtectsHeadroom() {
        val result=TrackLevelAdvisor.analyze(floatArrayOf(0.5f,-0.5f,0.5f,-0.5f))
        assertTrue(result.recommendedGainDb <= 0.1f); assertFalse(result.silent)
    }

    @Test fun sectionAnalysisRejectsTinyTimeline() {
        assertTrue(SectionBoundaryAnalyzer.suggest(List(20){0.5f},1).isEmpty())
    }
}

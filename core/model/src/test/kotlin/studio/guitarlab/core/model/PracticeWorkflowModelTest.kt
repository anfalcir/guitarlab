package studio.guitarlab.core.model

import kotlin.test.*
import kotlinx.serialization.json.Json

class PracticeWorkflowModelTest {
    @Test fun schemaOneWithoutNewFieldsRemainsReadable() {
        val json="""{"schemaVersion":1,"id":"p","name":"P","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1}"""
        val project=Json { ignoreUnknownKeys=true }.decodeFromString<GuitarProject>(json)
        assertTrue(project.takes.isEmpty()); assertNull(project.punchRegion)
    }

    @Test fun validatorRejectsMultipleActiveTakes() {
        val track=AudioTrack("t","T",order=0)
        val clips=listOf(AudioClip("c1","t","1","x",0,lengthFrames=1,takeId="a"),AudioClip("c2","t","2","x",0,lengthFrames=1,takeId="b"))
        val p=GuitarProject(id="p",name="P",template=ProjectTemplate.BLANK,createdAtEpochMs=1,updatedAtEpochMs=1,tracks=listOf(track),clips=clips,takes=listOf(RecordingTake("a","t","c1","a",1,true),RecordingTake("b","t","c2","b",2,true)))
        assertTrue(ProjectValidator.validate(p).any { it.code=="take.active.multiple" })
    }
}

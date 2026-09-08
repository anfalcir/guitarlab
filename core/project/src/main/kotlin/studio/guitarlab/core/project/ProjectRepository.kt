package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject

interface ProjectRepository {
    fun list(): List<GuitarProject>
    fun load(projectId: String): GuitarProject?
    fun save(project: GuitarProject): GuitarProject
    fun delete(projectId: String): Boolean
    fun duplicate(projectId: String, newName: String, newProjectId: String, nowEpochMs: Long): GuitarProject
}

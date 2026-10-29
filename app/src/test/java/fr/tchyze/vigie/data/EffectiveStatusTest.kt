package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.Eol
import fr.tchyze.vigie.data.model.effectiveStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EffectiveStatusTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun branch(status: String, trigger: String?, declared: Boolean = false) = Branch(
        id = "p-1", product = "p", vendor = "V", category = "os", branch = "1", label = "1",
        lts = null, releaseDate = null, status = status, latestVersion = null, latestReleaseDate = null,
        eol = Eol(null, null, trigger, null, "end_of_life", trigger, declared, emptyMap(), null),
        releases = emptyList(), sources = emptyList(),
    )

    private fun status(b: Branch) = b.effectiveStatus(today, eolSoonMonths = 6)

    @Test
    fun `la date de déclenchement passée rend la branche EOL`() {
        assertEquals(BranchStatus.EOL, status(branch("active", "2026-10-29")))
        assertEquals(BranchStatus.EOL, status(branch("eol_soon", "2026-01-01")))
    }

    @Test
    fun `une échéance dans les six mois rend la branche bientôt EOL`() {
        assertEquals(BranchStatus.EOL_SOON, status(branch("active", "2027-04-29")))
        assertEquals(BranchStatus.ACTIVE, status(branch("active", "2027-04-30")))
    }

    @Test
    fun `sans date, seule la déclaration de fin de vie compte`() {
        assertEquals(BranchStatus.ACTIVE, status(branch("active", null)))
        assertEquals(BranchStatus.EOL, status(branch("active", null, declared = true)))
    }

    @Test
    fun `le statut publié n'est jamais adouci par le recalcul`() {
        // Surcharge côté collecteur : branche déclarée EOL malgré une date lointaine.
        assertEquals(BranchStatus.EOL, status(branch("eol", "2030-01-01")))
        assertEquals(BranchStatus.EOL_SOON, status(branch("eol_soon", null)))
    }

    @Test
    fun `une date mal formée est ignorée`() {
        assertEquals(BranchStatus.ACTIVE, status(branch("active", "bientôt")))
    }
}

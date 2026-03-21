package com.aquiles.crosschapp.competition

import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.domain.competition.CompetitionFormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * ================================================
 * TESTS UNITARIOS: Flujo de Competencias
 * ================================================
 *
 * Cobertura:
 *  1. Validación del formulario de creación
 *  2. Campos dinámicos por criterio de ranking
 *  3. Validación de resultados ingresados (score)
 *  4. Lista de competidores
 *  5. Lógica de ranking y pódium (1°, 2°, 3°)
 */
class CompetitionFlowTest {

    // ===================================================
    // HELPERS
    // ===================================================
    private fun dateInDays(offset: Int): Date {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, offset)
        }.time
    }

    private fun validFormArgs(
        title: String = "Torneo Verano 2025",
        description: String = "Competencia anual de CrossFit en temporada de verano",
        startDate: Date = dateInDays(0),
        endDate: Date = dateInDays(7),
        maxCapacity: Int = 50,
        criteriaField: String? = null,
        criteria: RankingCriteria? = null
    ) = CompetitionFormValidator.validate(title, description, startDate, endDate, maxCapacity, criteriaField, criteria)

    // ===================================================
    // 1. VALIDACIÓN DEL FORMULARIO
    // ===================================================

    @Test
    fun `titulo vacio devuelve error`() {
        val result = validFormArgs(title = "")
        assertFalse("Deberia ser inválido", result.isValid)
        assertTrue("Deberia haber error de titulo", result.errors.containsKey("title"))
    }

    @Test
    fun `titulo de menos de 3 caracteres devuelve error`() {
        val result = validFormArgs(title = "AB")
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("title"))
    }

    @Test
    fun `titulo valido no genera error`() {
        val result = validFormArgs(title = "Gran Torneo")
        assertFalse("No debe haber error de titulo", result.errors.containsKey("title"))
    }

    @Test
    fun `descripcion vacia devuelve error`() {
        val result = validFormArgs(description = "")
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("description"))
    }

    @Test
    fun `descripcion valida no genera error`() {
        val result = validFormArgs(description = "Una descripción completa del evento.")
        assertFalse(result.errors.containsKey("description"))
    }

    @Test
    fun `fecha fin igual a inicio devuelve error`() {
        val now = Date()
        val result = validFormArgs(startDate = now, endDate = now)
        assertFalse(result.isValid)
        assertTrue("Deberia haber error de fechas", result.errors.containsKey("dates"))
    }

    @Test
    fun `fecha fin anterior a inicio devuelve error`() {
        val result = validFormArgs(
            startDate = dateInDays(5),
            endDate = dateInDays(1)
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("dates"))
    }

    @Test
    fun `fechas validas no generan error`() {
        val result = validFormArgs(
            startDate = dateInDays(0),
            endDate = dateInDays(14)
        )
        assertFalse("No debe haber error de fechas", result.errors.containsKey("dates"))
    }

    @Test
    fun `capacidad cero devuelve error`() {
        val result = validFormArgs(maxCapacity = 0)
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("capacity"))
    }

    @Test
    fun `capacidad negativa devuelve error`() {
        val result = validFormArgs(maxCapacity = -5)
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("capacity"))
    }

    @Test
    fun `capacidad valida no genera error`() {
        val result = validFormArgs(maxCapacity = 30)
        assertFalse(result.errors.containsKey("capacity"))
    }

    @Test
    fun `formulario completamente vacio genera multiples errores`() {
        val result = CompetitionFormValidator.validate(
            title = "",
            description = "",
            startDate = dateInDays(0),
            endDate = dateInDays(0), // igual = error de fechas
            maxCapacity = 0
        )
        assertFalse(result.isValid)
        assertTrue("Debe haber múltiples errores", result.errors.size >= 3)
    }

    @Test
    fun `formulario valido completo no genera errores`() {
        val result = validFormArgs()
        assertTrue("Formulario completo debe ser válido", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    // ===================================================
    // 2. CAMPOS DINÁMICOS POR CRITERIO
    // ===================================================

    @Test
    fun `criterio TIME muestra campo de tiempo limite`() {
        val config = CompetitionFormValidator.getCriteriaFieldConfig(RankingCriteria.TIME)
        assertNotNull("Debe haber config para TIME", config)
        assertTrue("Debe mencionar tiempo", config!!.label.contains("tiempo", ignoreCase = true))
        assertEquals(CompetitionFormValidator.InputType.DECIMAL, config.inputType)
    }

    @Test
    fun `criterio LOAD muestra campo de peso`() {
        val config = CompetitionFormValidator.getCriteriaFieldConfig(RankingCriteria.LOAD)
        assertNotNull("Debe haber config para LOAD", config)
        assertTrue("Debe mencionar peso o kg", config!!.label.contains("peso", ignoreCase = true) || config.label.contains("kg", ignoreCase = true))
    }

    @Test
    fun `criterio REPS muestra campo de rondas`() {
        val config = CompetitionFormValidator.getCriteriaFieldConfig(RankingCriteria.REPS)
        assertNotNull("Debe haber config para REPS", config)
        assertEquals(CompetitionFormValidator.InputType.INTEGER, config!!.inputType)
    }

    @Test
    fun `criterio POINTS no tiene campo extra`() {
        val config = CompetitionFormValidator.getCriteriaFieldConfig(RankingCriteria.POINTS)
        assertNull("POINTS no debe tener campo extra", config)
    }

    @Test
    fun `campo TIME con valor invalido genera error`() {
        val result = validFormArgs(
            criteriaField = "abc",
            criteria = RankingCriteria.TIME
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("criteriaField"))
    }

    @Test
    fun `campo TIME con valor negativo genera error`() {
        val result = validFormArgs(
            criteriaField = "-5",
            criteria = RankingCriteria.TIME
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("criteriaField"))
    }

    @Test
    fun `campo TIME valido no genera error`() {
        val result = validFormArgs(
            criteriaField = "20",
            criteria = RankingCriteria.TIME
        )
        assertFalse("Campo de tiempo válido no debe dar error", result.errors.containsKey("criteriaField"))
    }

    @Test
    fun `campo LOAD valido no genera error`() {
        val result = validFormArgs(
            criteriaField = "100.5",
            criteria = RankingCriteria.LOAD
        )
        assertFalse(result.errors.containsKey("criteriaField"))
    }

    // ===================================================
    // 3. VALIDACIÓN DE RESULTADOS (SCORE)
    // ===================================================

    @Test
    fun `score vacio devuelve error`() {
        val error = CompetitionFormValidator.validateScore("", RankingCriteria.TIME)
        assertNotNull("Score vacío debe generar error", error)
    }

    @Test
    fun `score no numerico devuelve error`() {
        val error = CompetitionFormValidator.validateScore("veinte minutos", RankingCriteria.TIME)
        assertNotNull(error)
    }

    @Test
    fun `score negativo devuelve error`() {
        val error = CompetitionFormValidator.validateScore("-5", RankingCriteria.REPS)
        assertNotNull(error)
    }

    @Test
    fun `score de tiempo valido pasa`() {
        val error = CompetitionFormValidator.validateScore("20", RankingCriteria.TIME)
        assertNull("Tiempo válido no debe dar error", error)
    }

    @Test
    fun `score de reps con decimal devuelve error`() {
        val error = CompetitionFormValidator.validateScore("15.5", RankingCriteria.REPS)
        assertNotNull("Reps no puede ser decimal", error)
    }

    @Test
    fun `score de reps entero valido pasa`() {
        val error = CompetitionFormValidator.validateScore("15", RankingCriteria.REPS)
        assertNull("Reps entero debe pasar", error)
    }

    @Test
    fun `score de carga valido pasa`() {
        val error = CompetitionFormValidator.validateScore("100.5", RankingCriteria.LOAD)
        assertNull("Carga decimal debe pasar", error)
    }

    // ===================================================
    // 4. LISTA DE COMPETIDORES
    // ===================================================

    @Test
    fun `lista vacia de competidores devuelve error`() {
        val error = CompetitionFormValidator.validateCompetitorList(emptyList())
        assertNotNull("Lista vacía debe generar error", error)
    }

    @Test
    fun `lista con un atleta devuelve error por minimo requerido`() {
        val error = CompetitionFormValidator.validateCompetitorList(listOf("user1"), minRequired = 2)
        assertNotNull("1 atleta no es suficiente para iniciar", error)
    }

    @Test
    fun `lista con competidores suficientes pasa`() {
        val users = listOf("user1", "user2", "user3")
        val error = CompetitionFormValidator.validateCompetitorList(users, minRequired = 2)
        assertNull("Lista con 3 atletas debe pasar", error)
    }

    // ===================================================
    // 5. RANKING Y PÓDIUM (1°, 2°, 3°)
    // ===================================================

    private fun makeEntry(userId: String, name: String, score: Double) =
        CompetitionFormValidator.PodiumEntry(
            userId = userId, userName = name,
            scoreDisplay = score.toString(), numericScore = score
        )

    @Test
    fun `podium con 5 atletas solo devuelve top 3`() {
        val ranked = listOf(
            makeEntry("u1", "Ana", 100.0),
            makeEntry("u2", "Bruno", 95.0),
            makeEntry("u3", "Carlos", 88.0),
            makeEntry("u4", "Diana", 75.0),
            makeEntry("u5", "Esteban", 60.0),
        )
        val podium = CompetitionFormValidator.calculatePodium(ranked)
        assertEquals("El podium debe tener exactamente 3 lugares", 3, podium.size)
    }

    @Test
    fun `podium asigna correctamente el primer puesto`() {
        val ranked = listOf(
            makeEntry("u1", "Ana", 100.0),
            makeEntry("u2", "Bruno", 95.0),
            makeEntry("u3", "Carlos", 88.0),
        )
        val podium = CompetitionFormValidator.calculatePodium(ranked)
        assertEquals("El 1° lugar debe ser Ana", "Ana", podium[0].userName)
        assertEquals(1, podium[0].position)
    }

    @Test
    fun `podium asigna correctamente el segundo y tercer puesto`() {
        val ranked = listOf(
            makeEntry("u1", "Ana", 100.0),
            makeEntry("u2", "Bruno", 95.0),
            makeEntry("u3", "Carlos", 88.0),
        )
        val podium = CompetitionFormValidator.calculatePodium(ranked)
        assertEquals(2, podium[1].position)
        assertEquals("Bruno", podium[1].userName)
        assertEquals(3, podium[2].position)
        assertEquals("Carlos", podium[2].userName)
    }

    @Test
    fun `podium con menos de 3 atletas devuelve los disponibles`() {
        val ranked = listOf(
            makeEntry("u1", "Ana", 100.0),
            makeEntry("u2", "Bruno", 95.0),
        )
        val podium = CompetitionFormValidator.calculatePodium(ranked)
        assertEquals("Podium con 2 atletas debe tener 2 entradas", 2, podium.size)
    }

    @Test
    fun `podium vacio no falla`() {
        val podium = CompetitionFormValidator.calculatePodium(emptyList())
        assertTrue("Podium vacío debe estar vacío", podium.isEmpty())
    }

    @Test
    fun `podium usa el orden de la lista de entrada (ya rankeada)`() {
        // Si los resultados ya fueron ordenados por el ViewModel (menor tiempo es mejor),
        // el pódium debe respetar ese orden
        val ranked = listOf(
            makeEntry("u3", "Carlos", 12.5), // menor tiempo = 1°
            makeEntry("u1", "Ana", 15.0),
            makeEntry("u2", "Bruno", 20.0),
        )
        val podium = CompetitionFormValidator.calculatePodium(ranked)
        assertEquals("Carlos debe ser 1° (menor tiempo)", "Carlos", podium[0].userName)
    }
}

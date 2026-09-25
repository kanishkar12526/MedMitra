package com.example.medmitra

import com.example.medmitra.util.OfflinePrescriptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePrescriptionParserTest {

    @Test
    fun testParseStandardPrescription() {
        val prescriptionText = """
            ST. JUDE HOSPITAL
            Dr. Smith
            
            1. Tab. Amoxicillin 500 mg - 1-0-1 - after meals
            2. Cap. Omeprazole 20mg - 1-0-0 - before food
            3. Paracetamol 650 mg - twice daily - 08:00 AM, 08:00 PM
        """.trimIndent()

        val items = OfflinePrescriptionParser.parse(prescriptionText)
        assertEquals(3, items.size)

        // Item 1: Amoxicillin
        assertEquals("Amoxicillin", items[0].name)
        assertEquals("500 mg", items[0].dosage)
        assertEquals(listOf("08:00", "20:00"), items[0].times)
        assertEquals("After food", items[0].instructions)

        // Item 2: Omeprazole
        assertEquals("Omeprazole", items[1].name)
        assertEquals("20mg", items[1].dosage)
        assertEquals(listOf("08:00"), items[1].times)
        assertEquals("Before food", items[1].instructions)

        // Item 3: Paracetamol
        assertEquals("Paracetamol", items[2].name)
        assertEquals("650 mg", items[2].dosage)
        assertEquals(listOf("08:00", "20:00"), items[2].times)
    }

    @Test
    fun testParseDirtyPrescription() {
        val prescriptionText = """
            Rx
            Metformin 1000 mg
            1-1-1
            
            Atorvastatin 20 mg 0-0-1 night
        """.trimIndent()

        val items = OfflinePrescriptionParser.parse(prescriptionText)
        assertTrue(items.isNotEmpty())

        val metformin = items.firstOrNull { it.name.contains("Metformin", ignoreCase = true) }
        assertTrue(metformin != null)
        assertEquals("1000 mg", metformin?.dosage)
        assertEquals(listOf("08:00", "14:00", "20:00"), metformin?.times)
    }
}

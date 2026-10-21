/*
 * This file is part of possy.
 *
 * possy is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * possy is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with possy. If not, see <http://www.gnu.org/licenses/>.
 */
package net.g24.possy.daemon

import net.g24.possy.daemon.configuration.PossyProperties
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.core.io.DefaultResourceLoader
import java.util.UUID

/**
 * Characterization test: pins down what the generated PDF looks like today
 * (page size, orientation, printed texts) so that upgrades of Kotlin, Spring
 * and PDFBox can be checked against it.
 *
 * Runs without a Spring context. The settings mirror the defaults in
 * src/main/resources/application.yml – keep both in sync.
 */
class PdfGeneratorCharacterizationTest {

    companion object {
        private const val POINTS_PER_MM = 72f / 25.4f
        private const val TOLERANCE = 0.01f

        private const val KEY = "KEY-42"
        private const val WEIGHT = "XL"
        private const val TAG = "TAG-7"
        private const val CONTENT = "Hello possy"

        /** page size in mm as configured in application.yml */
        private val PAGE_SIZES_MM = mapOf(
                PrintTemplate.STORY to (160f to 64f),
                PrintTemplate.TASK to (64f to 64f),
                PrintTemplate.FREEFORM to (72f to 80f),
                PrintTemplate.BUG to (72f to 80f))

        /** templates that print the weight box */
        private val WITH_WEIGHT = setOf(PrintTemplate.STORY, PrintTemplate.BUG)

        fun defaultProperties(): PossyProperties {
            val properties = PossyProperties()
            with(properties.pdfGenerator.fonts) {
                header.apply { font = "classpath:SourceSansPro-Black.ttf"; size = 26; lineHeight = 29 }
                content.apply { font = "classpath:ShortStack-Regular.ttf"; size = 16; lineHeight = 20 }
                footer.apply { font = "classpath:SourceSansPro-Regular.ttf"; size = 12; lineHeight = 20 }
            }
            with(properties.pdfGenerator.pages) {
                story.apply { width = 160f; height = 64f; border = 1f; margin = 3f }
                task.apply { width = 64f; height = 64f; border = 1f; margin = 3f }
                freeform.apply { width = 72f; height = 80f; border = 1f; margin = 3f }
                bug.apply { width = 72f; height = 80f; border = 1f; margin = 3f }
            }
            return properties
        }
    }

    private val pdfGenerator = PdfGenerator(defaultProperties(), DefaultResourceLoader())

    private fun render(template: PrintTemplate): ByteArray = pdfGenerator.createPdf(PrintRequest(
            id = UUID.fromString("00000000-0000-0000-0000-000000000042"),
            template = template,
            key = KEY,
            weight = WEIGHT,
            tag = TAG,
            content = CONTENT.toByteArray(),
            mimetype = "plain/text"))

    @ParameterizedTest
    @EnumSource(value = PrintTemplate::class, names = ["STORY", "TASK", "FREEFORM", "BUG"])
    fun `page is portrait with the configured size`(template: PrintTemplate) {
        PDDocument.load(render(template)).use { doc ->
            assertEquals(1, doc.numberOfPages)

            // landscape notes (story) are printed as a portrait page with turned content,
            // so they come out of the 80 mm roll printer lengthwise with any CUPS driver
            val page = doc.getPage(0)
            val (widthMm, heightMm) = PAGE_SIZES_MM.getValue(template)
            assertEquals(minOf(widthMm, heightMm) * POINTS_PER_MM, page.mediaBox.width, TOLERANCE)
            assertEquals(maxOf(widthMm, heightMm) * POINTS_PER_MM, page.mediaBox.height, TOLERANCE)
            // no /Rotate flag: some CUPS drivers ignore it and print only a strip of the note
            assertEquals(0, page.rotation)
        }
    }

    @ParameterizedTest
    @EnumSource(value = PrintTemplate::class, names = ["STORY", "TASK", "FREEFORM", "BUG"])
    fun `page shows key, content and tag`(template: PrintTemplate) {
        // sortByPosition: on the rotated story page the unsorted extraction splits words into single letters
        // hyphen normalization: the fonts use one glyph for '-' and U+2010/U+2011; depending on
        // the PDFBox version the embedded ToUnicode map points to a different one of them
        val text = PDDocument.load(render(template)).use { doc ->
            PDFTextStripper().apply { sortByPosition = true }.getText(doc)
        }.replace('‐', '-').replace('‑', '-')

        assertTrue(text.contains(KEY), "key missing in: $text")
        assertTrue(text.contains(CONTENT), "content missing in: $text")
        assertTrue(text.contains(TAG), "tag missing in: $text")
        assertEquals(template in WITH_WEIGHT, text.contains(WEIGHT), "weight shown only for $WITH_WEIGHT, text: $text")
    }

    @ParameterizedTest
    @EnumSource(value = PrintTemplate::class, names = ["IMAGE"])
    fun `image template is not implemented yet and produces no output`(template: PrintTemplate) {
        assertEquals(0, render(template).size)
    }
}

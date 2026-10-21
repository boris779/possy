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
import net.g24.possy.daemon.templaterenderer.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.util.Matrix
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PdfGenerator(private val possyProperties: PossyProperties, private val resourceLoader: ResourceLoader) {

    fun createPdf(printRequest: PrintRequest): ByteArray {
        ByteArrayOutputStream().use { stream ->
            when (printRequest.template) {
                PrintTemplate.STORY -> generatePdf(printRequest, stream, possyProperties.pdfGenerator.pages.story, StoryTemplateRenderer())
                PrintTemplate.TASK -> generatePdf(printRequest, stream, possyProperties.pdfGenerator.pages.task, TaskTemplateRenderer())
                PrintTemplate.FREEFORM -> generatePdf(printRequest, stream, possyProperties.pdfGenerator.pages.freeform, FreeformTemplateRenderer())
                PrintTemplate.BUG -> generatePdf(printRequest, stream, possyProperties.pdfGenerator.pages.bug, DefaultTemplateRenderer())
                PrintTemplate.IMAGE -> stream // TODO
            }
            return stream.toByteArray()
        }
    }

    private fun generatePdf(
            printRequest: PrintRequest,
            stream: ByteArrayOutputStream,
            pageSpec: PossyProperties.PdfGenerator.Pages.PageSpec,
            layoutRenderer: LayoutRenderer) {
        PDDocument().use { doc ->
            val renderContext = RenderContext(
                    pageSpec,
                    FontContext(resourceLoader, possyProperties.pdfGenerator.fonts.header),
                    FontContext(resourceLoader, possyProperties.pdfGenerator.fonts.content),
                    FontContext(resourceLoader, possyProperties.pdfGenerator.fonts.footer))
            val landscape = renderContext.width > renderContext.height
            val page = PDPage()
            doc.addPage(page)
            if (landscape) {
                // Landscape notes (story) are emitted as a portrait page with the content turned
                // by 90° clockwise. The former /Rotate 90 page flag was ignored by some CUPS
                // drivers, which then printed only a narrow strip of the note on the 80 mm roll.
                page.mediaBox = PDRectangle(renderContext.height, renderContext.width)
            } else {
                page.mediaBox = PDRectangle(renderContext.width, renderContext.height)
            }
            PDPageContentStream(doc, page).use { contents ->
                if (landscape) {
                    // maps landscape (x, y) to portrait (y, width - x) – same result as /Rotate 90
                    contents.transform(Matrix(0f, -1f, 1f, 0f, 0f, renderContext.width))
                }
                layoutRenderer.render(printRequest, doc, contents, renderContext)
            }
            doc.save(stream)
        }
    }
}

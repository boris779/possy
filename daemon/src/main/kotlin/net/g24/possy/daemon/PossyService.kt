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

import net.g24.possy.daemon.configuration.CupsProperties
import net.g24.possy.daemon.configuration.PossyProperties
import org.cups4j.CupsClient
import org.cups4j.CupsPrinter
import org.cups4j.PrintJob
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.*

@Component
class PossyService(
        private val cupsProperties: CupsProperties,
        private val possyProperties: PossyProperties,
        private val pdfGenerator: PdfGenerator) {

    private var jobId = 0
    private val client: CupsClient = CupsClient(cupsProperties.host!!, cupsProperties.port)

    /**
     * Logs the CUPS printers once the application is up. Not done in the constructor any more:
     * an unreachable CUPS server must not prevent the daemon from starting
     * (e.g. with redirect-to-file, where CUPS is not needed at all).
     */
    @EventListener(ApplicationReadyEvent::class)
    fun logPrinters() {
        try {
            val printers = client.printers
            if (printers.isEmpty()) {
                logger().warn("CUPS at {}:{} reports no printers (not shared, or request rejected?)",
                        cupsProperties.host, cupsProperties.port)
            }
            printers.forEach { logPrinter(it) }
            configuredPrinterNames()
                    .filter { name -> printers.none { it.name == name } }
                    .forEach { logger().warn("Configured printer '{}' not found in CUPS", it) }
        } catch (e: Exception) {
            logger().warn("Cannot list printers of CUPS at {}:{}: {}", cupsProperties.host, cupsProperties.port, e.toString())
        }
    }

    fun print(printRequest: PrintRequest) {
        if (printRequest.template === PrintTemplate.IMAGE) {
            printImage(printRequest.content, printRequest.mimetype)
        } else {
            printDocument(printRequest)
        }
    }

    private fun printImage(content: ByteArray, mimetype: String?) {
        // TODO implement (https://github.com/gerald24/possy/issues/4)
    }

    private fun printDocument(printRequest: PrintRequest) {
        val out = pdfGenerator.createPdf(printRequest)

        val attribs = HashMap<String, String>()
        attribs["document-format"] = "application/pdf"

        if (possyProperties.pdfGenerator.isRedirectToFile) {
            Files.write(Files.createTempFile("possy", ".pdf").toAbsolutePath(), out)
        } else {
            val cupsPrinter = findCupsPrinter(printRequest.template)
            val builder = PrintJob.Builder(out)
                    .jobName("Possy #" + ++jobId)
                    .attributes(attribs)
                    .userName("anonymous")
                    .copies(1)
            if (cupsProperties.sendPageSize) {
                pdfGenerator.printedPageSizeMm(printRequest.template)
                        ?.let { (width, height) -> builder.pageFormat(customMedia(width, height)) }
            }
            cupsPrinter.print(builder.build())
        }
    }

    private fun findCupsPrinter(template: PrintTemplate): CupsPrinter {
        val printerName = printerNameForTemplate(template)
        val printers = client.printers
        return printers.firstOrNull { printerName == it.name }
                ?: throw IllegalStateException("CUPS printer '$printerName' for $template not found " +
                        "at ${cupsProperties.host}:${cupsProperties.port}; available: ${printers.map { it.name }}")
    }

    private fun configuredPrinterNames(): Set<String> = with(cupsProperties.printers) {
        setOfNotNull(white, pink, yellow)
    }

    private fun printerNameForTemplate(template: PrintTemplate): String = when (template.paper) {
        PrintPaper.WHITE -> cupsProperties.printers.white!!
        PrintPaper.PINK -> cupsProperties.printers.pink!!
        PrintPaper.YELLOW -> cupsProperties.printers.yellow!!
    }

    private fun logPrinter(printer: CupsPrinter) {
        logger().info("Printer: {}", printer)

        logger().debug(" Media supported:")
        printer.mediaSupported.forEach { logger().debug("  - $it") }

        logger().debug(" Resolution supported:")
        printer.resolutionSupported.forEach { logger().debug("  - $it") }

        logger().debug(" Mime-Types supported:")
        printer.mimeTypesSupported.forEach { logger().debug("  - $it") }
    }
}

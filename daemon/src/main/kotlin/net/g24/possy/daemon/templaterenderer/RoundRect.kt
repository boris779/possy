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
package net.g24.possy.daemon.templaterenderer

import org.apache.pdfbox.pdmodel.PDPageContentStream

/** Control point factor to approximate a quarter circle with a cubic Bézier curve. */
private const val BEZIER_CIRCLE = 0.5519150245f

/**
 * Adds a closed rounded-rectangle path; call fill() or stroke() afterwards.
 *
 * [x]/[y] is the lower left corner in PDF user space. Uses only PDFBox core API
 * (works with PDFBox 2 and 3) and replaces RoundRect from pdfbox-layout,
 * which was the only part of that library possy used.
 */
fun PDPageContentStream.addRoundRect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
    val r = minOf(radius, width / 2, height / 2)
    val k = r * BEZIER_CIRCLE
    val right = x + width
    val top = y + height

    moveTo(x + r, top)
    lineTo(right - r, top)
    curveTo(right - r + k, top, right, top - r + k, right, top - r)
    lineTo(right, y + r)
    curveTo(right, y + r - k, right - r + k, y, right - r, y)
    lineTo(x + r, y)
    curveTo(x + r - k, y, x, y + r - k, x, y + r)
    lineTo(x, top - r)
    curveTo(x, top - r + k, x + r - k, top, x + r, top)
    closePath()
}

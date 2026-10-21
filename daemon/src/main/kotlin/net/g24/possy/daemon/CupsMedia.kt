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

/** CUPS custom media name, e.g. "Custom.72x80mm" (same as `lp -o media=Custom.72x80mm`). */
internal fun customMedia(widthMm: Float, heightMm: Float): String = "Custom.${widthMm.mm()}x${heightMm.mm()}mm"

private fun Float.mm(): String = if (this % 1f == 0f) toInt().toString() else toString()

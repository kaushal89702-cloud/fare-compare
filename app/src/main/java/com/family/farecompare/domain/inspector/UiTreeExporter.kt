package com.family.farecompare.domain.inspector

/**
 * Exports a formatted UI tree dump to the device's shared Documents
 * directory (Documents/FareCompare/<fileName>). Returns the logical path
 * of the written file, or null if the export could not be completed (e.g.
 * missing legacy storage permission on very old Android versions) - never
 * throws.
 */
interface UiTreeExporter {
    suspend fun exportToDocuments(fileName: String, content: String): String?
}

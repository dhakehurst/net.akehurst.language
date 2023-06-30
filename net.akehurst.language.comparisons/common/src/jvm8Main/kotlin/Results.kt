/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.common

import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createFile

actual fun runTest(block: suspend () -> Unit):Unit = kotlinx.coroutines.runBlocking{ block() }

object Results {

    data class Result(
        val success: Boolean,
        val col: String,
        val fileData: FileData,
        val value: Duration
    )

    val resultsByCol = mutableMapOf<String, MutableMap<Int, Result>>()

    fun reset() {
        this.resultsByCol.clear()
    }

    @Synchronized
    fun log(success: Boolean, col: String, fileData: FileData, value: Duration) {
        val res = Result(success, col, fileData, value)
        val results = if (resultsByCol.containsKey(col)) {
            resultsByCol[col]!!
        } else {
            val m = mutableMapOf<Int, Result>()
            resultsByCol[col] = m
            m
        }
        results[fileData.index] = res
    }

    @Synchronized
    fun logError(col: String, fileData: FileData) {
        val res = Result(false, col, fileData, Duration.ZERO)
        val results = if (resultsByCol.containsKey(col)) {
            resultsByCol[col]!!
        } else {
            val m = mutableMapOf<Int, Result>()
            resultsByCol[col] = m
            m
        }
        results[fileData.index] = res
    }

    private fun dateTimeNow(): String {
        val t = ZonedDateTime.now()
        return t.format(DateTimeFormatter.ISO_DATE_TIME).substringBefore(".").replace(":", "_")
    }

    fun write() {
        try {
            val resultsFileIn = Paths.get("../results/results_Empty.xlsx")
            ZipSecureFile.setMinInflateRatio(0.00009)
            val wb: Workbook? = if (resultsFileIn.toFile().exists()) {
                WorkbookFactory.create(Files.newInputStream(resultsFileIn))
            } else {
                XSSFWorkbook()
            }
            var sheet: Sheet? = null
            if (0 == wb!!.numberOfSheets) {
                sheet = wb.createSheet()
            }
            sheet = wb.getSheetAt(0)

            for (results in this.resultsByCol.values) {
                for (result in results.values.sortedBy { it.fileData.index }) {
                    write(wb, sheet, result.success, result.col, result.fileData, result.value)
                }
            }
            val resultsFileOut = Paths.get("../results/results_${dateTimeNow()}.xlsx")
            resultsFileOut.createFile()
            Files.newOutputStream(resultsFileOut).use { fileOut -> wb.write(fileOut) }
        } catch (ex: Throwable) {
            throw RuntimeException("Error logging results", ex)
        }
    }

    private fun write(wb: Workbook, sheet: Sheet, success: Boolean, col: String, fileData: FileData, value: Duration) {
        try {
            val headerFont = wb.createFont()
            headerFont.bold = true
            headerFont.fontHeightInPoints = 14.toShort()
            headerFont.color = IndexedColors.BLUE.getIndex()
            val headerCellStyle = wb.createCellStyle()
            headerCellStyle.setFont(headerFont)
            val errorFont = wb.createFont()
            headerFont.bold = false
            headerFont.color = IndexedColors.RED.getIndex()
            val errorCellStyle = wb.createCellStyle()
            headerCellStyle.setFont(errorFont)

            var headerRow = sheet.getRow(0)
            if (null == headerRow) {
                headerRow = sheet.createRow(0)
            }
            val itemCol = headerRow!!.getCell(0)
            if (null == itemCol || itemCol.stringCellValue.isNullOrBlank()) {
                val cell = headerRow.createCell(0)
                cell.setCellValue("File")
                cell.cellStyle = headerCellStyle
                val cell2 = headerRow.createCell(1)
                cell2.setCellValue("Chars")
                cell2.cellStyle = headerCellStyle
                val cell3 = headerRow.createCell(2)
                cell3.setCellValue("CharsNoComments")
                cell3.cellStyle = headerCellStyle

            }
            var colNum = -1
            for (c in headerRow) {
                if (col == c.stringCellValue) {
                    colNum = c.columnIndex
                }
            }
            if (-1 == colNum) {
                val cell = headerRow.createCell(headerRow.lastCellNum.toInt())
                cell.setCellValue(col)
                cell.cellStyle = headerCellStyle
                colNum = cell.columnIndex
            }
            val rowNum = fileData.index + 1 //+1 for headings
            var valueRow = sheet.getRow(rowNum)
            if (null == valueRow) {
                valueRow = sheet.createRow(rowNum)
                val c1 = valueRow.createCell(0)
                c1.setCellValue(fileData.path.toString())
                val c2 = valueRow.createCell(1)
                c2.setCellValue(fileData.chars.toDouble())
                val c3 = valueRow.createCell(2)
                c3.setCellValue(fileData.charsNoComments.toDouble())
            } else {
                val c1 = valueRow.createCell(0)
                if (c1.stringCellValue.isNullOrBlank()) {
                    c1.setCellValue(fileData.path.toString())
                    val c2 = valueRow.createCell(1)
                    c2.setCellValue(fileData.chars.toDouble())
                    val c3 = valueRow.createCell(2)
                    c3.setCellValue(fileData.charsNoComments.toDouble())
                }
            }

            var valueCell = valueRow.getCell(colNum)
            if (null == valueCell) {
                valueCell = valueRow.createCell(colNum)
            }
            if (success) {
                valueCell!!.setCellValue(value.toNanos().div(1000).toDouble()) //micro seconds
            } else {
                valueCell.cellStyle = errorCellStyle
                valueCell.setBlank()
            }
        } catch (ex: Exception) {
            throw RuntimeException("Error writing $fileData", ex)
        }
    }


}
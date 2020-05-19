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

object Results {

    data class Result(
            val success: Boolean,
            val col: String,
            val fileData: FileData,
            val value: Duration
    )

    val results = mutableMapOf<Int, Result>()

    fun reset() {
        this.results.clear()
    }

    @Synchronized
    fun log(success: Boolean, col: String, fileData: FileData, value: Duration) {
        val res = Result(success, col, fileData, value)
        this.results[fileData.index] = res
    }

    fun write() {
        try {
            val resultsFile = Paths.get("../results/results.xlsx")
            ZipSecureFile.setMinInflateRatio(0.00009)
            var wb: Workbook? = if (resultsFile.toFile().exists()) {
                WorkbookFactory.create(Files.newInputStream(resultsFile))
            } else {
                XSSFWorkbook()
            }
            var sheet: Sheet? = null
            if (0 == wb!!.numberOfSheets) {
                sheet = wb.createSheet()
            }
            sheet = wb.getSheetAt(0)

            for (result in this.results.values.sortedBy { it.fileData.index }) {
                write(wb,sheet,result.success, result.col, result.fileData, result.value)
            }

            Files.newOutputStream(resultsFile).use { fileOut -> wb.write(fileOut) }

        } catch (ex: Exception) {
            throw RuntimeException("Error logging results", ex)
        }
    }

    fun write(wb:Workbook, sheet:Sheet, success: Boolean, col: String, fileData: FileData, value: Duration) {
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
        if (null == itemCol) {
            val cell = headerRow.createCell(0)
            cell.setCellValue("File")
            cell.cellStyle = headerCellStyle
            val cell2 = headerRow.createCell(1)
            cell2.setCellValue("Size")
            cell2.cellStyle = headerCellStyle
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
        var rowNum = fileData.index + 1 //+1 for headings
        if (sheet.lastRowNum < rowNum) {
            val row = sheet.createRow(sheet.lastRowNum + 1)
            val c = row.createCell(0)
            c.setCellValue(fileData.path.toString())
            val c2 = row.createCell(1)
            c2.setCellValue(fileData.size.toDouble())
            rowNum = row.rowNum
        }
        val valueRow = sheet.getRow(rowNum)
        var valueCell = valueRow.getCell(colNum)
        if (null == valueCell) {
            valueCell = valueRow.createCell(colNum)
        }
        if (success) {
            valueCell!!.setCellValue(value.toNanos().div(1000).toDouble()) //micro seconds
        } else {
            valueCell.cellStyle = errorCellStyle
            valueCell.setCellValue("error")
        }
    }


}
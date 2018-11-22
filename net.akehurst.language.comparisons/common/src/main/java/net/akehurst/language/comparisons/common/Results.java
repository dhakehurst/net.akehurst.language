/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.common;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

public class Results {

    static Path resultsFile = Paths.get("../results/results.xlsx");

    public static void log(boolean success, String col, String item, Duration value) {
        ZipSecureFile.setMinInflateRatio(0.00009);
        Workbook wb = null;
        try {
            if (resultsFile.toFile().exists()) {
                wb = WorkbookFactory.create(Files.newInputStream(resultsFile));
            } else {
                wb = new XSSFWorkbook();
            }

            Sheet sheet = null;
            if (0==wb.getNumberOfSheets()) {
                sheet = wb.createSheet();
            }
            sheet =wb.getSheetAt(0);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerFont.setColor(IndexedColors.BLUE.getIndex());
            CellStyle headerCellStyle = wb.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Font errorFont = wb.createFont();
            headerFont.setBold(false);
            headerFont.setColor(IndexedColors.RED.getIndex());
            CellStyle errorCellStyle = wb.createCellStyle();
            headerCellStyle.setFont(errorFont);

            Row headerRow = sheet.getRow(0);
            if (null==headerRow) {
                headerRow = sheet.createRow(0);
            }

            Cell itemCol = headerRow.getCell(0);
            if (null==itemCol) {
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Item");
                cell.setCellStyle(headerCellStyle);
            }

            int colNum = -1;
            for(Cell c: headerRow) {
                if (Objects.equals(col, c.getStringCellValue())) {
                    colNum = c.getColumnIndex();
                }
            }
            if (-1 == colNum) {
                Cell cell = headerRow.createCell(headerRow.getLastCellNum());
                cell.setCellValue(col);
                cell.setCellStyle(headerCellStyle);
                colNum = cell.getColumnIndex();
            }

            int rowNum = -1;
            for(Row row: sheet) {
                if (Objects.equals(item, row.getCell(0).getStringCellValue())) {
                    rowNum = row.getRowNum();
                }
            }
            if (-1 == rowNum) {
               Row row = sheet.createRow(sheet.getLastRowNum()+1);
               Cell c = row.createCell(0);
               c.setCellValue(item);
               rowNum = row.getRowNum();
            }

            Row valueRow = sheet.getRow(rowNum);
            Cell valueCell = valueRow.getCell(colNum);
            if (null==valueCell) {
                valueCell = valueRow.createCell(colNum);

            }
            valueCell.setCellValue(value.toString());
            if (!success) {
                valueCell.setCellStyle(errorCellStyle);
            }

            try (OutputStream fileOut = Files.newOutputStream(resultsFile)) {
                wb.write(fileOut);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

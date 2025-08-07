package com.rosteroptimization.service.export;

import com.rosteroptimization.service.optimization.model.Assignment;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RosterExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] exportToExcel(RosterPlan rosterPlan) throws IOException {
        log.info("Starting Excel export for roster plan");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Roster Plan");
            
            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Staff Name", "Staff ID", "Task", "Date", "Start Time", "End Time", "Department"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Sort assignments by staff name and date
            List<Assignment> sortedAssignments = rosterPlan.getAssignments().stream()
                    .sorted(Comparator.comparing((Assignment a) -> a.getStaff().getName())
                            .thenComparing(a -> a.getShift().getStartTime()))
                    .collect(Collectors.toList());
            
            // Fill data
            int rowNum = 1;
            for (Assignment assignment : sortedAssignments) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(assignment.getStaff().getName());
                row.createCell(1).setCellValue(assignment.getStaff().getId().toString());
                row.createCell(2).setCellValue(assignment.getTask().getName());
                row.createCell(3).setCellValue(assignment.getShift().getStartTime().format(DATE_FORMATTER));
                row.createCell(4).setCellValue(assignment.getShift().getStartTime().format(TIME_FORMATTER));
                row.createCell(5).setCellValue(assignment.getShift().getEndTime().format(TIME_FORMATTER));
                row.createCell(6).setCellValue(assignment.getTask().getDepartment().getName());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("Excel export completed successfully. Total assignments: {}", sortedAssignments.size());
            return outputStream.toByteArray();
        }
    }

    public byte[] exportToPdf(RosterPlan rosterPlan) throws DocumentException, IOException {
        log.info("Starting PDF export for roster plan");
        
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        // Add title
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
        Paragraph title = new Paragraph("Roster Plan", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Add generation info
        com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
        Paragraph info = new Paragraph();
        info.add(new Chunk("Generated: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
        info.add(Chunk.NEWLINE);
        info.add(new Chunk("Total Assignments: " + rosterPlan.getAssignments().size(), normalFont));
        info.add(Chunk.NEWLINE);
        info.add(new Chunk("Fitness Score: " + String.format("%.2f", rosterPlan.getFitnessScore()), normalFont));
        info.setSpacingAfter(20);
        document.add(info);
        
        // Create table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{20, 10, 20, 12, 10, 10, 18});
        
        // Table headers
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
        String[] headers = {"Staff Name", "Staff ID", "Task", "Date", "Start", "End", "Department"};
        
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
        
        // Sort assignments
        List<Assignment> sortedAssignments = rosterPlan.getAssignments().stream()
                .sorted(Comparator.comparing((Assignment a) -> a.getStaff().getName())
                        .thenComparing(a -> a.getShift().getStartTime()))
                .collect(Collectors.toList());
        
        // Add data rows
        com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        for (Assignment assignment : sortedAssignments) {
            table.addCell(new PdfPCell(new Phrase(assignment.getStaff().getName(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getStaff().getId().toString(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getTask().getName(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getShift().getStartTime().format(DATE_FORMATTER), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getShift().getStartTime().format(TIME_FORMATTER), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getShift().getEndTime().format(TIME_FORMATTER), dataFont)));
            table.addCell(new PdfPCell(new Phrase(assignment.getTask().getDepartment().getName(), dataFont)));
        }
        
        document.add(table);
        document.close();
        
        log.info("PDF export completed successfully. Total assignments: {}", sortedAssignments.size());
        return outputStream.toByteArray();
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
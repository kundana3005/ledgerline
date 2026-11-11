package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.StatementLine;
import com.ledgerline.backend.dto.StatementResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class StatementExportService {

    public byte[] csv(StatementResponse s) {
        StringBuilder sb = new StringBuilder();
        row(sb, "Account", s.accountNumber());
        row(sb, "Account type", s.accountType());
        row(sb, "Period (UTC)", s.from() + " to " + s.to());
        row(sb, "Opening balance", s.openingBalance().toPlainString());
        row(sb, "Closing balance", s.closingBalance().toPlainString());
        row(sb, "Total credits", s.totalCredits().toPlainString());
        row(sb, "Total debits", s.totalDebits().toPlainString());
        sb.append("\r\n");
        row(sb, "Date (UTC)", "Reference", "Type", "Direction", "Amount", "Balance after");
        for (StatementLine l : s.entries()) {
            row(sb, DateTimeFormatter.ISO_INSTANT.format(l.date()), l.reference(), l.type(), l.direction(),
                    l.amount().toPlainString(), l.balanceAfter().toPlainString());
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(cells[i]));
        }
        sb.append("\r\n");
    }

    /** Quotes the cell and neutralises spreadsheet formulas. */
    private static String cell(String v) {
        String t = v == null ? "" : v;
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0 && !t.matches("-?\\d+(\\.\\d+)?")) t = "'" + t;
        return t.contains(",") || t.contains("\"") || t.contains("\n") ? "\"" + t.replace("\"", "\"\"") + "\"" : t;
    }

    public byte[] pdf(StatementResponse s) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);

            doc.add(new Paragraph("LedgerLine account statement", title));
            doc.add(new Paragraph("Account: " + s.accountNumber() + " (" + s.accountType() + ")", body));
            doc.add(new Paragraph("Period (UTC): " + s.from() + " to " + s.to(), body));
            doc.add(new Paragraph("Opening balance: INR " + s.openingBalance().toPlainString(), bold));
            doc.add(new Paragraph("Closing balance: INR " + s.closingBalance().toPlainString(), bold));
            doc.add(new Paragraph("Total credits: INR " + s.totalCredits().toPlainString()
                    + "    Total debits: INR " + s.totalDebits().toPlainString(), body));
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(new float[]{3f, 5f, 2.2f, 2f, 2.3f, 2.5f});
            t.setWidthPercentage(100);
            for (String h : new String[]{"Date (UTC)", "Reference", "Type", "Direction", "Amount", "Balance after"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, bold));
                c.setBackgroundColor(new java.awt.Color(238, 242, 247));
                t.addCell(c);
            }
            for (StatementLine l : s.entries()) {
                t.addCell(new Phrase(DateTimeFormatter.ISO_INSTANT.format(l.date()).replace("T", " ").replace("Z", ""), small));
                t.addCell(new Phrase(l.reference(), small));
                t.addCell(new Phrase(l.type(), small));
                t.addCell(new Phrase(l.direction(), small));
                PdfPCell amt = new PdfPCell(new Phrase(l.amount().toPlainString(), small));
                amt.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(amt);
                PdfPCell bal = new PdfPCell(new Phrase(l.balanceAfter().toPlainString(), small));
                bal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(bal);
            }
            doc.add(t);
            if (s.entries().isEmpty()) doc.add(new Paragraph("No activity in this period.", body));
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build the statement PDF", e);
        }
    }
}
package com.paymentgateway.worker.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.paymentgateway.worker.repository.TransactionEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    public byte[] generateReceiptPdf(TransactionEntity transaction) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("Payment Gateway Engine", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Comprovante de Transacao", valueFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            addField(document, "ID da Transacao: ", String.valueOf(transaction.getId()), labelFont, valueFont);
            addField(document, "Merchant ID: ", String.valueOf(transaction.getMerchantId()), labelFont, valueFont);
            addField(document, "Valor: ", "R$ " + transaction.getAmount(), labelFont, valueFont);
            addField(document, "Metodo: ", String.valueOf(transaction.getPaymentMethod()), labelFont, valueFont);
            addField(document, "Status: ", String.valueOf(transaction.getStatus()), labelFont, valueFont);
            addField(document, "Emitido em: ",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    labelFont, valueFont);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    private void addField(Document document, String label, String value, Font labelFont, Font valueFont) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new com.lowagie.text.Chunk(label, labelFont));
        paragraph.add(new com.lowagie.text.Chunk(value, valueFont));
        document.add(paragraph);
    }
}

package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

@ApplicationScoped
public class PdfExtractor implements DocumentExtractor {

    @Override
    public boolean supports(String fileName) {

        return fileName.endsWith(".pdf");
    }

    public String extract(InputStream inputStream) {

        try {

            PDDocument document = Loader.loadPDF(inputStream.readAllBytes());

            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(document);

            document.close();

            return text;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
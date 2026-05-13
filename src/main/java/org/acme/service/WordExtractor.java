package org.acme.service;


import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.InputStream;

@ApplicationScoped
public class WordExtractor implements DocumentExtractor {

    @Override
    public boolean supports(String fileName) {

        return fileName.endsWith(".docx");
    }

    @Override
    public String extract(InputStream inputStream) {

        try (XWPFDocument document =
                     new XWPFDocument(inputStream)) {

            return document.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", (a, b) -> a + "\n" + b);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}

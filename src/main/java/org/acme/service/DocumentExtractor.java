package org.acme.service;

import java.io.InputStream;

public interface DocumentExtractor {

    boolean supports(String fileName);

    String extract(InputStream inputStream);
}

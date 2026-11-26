/*
 * Copyright 2015 Datalogics, Inc.
 */

package com.datalogics.pdf.samples.signature;

import static com.datalogics.pdf.samples.util.ContentTextItemMatchers.hasText;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.adobe.pdfjt.core.exceptions.PDFConfigurationException;
import com.adobe.pdfjt.core.exceptions.PDFFontException;
import com.adobe.pdfjt.core.exceptions.PDFIOException;
import com.adobe.pdfjt.core.exceptions.PDFInvalidDocumentException;
import com.adobe.pdfjt.core.exceptions.PDFInvalidParameterException;
import com.adobe.pdfjt.core.exceptions.PDFSecurityException;
import com.adobe.pdfjt.core.types.ASName;
import com.adobe.pdfjt.graphicsDOM.ContentItem;
import com.adobe.pdfjt.graphicsDOM.ContentTextItem;
import com.adobe.pdfjt.graphicsDOM.XObject;
import com.adobe.pdfjt.pdf.document.PDFDocument;
import com.adobe.pdfjt.pdf.document.PDFResources;
import com.adobe.pdfjt.pdf.graphics.font.PDFFont;
import com.adobe.pdfjt.pdf.graphics.font.impl.PDFFontUtils;
import com.adobe.pdfjt.pdf.graphics.xobject.PDFXObjectForm;
import com.adobe.pdfjt.pdf.interactive.annotation.PDFAnnotationWidget;
import com.adobe.pdfjt.pdf.page.PDFPage;
import com.adobe.pdfjt.services.digsig.SignatureFieldInterface;
import com.adobe.pdfjt.services.digsig.SignatureManager;
import com.adobe.pdfjt.services.rasterizer.impl.RasterContentItem;

import com.datalogics.pdf.samples.SampleTest;
import com.datalogics.pdf.samples.util.DocumentUtils;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the SignDocument Sample.
 */
public class SignDocumentTest extends SampleTest {
    private static final String FILE_NAME = "SignedField.pdf";
    private static final String QUALIFIED_SIGNATURE_FIELD_NAME = "Approver";
    private static final String BLACK_RIGHTWARDS_ARROW = "\u27a1"; // U+27A1 BLACK RIGHTWARDS ARROW
    private static File file = null;
    private static PDFDocument pdfDocument;

    @BeforeClass
    public static void setUpDocument() throws Exception {
        pdfDocument = null;
    }

    /**
     * Create the document if this is the first test run in the class.
     *
     * @throws Exception a general exception was thrown
     */
    private static void ensureDocument() throws Exception {
        if (pdfDocument == null) {
            final URL inputUrl = SignDocument.class.getResource(SignDocument.INPUT_UNSIGNED_PDF_PATH);

            file = SampleTest.newOutputFileWithDelete(FILE_NAME);

            // The complete file name will be set in the SignDocument class.
            final URL outputUrl = file.toURI().toURL();

            SignDocument.signExistingSignatureFields(inputUrl, outputUrl);

            pdfDocument = DocumentUtils.openPdfDocument(file.toURI().toURL());
        }
    }

    /**
     * Clean up after the tests.
     *
     * @throws Exception a general exception was thrown
     */
    @AfterClass
    public static void tearDownDocument() throws Exception {
        if (pdfDocument != null) {
            pdfDocument.close();
        }
        pdfDocument = null;
        file = null;
    }

    @Test
    public void theOutputFileMustExist() throws Exception {
        ensureDocument();
        assertTrue(file.getPath() + " must exist after run", file.exists());

    }

    @Test
    public void theSignatureFieldIsSigned() throws Exception {
        ensureDocument();

        // Make sure that Signature field is signed.
        final SignatureFieldInterface sigField = getSignedSignatureField(pdfDocument);
        assertTrue("Signature field must be signed", sigField.isSigned());
        assertTrue("Signature field must be visible", sigField.isVisible());
        assertEquals("Qualified field names must match", QUALIFIED_SIGNATURE_FIELD_NAME,
                     sigField.getQualifiedName());
    }


    @Test
    public void theCustomStringWasUsed() throws Exception {
        ensureDocument();

        final SignatureFieldInterface sigField = getSignedSignatureField(pdfDocument);
        final PDFPage signedPage = pdfDocument.requirePages().getPage(0);
        final PDFAnnotationWidget annot = (PDFAnnotationWidget) sigField.getPDFField().getPDFFieldSignature()
                                                                        .getAnnotation();
        PDFResources normFormResources = annot.getNormalStateAppearance().getResources();
        PDFXObjectForm frmXObject = (PDFXObjectForm) normFormResources.getXObject(ASName.create("FRM"));
        PDFResources frmResources = frmXObject.getResources();
        PDFXObjectForm n2Form = (PDFXObjectForm) frmResources.getXObject(ASName.create("n2"));
        final List<RasterContentItem> formContentItems = DocumentUtils.getFormContentItems(signedPage, n2Form,
                                                                                           null);
        XObject innerN2Form = (XObject) formContentItems.get(formContentItems.size() - 1);

        List<ContentTextItem<?, ?>> textItems = getContentTextItems(innerN2Form);
        textItems = textItems.subList(0, 11);

        assertThat(textItems, contains(hasText(BLACK_RIGHTWARDS_ARROW),
                                       hasText(" "),
                                       hasText("John"),
                                       hasText(" "),
                                       hasText("Doe"),
                                       hasText(" "),
                                       hasText("signed"),
                                       hasText(" "),
                                       hasText("this"),
                                       hasText(" "),
                                       hasText("document")));
    }

    private List<ContentTextItem<?, ?>> getContentTextItems(XObject overlayTextForm) {
        List<ContentTextItem<?, ?>> textItems = new ArrayList<>();
        for (ContentItem<?> item : new IteratorIterable<ContentItem<?>>(overlayTextForm.getContentItems()
                                                                                       .iterator())) {
            if (item instanceof ContentTextItem) {
                ContentTextItem<?, ?> textItem = (ContentTextItem<?, ?>) item;
                textItems.add(textItem);
            }
        }
        return textItems;
    }

    private static SignatureFieldInterface getSignedSignatureField(final PDFDocument doc)
                    throws PDFInvalidDocumentException, PDFIOException, PDFSecurityException {
        // Set up a signature service and get the first signature field.
        final SignatureManager sigService = SignatureManager.newInstance(doc);
        if (sigService.hasSignedSignatureFields()) {
            final Iterator<SignatureFieldInterface> iter = sigService.getDocSignatureFieldIterator();
            if (iter.hasNext()) {
                return iter.next();
            }
        }
        return null;
    }
}

package com.ddairways.model;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.*;
import edu.emory.mathcs.backport.java.util.Arrays;
import net.fortuna.ical4j.model.ValidationException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ElectronicBoardingPassTest {
    private static final Airport mumbai = new Airport("BOM", "Mumbai");
    private static final Airport chennai = new Airport("MAA", "Chennai");
    private static final org.joda.time.DateTime departure = org.joda.time.DateTime.now();
    private static final Flight flight = new Flight("9W", "465", mumbai, chennai, departure.toDate(), 2);
    private static final Passenger passenger = new Passenger("First", "Last", "first.last@company.com", "Economy");
    private static final String pnr = "A1B2C3";
    private static final String seat = "10D";
    private static final String seqNo = "0018";
    private static BoardingPass boardingPass;
    private static List<String> pdfDataAsText;
    private static byte[] pdfData;

    @BeforeClass
    public static void setUp() throws ValidationException, WriterException, IOException, DocumentException, URISyntaxException {
        boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo);
        pdfData = boardingPass.render(BoardingPass.Type.ELECTRONIC);
        pdfDataAsText = Arrays.asList(pdfToText(pdfData).split("\\n"));
    }

    @Test
    public void beginsWithAgentCopyAsTitle() throws Exception {
        final String firstLine = pdfDataAsText.get(0);
        assertTrue("Title 'Agent copy' must be present", firstLine.startsWith("Agent Copy"));
    }

    @Test
    public void passengerBoardingDetailsTitleAs1stLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails1stLine = pdfDataAsText.get(3);
        assertEquals("DD Airways Web Check-In Web Check-In", boardingDetails1stLine);
    }

    @Test
    public void passengerNameAndTravelClassAs2ndLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails2ndLine = pdfDataAsText.get(5);
        assertEquals("NAME: First Last CLASS: Economy NAME: First Last", boardingDetails2ndLine);
    }

    @Test
    public void passengerFlightNumberAndPNRAs3rdLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails3rdLine = pdfDataAsText.get(6);
        assertEquals("FLT: 9W465 PNR: A1B2C3 FLT: 9W465", boardingDetails3rdLine);
    }

    @Test
    public void flightOriginAndDepartureCitiesWithAirportCodesAs4thLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails4thLine = pdfDataAsText.get(7);
        assertEquals("FROM: Mumbai (BOM) TO: Chennai (MAA) FROM: BOM  TO: MAA", boardingDetails4thLine);
    }

    @Test
    public void flightDepAndArrTimeAs5thLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails5thLine = pdfDataAsText.get(8);
        final String timeInfo = String.format("DEP: %s ARR: %s", flight.getDepartureTime(), flight.getArrivalTime());
        final String expected = String.format("%s %s", timeInfo, timeInfo);
        assertEquals(expected, boardingDetails5thLine);
    }

    @Test
    public void passengerSeqNoSeatAndTravelDateAs6thLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails6thLine = pdfDataAsText.get(9);
        final String expected = String.format("SEQ: %s SEAT: %s DATE: %s", seqNo, seat, flight.getDepartureDate());
        assertEquals(expected, boardingDetails6thLine);
    }

    @Test
    public void travelDateAndPassengerSeatAs7thLineOfAgentCopyBoardingDetails() throws Exception {
        final String boardingDetails7thLine = pdfDataAsText.get(10);
        final String expected = String.format("DATE: %s SEAT: %s", flight.getDepartureDate(), seat);
        assertEquals(expected, boardingDetails7thLine);
    }

    @Test
    public void aCutPaperBelowInstructionSeparatesAgentCopyAndCustomerCopy() {
        final String cutLine = pdfDataAsText.get(11);
        assertEquals("cut on the dotted line below.", cutLine);
    }

    @Test
    public void customerCopyTitleAfterTheCutLine() {
        final String customerCopyTitleLine = pdfDataAsText.get(13);
        assertEquals("Title 'Customer Copy' must be present", "Customer Copy", customerCopyTitleLine);
    }

    @Test
    public void passengerInstructionsFollowCustomerCopyTitle() throws Exception {
        final List<String> instructions = pdfDataAsText.subList(14, 32);
        for (String instruction : instructions) {
            assertFalse("Instruction cannot be empty", instruction.isEmpty());
        }
    }

    @Test
    public void passengerBoardingDetailsTitleAs1stLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetailsFirstLine = pdfDataAsText.get(34);
        assertEquals("DD Airways Web Check-In Web Check-In", boardingDetailsFirstLine);
    }

    @Test
    public void passengerNameAndTravelClassAs2ndLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails1stLine = pdfDataAsText.get(36);
        assertEquals("NAME: First Last CLASS: Economy NAME: First Last", boardingDetails1stLine);
    }

    @Test
    public void passengerFlightNumberAndPNRAs3rdLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails3rdLine = pdfDataAsText.get(37);
        assertEquals("FLT: 9W465 PNR: A1B2C3 FLT: 9W465", boardingDetails3rdLine);
    }

    @Test
    public void flightOriginAndDepartureCitiesWithAirportCodesAs4thLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails4thLine = pdfDataAsText.get(38);
        assertEquals("FROM: Mumbai (BOM) TO: Chennai (MAA) FROM: BOM  TO: MAA", boardingDetails4thLine);
    }

    @Test
    public void flightDepAndArrTimeAs5thLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails5thLine = pdfDataAsText.get(39);
        final String timeInfo = String.format("DEP: %s ARR: %s", flight.getDepartureTime(), flight.getArrivalTime());
        final String expected = String.format("%s %s", timeInfo, timeInfo);
        assertEquals(expected, boardingDetails5thLine);
    }

    @Test
    public void passengerSeqNoSeatAndTravelDateAs6thLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails6thLine = pdfDataAsText.get(40);
        final String expected = String.format("SEQ: %s SEAT: %s DATE: %s", seqNo, seat, flight.getDepartureDate());
        assertEquals(expected, boardingDetails6thLine);
    }

    @Test
    public void travelDateAndPassengerSeatA7thLineOfCustomerCopyBoardingDetails() throws Exception {
        final String boardingDetails7thLine = pdfDataAsText.get(41);
        final String expected = String.format("DATE: %s SEAT: %s", flight.getDepartureDate(), seat);
        assertEquals(expected, boardingDetails7thLine);
    }

    @Test
    public void endsWithAirlineFooter() throws Exception {
        final String lastLine = pdfDataAsText.get(pdfDataAsText.size() - 1);
        assertEquals("Airline footer must be present", "DD Airways Electronic Boarding Pass - Wish you a Pleasant Flight", lastLine);
    }

    @Test
    public void hasTwoPDF417BarcodeFormatImage() throws Exception {
        final List<BufferedImage> images = extractImagesFromPdf(pdfData);
        assertEquals(2, images.size());
        final List<Result> results = toBarcodeResults(images);
        assertBarcodeFormat(BarcodeFormat.PDF_417, results.get(0).getBarcodeFormat());
        assertBarcodeFormat(BarcodeFormat.PDF_417, results.get(1).getBarcodeFormat());
    }

    @Test
    public void barcodeTextDoesNotIncludeGateInformation() throws Exception {
        final List<Result> results = toBarcodeResults(extractImagesFromPdf(pdfData));
        String expectedText = flight.getBarcodeData() + seat + seqNo + passenger.fullName();
        assertEquals(expectedText, results.get(0).getText());
        assertEquals(expectedText, results.get(1).getText());
    }

    private void assertBarcodeFormat(BarcodeFormat expected, BarcodeFormat actual) {
        assertEquals(expected, actual);
    }

    private static String pdfToText(byte[] pdfData) throws IOException {
        StringWriter s = new StringWriter();
        BufferedWriter bw = new BufferedWriter(s);
        PdfReader pr = new PdfReader(pdfData);
        int pages = pr.getNumberOfPages();
        //extract pdfAsText from each page and write it to the output pdfAsText file
        for (int page = 1; page <= pages; page++) {
            String text = PdfTextExtractor.getTextFromPage(pr, page);
            bw.write(text);
            bw.newLine();
        }
        pr.close();
        bw.flush();
        bw.close();
        return s.toString();
    }

    private List<Result> toBarcodeResults(List<BufferedImage> images, BarcodeFormat ...formats) throws NotFoundException {
        List<Result> results = new ArrayList<>();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(formats));
        hints.put(DecodeHintType.CHARACTER_SET, "ISO-8859-1");
        MultiFormatReader reader = new MultiFormatReader();
        for (BufferedImage image : images) {
            BufferedImageLuminanceSource lumSrc =
                    new BufferedImageLuminanceSource(image);
            HybridBinarizer hb = new HybridBinarizer(lumSrc);

            results.add(reader.decode(new BinaryBitmap(hb), hints));
            reader.reset();
        }
        return results;
    }

    private List<BufferedImage> extractImagesFromPdf(byte []pdfData) throws Exception {
        final List<BufferedImage> images = new ArrayList<>();
        final PdfReader reader = new PdfReader(pdfData);
        PdfReaderContentParser contentParser = new PdfReaderContentParser(reader);
        final int pages = reader.getNumberOfPages();
        //extract pdfImages from each page and add to images list
        for (int page = 1; page <= pages; page++) {
            contentParser.processContent(page, new RenderListener() {
                @Override
                public void beginTextBlock() {
                }

                public void renderText(TextRenderInfo renderInfo) {
                }

                @Override
                public void endTextBlock() {
                }

                @Override
                public void renderImage(ImageRenderInfo renderInfo) {
                    try {
                        PdfImageObject image = renderInfo.getImage();
                        assert (image != null);
                        images.add(image.getBufferedImage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        reader.close();
        return images;
    }
}

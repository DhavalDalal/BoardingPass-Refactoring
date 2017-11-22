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
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KioskBoardingPassTest {
    private static final Airport mumbai = new Airport("BOM", "Mumbai");
    private static final Airport chennai = new Airport("MAA", "Chennai");
    private static final org.joda.time.DateTime departure = org.joda.time.DateTime.now();
    private static final Flight flight = new Flight("9W", "465", mumbai, chennai, departure.toDate(), 2);
    private static final Passenger passenger = new Passenger("First", "Last", "first.last@company.com", "Economy");
    private static final String pnr = "A1B2C3";
    private static final String seat = "10D";
    private static final String seqNo = "0018";
    private static String gate = "45C";
    private static BoardingPass boardingPass;
    private static List<String> pdfAsText;
    private static byte[] pdfData;

    @BeforeClass
    public static void setUp() throws ValidationException, WriterException, IOException, DocumentException, URISyntaxException {
        boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo, gate);
        pdfData = boardingPass.render(BoardingPass.Type.KIOSK);
        pdfAsText = Arrays.asList(pdfToText(pdfData).split("\\n"));
    }

    @Test
    public void beginsWithAirlinesKioskCheckInHeader() throws Exception {
        final String header = pdfAsText.get(0);
        assertEquals("DD Airways Kiosk Check-In Kiosk Check-In", header);
    }

    @Test
    public void passengerNameAndTravelClassAs1stLine() throws Exception {
        final String boardingDetails1stLine = pdfAsText.get(1);
        assertEquals("NAME: First Last CLASS: Economy NAME: First Last", boardingDetails1stLine);
    }

    @Test
    public void passengerFlightNumberAndPNRAs2ndLine() throws Exception {
        final String boardingDetails2ndLine = pdfAsText.get(2);
        assertEquals("FLT: 9W465 PNR: A1B2C3 FLT: 9W465", boardingDetails2ndLine);
    }

    @Test
    public void flightOriginAndDepartureCitiesWithAirportCodesAs3rdLine() throws Exception {
        final String boardingDetails3rdLine = pdfAsText.get(3);
        assertEquals("FROM: Mumbai (BOM) TO: Chennai (MAA) FROM: BOM  TO: MAA", boardingDetails3rdLine);
    }

    @Test
    public void flightDepAndArrTimeAs4thLine() throws Exception {
        final String boardingDetails4thLine = pdfAsText.get(4);
        final String timeInfo = String.format("DEP: %s ARR: %s", flight.getDepartureTime(), flight.getArrivalTime());
        final String expected = String.format("%s %s", timeInfo, timeInfo);
        assertEquals(expected, boardingDetails4thLine);
    }

    @Test
    public void passengerSeqNoSeatAndTravelDateAs5thLine() throws Exception {
        final String boardingDetails5thLine = pdfAsText.get(5);
        final String expected = String.format("SEQ: %s SEAT: %s DATE: %s", seqNo, seat, flight.getDepartureDate());
        assertEquals(expected, boardingDetails5thLine);
    }

    @Test
    public void travelDateAndPassengerSeatA6thLine() throws Exception {
        final String boardingDetails6thLine = pdfAsText.get(6);
        final String expected = String.format("DATE: %s GATE: %s SEAT: %s   GATE: %s", flight.getDepartureDate(), gate, seat, gate);
        assertEquals(expected, boardingDetails6thLine);
    }

    @Test
    public void hasTwoPDF417BarcodeFormatImages() throws Exception {
        final List<Result> results = toBarcodeResults(extractImagesFromPdf(pdfData));
        assertEquals(2, results.size());
        assertBarcodeFormat(BarcodeFormat.PDF_417, results.get(0).getBarcodeFormat());
        assertBarcodeFormat(BarcodeFormat.PDF_417, results.get(1).getBarcodeFormat());
    }

    @Test
    public void barcodeTextIncludesGateInformation() throws Exception {
        final List<Result> results = toBarcodeResults(extractImagesFromPdf(pdfData));
        String expectedText = flight.getBarcodeData() + gate + seat + seqNo + passenger.fullName();
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

    private List<Result> toBarcodeResults(List<BufferedImage> images) throws NotFoundException {
        List<Result> results = new ArrayList<>();
        MultiFormatReader reader = new MultiFormatReader();
        for (BufferedImage image : images) {
            BufferedImageLuminanceSource lumSrc =
                    new BufferedImageLuminanceSource(image);
            HybridBinarizer hb = new HybridBinarizer(lumSrc);
            results.add(reader.decode(new BinaryBitmap(hb)));
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

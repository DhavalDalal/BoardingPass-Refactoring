package com.ddairways.model;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import edu.emory.mathcs.backport.java.util.Arrays;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChannelsTest {
    private final Airport mumbai = new Airport("BOM", "Mumbai");
    private final Airport chennai = new Airport("MAA", "Chennai");
    private final org.joda.time.DateTime departure = org.joda.time.DateTime.now();
    private final Flight flight = new Flight("9W", "465", mumbai, chennai, departure.toDate(), 2);
    private final Passenger passenger = new Passenger("First", "Last", "first.last@company.com", "Economy");
    private final String pnr = "A1B2C3";
    private final String seat = "10D";
    private final String seqNo = "0018";
    private String gate = "45C";
    private BoardingPass boardingPass;

    @Before
    public void setUp() throws Exception {
        boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo, gate);
    }

    @Test
    public void airportCheckinAtCounterProducesKioskBoardingPass() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.AIRPORT_COUNTER).get(0);
        List<String> pdfAsText = Arrays.asList(pdfToText(bytes).split(System.getProperty("line.separator")));
        final String header = pdfAsText.get(0);
        assertEquals("DD Airways Kiosk Check-In Kiosk Check-In", header);
    }

    @Test
    public void airportCheckinAtKioskProducesKioskBoardingPass() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.KIOSK).get(0);
        List<String> pdfAsText = Arrays.asList(pdfToText(bytes).split(System.getProperty("line.separator")));
        final String header = pdfAsText.get(0);
        assertEquals("DD Airways Kiosk Check-In Kiosk Check-In", header);
    }

    @Test
    public void onlineCheckinUsingDesktopProducesAnElectronicBoardingPass() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.DESKTOP).get(0);
        List<String> pdfAsText = Arrays.asList(pdfToText(bytes).split(System.getProperty("line.separator")));
        final String footer = pdfAsText.get(pdfAsText.size() - 1);
        assertEquals("Airline footer must be present", "DD Airways Electronic Boarding Pass - Wish you a Pleasant Flight", footer);
    }

    @Test
    public void onlineCheckinUsingDesktopProducesACalendarEvent() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.DESKTOP).get(1);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(new ByteArrayInputStream(bytes));
        final List<VEvent> calendarEvents = extractEvents(calendar);
        assertEquals(1, calendarEvents.size());
    }

    @Test
    public void OnlineCheckinUsingMobilePhoneOrTabletProducesAMobileBoardingPass() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.MOBILE).get(0);
        List<String> pdfAsText = Arrays.asList(pdfToText(bytes).split(System.getProperty("line.separator")));
        final String footer = pdfAsText.get(pdfAsText.size() - 1);
        assertEquals("Mobile Boarding Pass", footer);
    }

    @Test
    public void OnlineCheckinUsingMobilePhoneOrTabletProducesACalendarEvent() throws Exception {
        final byte[] bytes = boardingPass.render(BoardingPass.Channel.MOBILE).get(1);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(new ByteArrayInputStream(bytes));
        final List<VEvent> calendarEvents = extractEvents(calendar);
        assertEquals(1, calendarEvents.size());
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

    private static List<VEvent> extractEvents(Calendar calendar) {
        List<VEvent> events = new ArrayList<>();
        for (Iterator<Component> i = calendar.getComponents().iterator(); i.hasNext(); ) {
            Component component = i.next();
            if (component.getName().equals("VEVENT")) {
                VEvent event = (VEvent) component;
                events.add(event);
            }
        }
        return events;
    }

}

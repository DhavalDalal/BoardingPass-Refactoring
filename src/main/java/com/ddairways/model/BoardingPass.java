package com.ddairways.model;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.DottedLineSeparator;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.UidGenerator;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BoardingPass {
    public enum Channel {
        MOBILE, DESKTOP, KIOSK, AIRPORT_COUNTER;
    }

    public enum Type {
        MOBILE, ELECTRONIC, KIOSK, CALENDAR_EVENT;
    }

    private final Flight flight;
    private final Passenger passenger;
    private final String pnr;
    private final String seat;
    private final String seqNo;
    private final String gate;

    public BoardingPass(Flight flight, Passenger passenger, String pnr, String seat, String seqNo) {
        this(flight, passenger, pnr, seat, seqNo, null);
    }

    public BoardingPass(Flight flight, Passenger passenger, String pnr, String seat, String seqNo, String gate) {
        this.flight = flight;
        this.passenger = passenger;
        this.pnr = pnr;
        this.seat = seat;
        this.seqNo = seqNo;
        this.gate = StringUtils.isEmpty(gate) ? "" : gate;
    }

    private PdfPCell cell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(20f);
        return cell;
    }

    private void addRow(PdfPTable table, String s1, String s2, String s3) {
        table.addCell(cell(new Phrase(s1)));
        table.addCell(cell(new Phrase(s2)));
        table.addCell(cell(new Phrase("")));
        table.addCell(cell(new Phrase(s3)));
    }

    private PdfPCell cell(Phrase phrase, float height) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
//        cell.setBorder(Rectangle.BOX);
        cell.setFixedHeight(height);
        return cell;
    }

    private void addRow(PdfPTable table, String s1, String s2) {
        table.addCell(cell(new Phrase(s1), 18f));
        table.addCell(cell(new Phrase(s2), 18f));
    }

    private void rotate(PdfContentByte contentByte, Image image, int x, int y, float angleInRadians, float scaleWidthFactor, float scaleHeightFactor) throws DocumentException {
//        contentByte.addImage(image, image.getWidth(), 0, 0, image.getHeight(), x, y);
        // Draw image as if the previous image was rotated around its center
        AffineTransform A = AffineTransform.getTranslateInstance(-0.5, -0.5);
        // Stretch it to its dimensions
        AffineTransform B = AffineTransform.getScaleInstance(image.getWidth() * scaleWidthFactor, image.getHeight() * scaleHeightFactor);
        // Rotate it
        AffineTransform C = AffineTransform.getRotateInstance(angleInRadians);
        // Move it to have the same center as above
        AffineTransform D = AffineTransform.getTranslateInstance(x + image.getWidth()/2, y + image.getHeight()/2);
        // Concatenate
        AffineTransform M = (AffineTransform) A.clone();
        M.preConcatenate(B);
        M.preConcatenate(C);
        M.preConcatenate(D);
        //Draw
        contentByte.addImage(image, M);
    }

    private byte [] createPdfElectronicBoardingPass() throws IOException, DocumentException, WriterException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document document = new Document();
        final PdfWriter pdfWriter = PdfWriter.getInstance(document, os);

        document.open();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(new float[] {225,225,5,190}, document.getPageSize());

        // Generate Barcode PDF417
        MultiFormatWriter writer = new MultiFormatWriter();
//        Map<EncodeHintType, String> hints = new HashMap<>();
//        hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
        BitMatrix bitMatrix = writer.encode(getBarcodeText(Type.ELECTRONIC), BarcodeFormat.PDF_417, 40, 15);
        BufferedImage barcode = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream barcodeBaos = new ByteArrayOutputStream();
        ImageIO.write(barcode, "jpg", barcodeBaos);
        Image itextImage = Image.getInstance(barcodeBaos.toByteArray());

        final String name = "NAME: " + passenger.fullName();
        final String flightNumber = "FLT: " + flight.getCompleteNumber();
        final String arrTime = "ARR: " + flight.getArrivalTime();
        final String depTime = "DEP: " + flight.getDepartureTime();
        final String passengerSeat = "SEAT: " + seat;
        final String depDate = "DATE: " + flight.getDepartureDate();
        addRow(table, name, "CLASS: " + passenger.getTravelClass(), name);
        addRow(table, flightNumber, "PNR: " + pnr, flightNumber);
        addRow(table, flight.getOriginCityWithAirportCode(), flight.getDestinationCityWithAirportCode(), flight.getOriginDestinationAirportCodes());
        addRow(table, depTime, arrTime, depTime + " " +  arrTime);
        addRow(table, "SEQ: " + seqNo, passengerSeat, depDate);
        addRow(table, depDate, "", passengerSeat);

        // Add Agent Copy of Boarding Pass
        final Phrase agentCopy = new Phrase("Agent Copy");
        document.add(agentCopy);

        Paragraph blankLine = new Paragraph("  ");
        document.add(blankLine);
        document.add(blankLine);
        document.add(blankLine);
        document.add(table);

        PdfContentByte canvas = pdfWriter.getDirectContentUnder();
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 18);

        // Agent Copy Passenger Stub - Top Header
        canvas.beginText();
        canvas.moveText(60, 765);
        //cb.SetFontAndSize(bf, 12);
        canvas.showText("DD Airways Web Check-In");
        canvas.endText();

        // Agent Copy Airline Stub - Top Header
        canvas.beginText();
        canvas.moveText(420, 765);
        canvas.showText("Web Check-In");
        canvas.endText();

        // Add Agent Copy Barcode Vertically towards Right
        // separating airline stub and passenger stub
        rotate(canvas, itextImage, 275, 650, (float) Math.PI/2, 0.75f, 0.75f);

        // Add rectangle with border to visually group agent copy
        Rectangle rect = new Rectangle(10, 630, 580, 790);
        rect.setBorder(Rectangle.BOX);
        rect.setBorderWidth(1);
        canvas.rectangle(rect);

        // Add dotted line - Marking Start of Customer Copy and End of Agent Copy
        Paragraph separator = new Paragraph("cut on the dotted line below.");
        separator.setAlignment(Element.ALIGN_CENTER);
        DottedLineSeparator dottedline = new DottedLineSeparator();
        dottedline.setOffset(-2);
        dottedline.setGap(2f);
        separator.add(dottedline);
        document.add(separator);

        document.add(blankLine);
        document.add(new Phrase("Customer Copy"));

        InputStream in = getClass().getClassLoader().getResourceAsStream("electronic-boarding-pass-instructions.txt");
        final DataInputStream dis = new DataInputStream(in);
        int readByte;
        StringBuilder instructions = new StringBuilder();
        while((readByte = dis.read()) != -1) {
            instructions.append((char)readByte);
        }

        document.add(new Paragraph(instructions.toString()));

        // Add Customer Copy of Boarding Pass
        document.add(blankLine);
        document.add(blankLine);
        document.add(blankLine);

        // Customer Copy Passenger Stub - Top Header
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 18);
        canvas.beginText();
        canvas.moveText(60, 215);
        canvas.showText("DD Airways Web Check-In");
        canvas.endText();

        // Customer Copy Airline Stub - Top Header
        canvas.beginText();
        canvas.moveText(420, 215);
        canvas.showText("Web Check-In");
        canvas.endText();

        // Customer Copy Boarding Pass Details
        document.add(table);

        //contentByte.addImage(itextImage, itextImage.getWidth(), 0, 0, itextImage.getHeight(), itextImage.getAbsoluteX(), itextImage.getAbsoluteY());
        // Add vertical barcode to right side separating
        // airline stub and passenger stub for Customer Copy
        rotate(canvas, itextImage, 275, 100, (float) Math.PI/2, 0.75f, 0.75f);

        rect = new Rectangle(10, 240, 580, 75);
        rect.setBorder(Rectangle.BOX);
        rect.setBorderWidth(1);
        canvas.rectangle(rect);

        // Footer
        canvas.beginText();
        canvas.moveText(25, 40);
        canvas.showText("DD Airways Electronic Boarding Pass - Wish you a Pleasant Flight");
        canvas.endText();

        document.close();

        return os.toByteArray();
    }

    private String getBarcodeText(Type type) {
        if (type == Type.MOBILE) {
            return flight.getBarcodeData() + seat + seqNo + passenger.fullName();
        }

        if (type == Type.KIOSK) {
            return flight.getBarcodeData() + gate + seat + seqNo + passenger.fullName();
        }

        if (type == Type.ELECTRONIC) {
            return flight.getBarcodeData() + seat + seqNo + passenger.fullName();
        }
        return "";
    }

    private byte [] createPdfKioskBoardingPass() throws IOException, DocumentException, WriterException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        Rectangle pageSize = new Rectangle(595, 220);
        Document document = new Document(pageSize);
        final PdfWriter pdfWriter = PdfWriter.getInstance(document, os);

        document.open();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(new float[] {225,225,5,190}, pageSize);
        //first 2 cols for passenger copy and last column for airline copy
        final String name = "NAME: " + passenger.fullName();
        final String airportGate = "GATE: " + gate;
        final String passengerSeat = "SEAT: " + seat;
        final String flightNumber = "FLT: " + flight.getCompleteNumber();
        final String arrTime = "ARR: " + flight.getArrivalTime();
        final String depTime = "DEP: " + flight.getDepartureTime();
        final String depDate = "DATE: " + flight.getDepartureDate();
        addRow(table, name, "CLASS: " + passenger.getTravelClass(), name);
        addRow(table, flightNumber, "PNR: " + pnr, flightNumber);
        addRow(table, flight.getOriginCityWithAirportCode(), flight.getDestinationCityWithAirportCode(), flight.getOriginDestinationAirportCodes());
        addRow(table, depTime, arrTime, depTime + " " + arrTime);
        addRow(table, "SEQ: " + seqNo, passengerSeat, depDate);
        addRow(table, depDate, airportGate, passengerSeat + "   " + airportGate);
        document.add(table);

        // Add vertical dashed line to right side separating
        // airline copy and passenger copy
        PdfContentByte contentByte = pdfWriter.getDirectContent();
        contentByte.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 24);
        contentByte.setLineDash(3, 3, 0);
        // move to right side as less space is desired for airline stub
        // and more space for passenger stub.
        contentByte.moveTo(410, 525);
        contentByte.lineTo(410, 0);
        contentByte.stroke();

        // Generate Barcode PDF417
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(getBarcodeText(Type.KIOSK), BarcodeFormat.PDF_417, 400, 80);
        BufferedImage barcode = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream barcodeBaos = new ByteArrayOutputStream();
        ImageIO.write(barcode, "jpg", barcodeBaos);
        Image itextImage = Image.getInstance(barcodeBaos.toByteArray());

        //contentByte.addImage(itextImage, itextImage.getWidth(), 0, 0, itextImage.getHeight(), itextImage.getAbsoluteX(), itextImage.getAbsoluteY());
        // barcode on left passenger copy
        contentByte.addImage(itextImage, 300, 0, 0, 60, 50, itextImage.getAbsoluteY());
        //barcode on right airline copy
        contentByte.addImage(itextImage, 120, 0, 0, 60, 420, itextImage.getAbsoluteY());

        // Passenger Copy - Top Header
        contentByte.beginText();
        contentByte.moveText(60, 190);
        contentByte.showText("DD Airways Kiosk Check-In");
        contentByte.endText();

        // Airline Copy - Top Header
        contentByte.beginText();
        contentByte.moveText(420, 190);
        contentByte.showText("Kiosk Check-In");
        contentByte.endText();

        document.close();

        return os.toByteArray();
    }

    private byte [] createPdfMobileBoardingPass() throws IOException, DocumentException, WriterException {
        ByteArrayOutputStream boardingPass = new ByteArrayOutputStream();
        Rectangle pageSize = new Rectangle(220, 340);
        Document document = new Document(pageSize);
        final PdfWriter pdfWriter = PdfWriter.getInstance(document, boardingPass);
        document.open();

        //Generate Aztec Barcode
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(getBarcodeText(Type.MOBILE), BarcodeFormat.AZTEC, 135, 135);
        BufferedImage aztecBarcode = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(aztecBarcode, "jpg", jpeg);
        Image itextImage = Image.getInstance(jpeg.toByteArray());
        document.add(itextImage);

        //Boarding Pass Info as Table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(new float[]{110, 110}, pageSize);
        addRow(table, passenger.getFirstName(), passenger.getLastName());
        addRow(table, flight.getCode(), flight.getNumber());
        addRow(table, flight.originAirportCode(), flight.destinationAirportCode());
        addRow(table, flight.getDepartureTime(), flight.getArrivalTime());
        addRow(table, passenger.getTravelClass(), seat);
        addRow(table, "DATE", flight.getDepartureDate());
        addRow(table, "PNR", pnr);
        document.add(table);

        PdfContentByte contentByte = pdfWriter.getDirectContent();
        contentByte.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 16);

        //Boarding Pass Footer
        contentByte.beginText();
        contentByte.moveText(35, 10);
        contentByte.showText("Mobile Boarding Pass");
        contentByte.endText();

        //Boarding Pass Header
        contentByte.beginText();
        contentByte.moveText(70, 315);
        contentByte.showText("DD Airways");
        contentByte.endText();

        document.close();

        return boardingPass.toByteArray();
    }

    private byte [] createCalendarEventWithBoardingPass(byte [] attachBoardingPass) throws IOException, URISyntaxException, ValidationException {
        // Create a TimeZone
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone("Asia/Calcutta");
        VTimeZone tz = timezone.getVTimeZone();

        // Create the event
        String eventName = String.format("Flight to %s (%s)", flight.getDestinationCity(), flight.getCompleteNumber());
        org.joda.time.DateTime departure = new org.joda.time.DateTime(flight.getDeparture());

        DateTime start = new DateTime(departure.toDate());
        DateTime end = new DateTime(departure.plusHours(2).toDate());
        VEvent flightTravel = new VEvent(start, end, eventName);

        // add timezone info..
        flightTravel.getProperties().add(tz.getTimeZoneId());

        // generate unique identifier..
        UidGenerator ug = new UidGenerator("uidGen");
        Uid uid = ug.generateUid();
        flightTravel.getProperties().add(uid);

        // add attendees for Event
        Attendee attendee = new Attendee(passenger.getEmailUri());
        attendee.getParameters().add(Role.REQ_PARTICIPANT);
        attendee.getParameters().add(new Cn(passenger.fullName()));
        flightTravel.getProperties().add(attendee);

        // Set Alarm for the the above Event
        //Creating an alarm to trigger one (3) hour before the scheduled start of the parent event
        VAlarm reminder = new VAlarm(new Dur(0, -3, 0, 0));
        // repeat reminder four (4) more times every thirty (30) minutes..
        reminder.getProperties().add(new Repeat(4));
        reminder.getProperties().add(new Duration(new Dur(0, 0, 30, 0)));

        //display a message for the Alarm
        reminder.getProperties().add(Action.AUDIO);
        reminder.getProperties().add(new Description(eventName));
        flightTravel.getAlarms().add(reminder);

        // Create a calendar
        net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
        icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.getProperties().add(CalScale.GREGORIAN);

        // Attach boarding pass
        if (attachBoardingPass != null) {
            ParameterList params = new ParameterList();
            params.add(Value.BINARY);
            params.add(Encoding.BASE64);
            params.add(new FmtType("Mobile Boarding Pass.pdf"));
            Attach attach = new Attach(params, attachBoardingPass);
            flightTravel.getProperties().add(attach);
        }

        // Add the event and print
        icsCalendar.getComponents().add(flightTravel);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.setValidating(false);
        outputter.output(icsCalendar, out);
        return out.toByteArray();
    }

    private byte[] render(Type type) throws URISyntaxException, WriterException, ValidationException, DocumentException, IOException {
        try {
            if (type == Type.MOBILE) {
                return createPdfMobileBoardingPass();
            }

            if (type == Type.KIOSK) {
                return createPdfKioskBoardingPass();
            }

            if (type == Type.ELECTRONIC) {
                return createPdfElectronicBoardingPass();
            }

            if (type == Type.CALENDAR_EVENT) {
                byte[] attachMobileBoardingPass = createPdfMobileBoardingPass();
                return createCalendarEventWithBoardingPass(attachMobileBoardingPass);
            }
        } catch (Exception e) {
            throw e;
        }
        return new byte[]{};
    }


    public List<byte[]> render(Channel channel) throws Exception {
        if(channel == Channel.AIRPORT_COUNTER || channel == Channel.KIOSK) {
            return Arrays.asList(render(Type.KIOSK));
        }
        if(channel == Channel.DESKTOP) {
            return Arrays.asList(render(Type.ELECTRONIC), render(Type.CALENDAR_EVENT));
//            return Arrays.asList(render(Type.MOBILE), render(Type.CALENDAR_EVENT));
        }
        if(channel == Channel.MOBILE) {
            return Arrays.asList(render(Type.MOBILE), render(Type.CALENDAR_EVENT));
        }
        return Collections.emptyList();
    }

    public void writeFileFor(Channel channel) throws Exception {
        FileOutputStream fos = null;
        final List<byte[]> bytes = render(channel);
        if(channel == Channel.AIRPORT_COUNTER || channel == Channel.KIOSK) {
            fos = new FileOutputStream("kiosk-boarding-pass-" + pnr + "-" + passenger.getLastName() + ".pdf");
            fos.write(bytes.get(0));
        }
        if(channel == Channel.DESKTOP) {
            fos = new FileOutputStream("desktop-boarding-passes-" + pnr + "-" + passenger.getLastName() + ".zip");
            ZipOutputStream zip = new ZipOutputStream(fos);
            final java.util.List<String> fileNames = Arrays.asList(
                    "electronic-boarding-pass-" + pnr + "-" + passenger.getLastName() + ".pdf",
                    "calendar-boarding-event-" + pnr + "-" + passenger.getLastName()  + ".ics");
            for(int i = 0; i < fileNames.size(); i++) {
                final byte[] nextBytes = bytes.get(i);
                ZipEntry zipEntry = new ZipEntry(fileNames.get(i));
                zip.putNextEntry(zipEntry);
                zip.write(nextBytes);
                zip.closeEntry();
            }
            zip.finish();
            zip.close();
        }
        if(channel == Channel.MOBILE) {
            fos = new FileOutputStream("mobile-boarding-pass-" + pnr + "-" + passenger.getLastName() + ".pdf");
            fos.write(bytes.get(0));
            fos.flush();
            fos.close();
            fos = new FileOutputStream("calendar-boarding-event-" + pnr + "-" + passenger.getLastName() + ".ics");
            fos.write(bytes.get(1));
        }
        fos.flush();
        fos.close();
    }
}
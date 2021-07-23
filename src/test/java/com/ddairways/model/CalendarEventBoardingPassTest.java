package com.ddairways.model;

import com.google.zxing.WriterException;
import com.itextpdf.text.DocumentException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.model.property.*;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CalendarEventBoardingPassTest {
    private static final Airport mumbai = new Airport("BOM", "Mumbai");
    private static final Airport chennai = new Airport("MAA", "Chennai");
    private static final org.joda.time.DateTime departure = org.joda.time.DateTime.now();
    private static final int flightDurationInHrs = 2;
    private static final Flight flight = new Flight("9W", "465", mumbai, chennai, departure.toDate(), flightDurationInHrs);
    private static final Passenger passenger = new Passenger("First", "Last", "first.last@company.com", "Economy");
    private static final String pnr = "A1B2C3";
    private static final String seat = "10D";
    private static final String seqNo = "0018";
    private static BoardingPass boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo);
    private static List<VEvent> calendarEvents;

    @BeforeClass
    public static void setUp() throws Exception {
        byte[] calendarEventData = boardingPass.render(BoardingPass.Channel.MOBILE).get(1);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(new ByteArrayInputStream(calendarEventData));
        calendarEvents = extractEvents(calendar);
    }

    @Test
    public void has1Event() throws IOException, ParserException {
        assertEquals(1, calendarEvents.size());
    }

    @Test
    public void eventSummaryContainsFlightDestinationCityAndNumber() {
        final VEvent travelEvent = calendarEvents.get(0);
        final Summary summary = travelEvent.getSummary();
        final String expected = String.format("Flight to %s (%s)", flight.getDestinationCity(), flight.getCompleteNumber());
        assertEquals(expected, summary.getValue());
    }

    @Test
    public void eventStartDateTimeIsFlightDepartureDateTime() {
        final VEvent travelEvent = calendarEvents.get(0);
        final Date eventStartDate = travelEvent.getStartDate().getDate();
        final org.joda.time.DateTime departureDateTime = new DateTime(flight.getDeparture());
        final net.fortuna.ical4j.model.DateTime departure = new net.fortuna.ical4j.model.DateTime(departureDateTime.toDate());
        assertEquals(departure, eventStartDate);
    }

    @Test
    public void eventEndDateTimeIsFlightArrivalDateTime() {
        final VEvent travelEvent = calendarEvents.get(0);
        final Date eventEndDate = travelEvent.getEndDate().getDate();
        final org.joda.time.DateTime departureDateTime = new DateTime(flight.getDeparture());
        final org.joda.time.DateTime arrivalDateTime = departureDateTime.plusHours(flightDurationInHrs);
        net.fortuna.ical4j.model.DateTime arrival = new net.fortuna.ical4j.model.DateTime(arrivalDateTime.toDate());
        assertEquals(arrival, eventEndDate);
    }

    @Test
    public void eventHasPassengerDetails() throws URISyntaxException {
        final VEvent travelEvent = calendarEvents.get(0);
        final Attendee attendee = (Attendee) travelEvent.getProperties("ATTENDEE").get(0);
        Attendee expected = new Attendee(passenger.getEmailUri());
        expected.getParameters().add(Role.REQ_PARTICIPANT);
        expected.getParameters().add(new Cn(passenger.fullName()));
        assertEquals(expected, attendee);
    }

    @Test
    public void eventHasPassengerMobileBoardingPassAttached() throws Exception {
        final VEvent travelEvent = calendarEvents.get(0);
        final Attach mobileBoardingPass = (Attach) travelEvent.getProperties("ATTACH").get(0);
        ParameterList params = new ParameterList();
        params.add(Value.BINARY);
        params.add(Encoding.BASE64);
        params.add(new FmtType("Mobile Boarding Pass.pdf"));
        byte[] attachBoardingPass = boardingPass.render(BoardingPass.Channel.MOBILE).get(0);
        Attach expected = new Attach(params, attachBoardingPass);
        assertEquals(expected.getBinary().length, mobileBoardingPass.getBinary().length);
    }

        @Test
    public void eventHasExactlyOneAlarmReminder() {
        assertEquals(1, getAlarmsFor(calendarEvents).size());
    }

    @Test
    public void reminderAlarmTriggers3HoursPriorToTheEvent() {
        final VAlarm alarm = getAlarmsFor(calendarEvents).get(0);
        final Trigger trigger = alarm.getTrigger();
        assertEquals(new Dur(0, -3, 0, 0), trigger.getDuration());
    }

    @Test
    public void reminderAlarmRepeats4TimesEvery30mins() {
        final VAlarm alarm = getAlarmsFor(calendarEvents).get(0);
        final Repeat repeat = alarm.getRepeat();
        assertEquals("4", repeat.getValue());
        final Duration duration = alarm.getDuration();
        assertEquals(new Dur(0, 0, 30, 0), duration.getDuration());
    }

    @Test
    public void reminderAlarmSoundsItselfWhenItTriggers() {
        final VAlarm alarm = getAlarmsFor(calendarEvents).get(0);
        final Action sound = alarm.getAction();
        assertEquals("AUDIO", sound.getValue());
    }

    @Test
    public void reminderAlarmDisplaysWithDescriptionItTriggers() {
        final VAlarm alarm = getAlarmsFor(calendarEvents).get(0);
        final Description description = alarm.getDescription();
        final String expectedDescription = String.format("Flight to %s (%s)", flight.getDestinationCity(), flight.getCompleteNumber());
        assertEquals(expectedDescription, description.getValue());
    }

    private static List<VAlarm> getAlarmsFor(List<VEvent> calendarEvents) {
        final VEvent travelEvent = calendarEvents.get(0);
        return travelEvent.getAlarms();
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

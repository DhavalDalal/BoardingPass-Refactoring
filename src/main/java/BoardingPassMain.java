import com.ddairways.model.Airport;
import com.ddairways.model.BoardingPass;
import com.ddairways.model.Flight;
import com.ddairways.model.Passenger;

public class BoardingPassMain {
    public static void main(String[] args) throws Exception {
        final Airport mumbai = new Airport("BOM", "Mumbai");
        final Airport chennai = new Airport("MAA", "Chennai");
        final org.joda.time.DateTime departure = org.joda.time.DateTime.now();
        final Flight flight = new Flight("9W", "465", mumbai, chennai, departure.toDate(), 2);
        final Passenger passenger = new Passenger("First", "Last", "first.last@company.com", "Economy");
        final String pnr = "A1B2C3";
        final String seat = "10D";
        final String seqNo = "0018";
        BoardingPass boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo);
        boardingPass.renderFor(BoardingPass.Channel.DESKTOP);
        boardingPass.renderFor(BoardingPass.Channel.MOBILE);

        //Kiosk and Counter boarding passes have gate information
        final String gate = "45C";
        boardingPass = new BoardingPass(flight, passenger, pnr, seat, seqNo, gate);
        boardingPass.renderFor(BoardingPass.Channel.AIRPORT_COUNTER);
        boardingPass.renderFor(BoardingPass.Channel.KIOSK);
    }

}

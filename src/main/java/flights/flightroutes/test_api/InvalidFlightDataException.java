package flights.flightroutes.test_api;

public class InvalidFlightDataException extends Exception {
    public InvalidFlightDataException(String errorMsg){
        super(errorMsg);
    }
}

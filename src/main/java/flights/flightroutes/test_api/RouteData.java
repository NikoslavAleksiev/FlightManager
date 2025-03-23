package flights.flightroutes.test_api;

import java.util.ArrayList;
import java.util.List;

public class RouteData {

    private ArrayList<String> route;
    private int price;

    public RouteData(ArrayList<String> route, int price) {
        this.route = route;
        this.price = price;
    }

    public List<String> getRoute() {
        return route;
    }

    public int getPrice() {
        return price;
    }
}

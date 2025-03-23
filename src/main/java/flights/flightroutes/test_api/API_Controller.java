package flights.flightroutes.test_api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/api")
public class API_Controller {
    private static String DATA_FILE_PATH = "src/main/java/flights/flightroutes/test_api/files/inputData.txt";

    @PostMapping("/routes")
    public ResponseEntity<?> getRoutes(@RequestBody Map<String, String> request) {
        String origin = request.get("origin");
        String destination = request.get("destination");
        String maxFlightsStr = request.get(("maxFlights"));

        String airportCodePattern = "^[A-Z]{3}$";

        if (origin == null || !origin.matches(airportCodePattern) ||
            destination == null  || !destination.matches(airportCodePattern)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid Data");
        }

        if (!(maxFlightsStr == null) && !maxFlightsStr.matches("^[1-9][0-9]*$")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid Data");
        }

        int maxFlights = (maxFlightsStr != null) ? Integer.parseInt(maxFlightsStr) : Integer.MAX_VALUE;
        HashMap<String, ArrayList<Flight>> graph = createFlightDataGraphFromFile();
        ArrayList<ArrayList<String>> allRoutes = new ArrayList<ArrayList<String>>();
        ArrayList<Integer> prices = new ArrayList<Integer>();

        findAllRoutesAndPrice(graph, allRoutes, prices, origin, destination);
        ArrayList<RouteData> sortedAndFilteredRoutesByPrice = sortRoutesByPriceAndApplyFilters(allRoutes, prices,maxFlights);

        if (sortedAndFilteredRoutesByPrice.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No routes");

        return ResponseEntity.ok(sortedAndFilteredRoutesByPrice);
    }

    public static HashMap<String, ArrayList<Flight>> createFlightDataGraphFromFile() {
        HashMap<String, ArrayList<Flight>> g = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(DATA_FILE_PATH);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("Flights:"))
                    continue;
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    if (!g.keySet().contains(parts[0])) {
                        ArrayList<Flight> af = new ArrayList<>();
                        Flight f = new Flight(parts[1], Integer.parseInt(parts[2]));
                        af.add(f);
                        g.put(parts[0], af);
                    } else {
                        ArrayList<Flight> af = g.get(parts[0]);
                        Flight f = new Flight(parts[1], Integer.parseInt(parts[2]));
                        af.add(f);
                        g.put(parts[0], af);
                    }
                }
                if (g.isEmpty() || g == null)
                    throw new InvalidFlightDataException("No flights listed!");
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (InvalidFlightDataException e) {
            e.printStackTrace();
            System.out.println();
        }
        return g;
    }

    public static void findAllRoutesAndPrice(HashMap<String, ArrayList<Flight>> graph,
            ArrayList<ArrayList<String>> allRoutes, ArrayList<Integer> prices, String current, String finDestination) {
        int price = 0;
        ArrayList<String> route = new ArrayList<String>();
        findNextCity(graph, allRoutes, current, finDestination, prices, route, price);
    }

    public static void findNextCity(HashMap<String, ArrayList<Flight>> graph, ArrayList<ArrayList<String>> allRoutes,
            String current, String finDestination, ArrayList<Integer> prices, ArrayList<String> route, int price) {
        route.add(current);
        if (current.equals(finDestination)) {
            allRoutes.add(new ArrayList<>(route));
            prices.add(price);
        } else {
            for (Flight f : graph.getOrDefault(current, new ArrayList<>())) {
                if (!route.contains(f.destination)) {
                    findNextCity(graph, allRoutes, f.destination, finDestination, prices, route, price + f.price);
                }
            }
        }
        route.remove(route.size() - 1);
    }

    public static ArrayList<RouteData> sortRoutesByPriceAndApplyFilters(ArrayList<ArrayList<String>> allRoutes,
            ArrayList<Integer> prices,
            int maxFlights) {
        ArrayList<RouteData> sortedRoutesByPrice = new ArrayList<>();
        for (int i = 0; i < allRoutes.size(); i++) {
            if (allRoutes.get(i).size() - 1 <= maxFlights) {
                RouteData currentRoute = new RouteData(allRoutes.get(i), prices.get(i));
                sortedRoutesByPrice.add(currentRoute);
            }
        }
        if (sortedRoutesByPrice.size() > 1) {
            Collections.sort(sortedRoutesByPrice, new Comparator<RouteData>() {
                @Override
                public int compare(RouteData r1, RouteData r2) {
                    return Integer.compare(r1.getPrice(), r2.getPrice()); // Compare by price
                }
            });
        }
        return sortedRoutesByPrice;
    }
}

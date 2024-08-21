package IntegerSetAPI;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class IntegerSetApiSimulation extends Simulation {

    // Defining the HTTP protocol
    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://127.0.0.1:8080")  // Base URL of your Flask API
            .acceptHeader("application/json")  // Common headers
            .contentTypeHeader("application/json");

    // Defining the feeder to provide dynamic data
    FeederBuilder.FileBased<Object> feeder = jsonFile("data/feederData.json").circular();

    ChainBuilder addItem = exec(
            http("Add Item Request")
                    .post("/add")
                    .body(StringBody(session -> {
                        Object item = session.get("item");
                        return "{\"item\": " + item + "}";
                    })).asJson()
                    .check(status().is(201))
                    .check(jsonPath("$.itemId").is(session -> session.getString("item")))  // Explicitly convert item to String
                    .check(responseTimeInMillis().saveAs("checkItemResponseTime"))
                    .check(bodyString().saveAs("addItemResponse"))

    ).exec(session -> {
        System.out.println("Add Item Response: " + session.getString("addItemResponse"));
        System.out.println("Check Item Response Time: " + session.getLong("checkItemResponseTime") + " ms");
        return session;
    });

    // Defining the chain of requests (ChainBuilder) for checking if the item exists
    ChainBuilder checkItemExists = exec(
            http("Check Item Exists")
                    .get("/has?itemId=#{item}")
                    .check(status().is(200))
                    .check(jsonPath("$.exists").is("true"))
                    .check(responseTimeInMillis().saveAs("checkItemResponseTime"))
                    .check(bodyString().saveAs("checkItemResponse"))
    ).exec(session -> {
        System.out.println("Check Item Response: " + session.getString("checkItemResponse"));
        System.out.println("Check Item Response Time: " + session.getLong("checkItemResponseTime") + " ms");
        return session;
    });

    // Defining the chain of requests (ChainBuilder) for removing an item
    ChainBuilder removeItem = exec(
            http("Remove Item Request")
                    .post("/remove")
                    .body(StringBody(session -> {
                        Object item = session.get("item");
                        return "{\"itemId\": " + item + "}";
                    })).asJson()
                    .check(status().is(200))
                    .check(jsonPath("$.message").is("Item removed successfully"))
                    .check(responseTimeInMillis().saveAs("checkItemResponseTime"))
                    .check(bodyString().saveAs("removeItemResponse"))
    ).exec(session -> {
        System.out.println("Remove Item Response: " + session.getString("removeItemResponse"));
        System.out.println("Check Item Response Time: " + session.getLong("checkItemResponseTime") + " ms");
        return session;
    });

    // Defining the scenario using the chain of requests
    ScenarioBuilder scn = scenario("Item Workflow Scenario")
            .feed(feeder)  // Load data from the feeder
            .exec(addItem)
            .pause(5)  // Increased pause to ensure the item is stored
            .exec(checkItemExists)
            .pause(1)
            .exec(removeItem);

    {
        // Setting up the simulation
        setUp(
                scn.injectOpen(
                        nothingFor(5),
                        rampUsers(8).during(30)
                )
        ).protocols(httpProtocol);
    }
}

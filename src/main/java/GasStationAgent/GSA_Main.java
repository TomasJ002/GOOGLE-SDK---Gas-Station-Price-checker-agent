package GasStationAgent;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.GoogleMapsTool;
import com.google.adk.tools.GoogleSearchTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;



public class GSA_Main {

    private static final String USER_ID = "student";
    private static final String APP_NAME = "gas_station_checker_app";

    public static void main(String[] args) throws Exception {
        Dotenv dotenv= Dotenv.configure()
                .directory("C:/Users/Tomáš/Desktop/Codes/Agents/personal/GasStationPriceChecker/")
                .load();

        LlmAgent gasStationPricesChecker = LlmAgent.builder()
                .model("gemini-1.5-flash")
                .name("gas station price checker")
                .description("Finds the best places to refuel on a certain trip")
                .instruction("""
                        You are an agent tasked to help the user find the best places to refuel while going on a specific route.
                        You will respond to the user in Slovak and always politely greet him and ask him, what route will he take, and if he needs petrol or diesel.
                        You will find the gas stations with the cheapest prices that follows these rules:
                        1. If a detour would be needed, it can´t take more than 10 minutes of driving.
                        2. Only check for big gas station companies, eg. OMV, Shell, Orlen and Slonaft (in Slovakia) or Mol (In Czech Republic), discard local and less known providers.
                        3. If the data you obtain is older than three days, notify the user of this and propose an alternative gas station with newer data.
                        4. Always add the cheapest Shell gas station, if it passes previous rules, as an alternative to the cheapest gas station.
                        5. Always display the prices in Euro, for other currencies, take the latest Slovak national bank exchange rate.
                        """)
                .tools(new GoogleMapsTool())
                .tools(new GoogleSearchTool())
                .build();

        InMemoryRunner runner = new InMemoryRunner(gasStationPricesChecker);

        Session session = runner.sessionService()
                .createSession(APP_NAME, USER_ID)
                .blockingGet();

        System.out.println("Agent ready");
        System.out.println("Tell me what route are you taking and which fuel do you need");

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();

                if ("quit".equalsIgnoreCase(userInput)) {
                    break;
                }

                Content userMsg = Content.fromParts(Part.fromText(userInput));
                Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);

                System.out.print("\nAgent > ");
                events.blockingForEach(event -> {
                    event.content().flatMap(Content::parts).ifPresent(parts -> {
                        for (Part part : parts) {
                            part.text().ifPresent(System.out::print);
                        }
                    });
                });
            }
        }
    }
}
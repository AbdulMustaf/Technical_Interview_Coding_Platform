package server;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class CodeExecution {
    private static final String API_KEY = "YOUR_JDoodle_API_KEY";
    private static final String CLIENT_SECRET = "YOUR_JDoodle_CLIENT_SECRET";


    public static String executeCode(String code, String language) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.jdoodle.com/v1/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            String.format("{\"clientId\": \"%s\", \"clientSecret\": \"%s\", \"script\": \"%s\", \"language\": \"%s\"}",
                                    API_KEY, CLIENT_SECRET, code, language)
                    ))
                    .build();


            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing code: " + e.getMessage();
        }
    }
}

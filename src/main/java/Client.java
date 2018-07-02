import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

public class Client {
    public static void main(String... args) throws Exception {
        HttpResponse res = Unirest.get("http://localhost:8080/?foo=foo%20bar").asString();

        System.out.println("status: " + res.getStatusText());
        System.out.println("body: " + res.getBody());
    }
}

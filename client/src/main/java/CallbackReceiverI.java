import Demo.Response;
import com.zeroc.Ice.Current;

public class CallbackReceiverI implements Demo.CallbackReceiver {

    @Override
    public void receiveMessage(String response, Current current) {
        System.out.println(response);
    }

    @Override
    public void updateStats(Response response, Current current) {
        Client.setLastValue(response.value);
        Client.setLastResponseTime(response.responseTime);
        Client.setLastThroughput(response.throughput);
        Client.setLastUnprocessRate(response.unprocessRate);
    }

}

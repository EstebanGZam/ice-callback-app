import com.zeroc.Ice.Current;

public class CallbackReceiverI implements Demo.CallbackReceiver {

    @Override
    public void receiveMessage(String response, Current current) {
        System.out.println(response);
    }
}

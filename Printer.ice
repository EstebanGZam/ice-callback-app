module Demo
{
    class Response
    {
        long responseTime;
        double throughput;
        double unprocessedRate;
        string value;
    };
    exception InvalidOperationError {
        string reason;
    };
    interface CallbackReceiver
    {
        void receiveMessage(string response);
    };
    interface CallbackSender
    {
        Response sendMessage(string message, CallbackReceiver* proxy);
        void registerClient(string hostname, CallbackReceiver* proxy);
        void removeClient(string name);
    };
};
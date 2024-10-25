module Demo
{
    class Response
    {   
        long startTime;
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
        Response sendMessage(string message, long startTime, CallbackReceiver* proxy);
        void registerClient(string hostname, CallbackReceiver* proxy);
        void removeClient(string name);
    };
};
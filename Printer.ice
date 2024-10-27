module Demo
{
    class Response
    {   
        string messageIdentifier;
        long requestedTime;
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
        Response sendMessage(string messageIdentifier, string message, long requestedTime, CallbackReceiver* proxy);
        void registerClient(string hostname, CallbackReceiver* proxy);
        void removeClient(string name);
    };
};
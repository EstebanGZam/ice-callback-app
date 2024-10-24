module Demo
{
    class Response
    {
        long responseTime;
        double throughput;
        double unprocessRate;
        string value;
    };
    interface CallbackReceiver
    {
        void receiveMessage(string response);
        void updateStats(Response response);
    };
    interface CallbackSender
    {
        void sendMessage(string message, CallbackReceiver* proxy);
        void registerClient(string hostname, CallbackReceiver* proxy);
        void removeClient(string name);
    };
};
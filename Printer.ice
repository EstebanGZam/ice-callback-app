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
        void sendMessage(string s, CallbackReceiver* proxy);
    };
};
module Demo
{
    class Response{
        long responseTime;
        double throughput;
        double unprocessRate;
        string value;
    };
    interface Printer
    {
        Response printString(string s);
    };
};
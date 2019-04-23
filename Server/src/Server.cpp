#include <Ice/Ice.h>
#include "../include/StreamingI.hpp"
#include "../include/MorceauI.hpp"

using namespace std;
using namespace Middleware;

int main(int argc, char* argv[])
{
	string ip = "192.168.0.5";
	string port = "12345";

	if (argc >= 2)
		ip = argv[1];
	if (argc >= 3)
		port = argv[2];

	try
	{
		Ice::CommunicatorHolder ich(argc, argv);
		auto adapter = ich -> createObjectAdapterWithEndpoints("ServeurStreamingAdapter", "default -h " + ip + " -p " + port);
		auto servant = make_shared<StreamingI>(ip, port);
		adapter -> add(servant, Ice::stringToIdentity("ServeurStreaming"));
		adapter -> activate();
		ich -> waitForShutdown();
	}
	catch (const std::exception& e)
	{
		cerr << e.what() << endl;

		return 1;
	}

	return 0;
}
// Examples of C++ ownership shapes for parser experiments.

int fileLevelCounter = 0;

int freeFunction(int value) {
	return value + 1;
}

static void fileLocalHelper() {
	fileLevelCounter++;
}

namespace tools {
	int namespaceCounter = 10;

	int namespaceFunction(int value) {
		return value + namespaceCounter;
	}

	class Logger {
	public:
		void write(const char* message) {
			lastMessage = message;
		}

		const char* read() const {
			return lastMessage;
		}

	private:
		const char* lastMessage = "";
	};
}

namespace company {
	namespace platform {
		namespace network {
			int nestedNamespaceValue = 42;

			int nestedNamespaceFunction() {
				return nestedNamespaceValue;
			}

			class Client {
			public:
				Client();

				void connect() {
					connected = true;
				}

				class Request {
				public:
					void build();

					bool isReady() const {
						return ready;
					}

				private:
					bool ready = false;
				};

			private:
				bool connected = false;
			};
		}
	}
}

namespace modern::nested::syntax {
	int compactNamespaceField = 7;

	int compactNamespaceFunction() {
		return compactNamespaceField;
	}

	class Service {
	public:
		void start();
	};
}

company::platform::network::Client::Client() {
	connect();
}

void company::platform::network::Client::Request::build() {
	ready = true;
}

void modern::nested::syntax::Service::start() {
	compactNamespaceField++;
}

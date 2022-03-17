in order validator-engine to work on Windows it has to be patched in:

collator.cpp line 3124:
    int i = 0;
    cfg0 = cfg_dict.lookup_ref(td::BitArray<32>(i));
validate-query.cpp line 4964:
  int i = 0;
  auto param0 = dict1.lookup_ref(td::BitArray<32>{i});


in order to do graceful shutdown on windows add this into validator-engine.cpp:

ON WINDOWS
#include <chrono>
#include <csignal>

add:
bool shutdownFlag;
also in validator-engine.hpp:
extern bool shutdownFlag;

line around 3420
  signal(SIGINT, sigint_handler);

where
void sigint_handler(int sig) {
  std::cout << "Exit call sig " << sig << "\n";
  shutdownFlag = true;
  std::chrono::seconds dura(3);
  std::this_thread::sleep_for(dura);
  std::cout << "Exit 3 sec\n";
  exit(0);
}

ON LINUX:

#include <csignal>

line around 3415
      signal(SIGINT, sigint_handler);

void sigint_handler(int sig) {
  std::cout << "Exit call sig " << sig << "\n";
  shutdownFlag = true;
  usleep(900000);
  usleep(900000);
  usleep(900000);
  std::cout << "Exit 3 sec\n";
  exit(0);
}

then use shutdownFlag in collator.cpp

#include "validator-engine/validator-engine.hpp"

bool Collator::create_mc_state_extra() {
  LOG(INFO) << "shutdown flag " << shutdownFlag;
  if (!is_masterchain()) {
    CHECK(mc_state_extra_.is_null());
    return true;
  }

  if (shutdownFlag) {
    LOG(INFO) << "cancelling creation of mc state due to positive shutdown flag";
    return false;
  }

in order to avoid node not synced on Windows patch the following in manager.cpp:
-  if (!started_) {
+  /*if (!started_) {
     promise.set_error(td::Status::Error(ErrorCode::notready, "node not synced"));
     return;
-  }
+  }*/


on windows
netsh int ipv4 set dynamicport tcp start=1025 num=64511
netsh int ipv4 set dynamicport udp start=1025 num=64511
netsh int ipv4 show dynamicport udp
netsh int ipv4 show dynamicport tcp



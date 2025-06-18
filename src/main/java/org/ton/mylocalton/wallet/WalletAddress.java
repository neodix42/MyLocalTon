package org.ton.mylocalton.wallet;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.*;
import org.ton.ton4j.tlb.Message;

@Builder
@Data
public class WalletAddress implements Serializable {
  String bounceableAddressBase64url;
  String nonBounceableAddressBase64Url;
  String bounceableAddressBase64;
  String nonBounceableAddressBase64;
  String fullWalletAddress;
  long wc;
  long subWalletId;
  String hexWalletAddress;
  String publicKeyHex;
  String publicKeyBase64;
  String privateKeyHex;
  String mnemonic;
  transient Message message;
  String privateKeyLocation;
  String filenameBase;
  String filenameBaseLocation;
  transient ByteBuffer walletQueryFileBoc; // contains smc code
  String walletQueryFileBocLocation;
}

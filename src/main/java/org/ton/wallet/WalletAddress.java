package org.ton.wallet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.smartcontract.types.InitExternalMessage;

import java.io.Serializable;
import java.nio.ByteBuffer;

@Builder
@Getter
@Setter
@ToString
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
    transient InitExternalMessage initExternalMessage;
    String privateKeyLocation;
    String filenameBase;
    String filenameBaseLocation;
    transient ByteBuffer walletQueryFileBoc; // contains smc code
    String walletQueryFileBocLocation;
}

package org.ton.wallet;

import lombok.*;

import java.nio.ByteBuffer;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletAddress {
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
    String privateKeyLocation;
    String filenameBase;
    String filenameBaseLocation;
    transient ByteBuffer walletQueryFileBoc; // contains smc code
    String walletQueryFileBocLocation;
}

package org.ton.mylocalton.utils;

public enum WalletVersion {
    V1R1("V1R1"),
    V1R2("V1R2"),
    V1R3("V1R3"),
    V2R1("V2R1"),
    V2R2("V2R2"),
    V3R1("V3R1"),
    V3R2("V3R2"),
    V5R1("V5R1"),
    V4R2("V4R2 plugins"),
    lockup("Restricted"),
    dnsCollection("DNS collection"),
    dnsItem("DNS item"),
    jettonMinter("Jetton minter"),
    jettonWallet("Jetton wallet"),
    nftCollection("NFT Collection"),
    nftItem("NFT Item"),
    nftSale("NFT Sale"),
    payments("Payments"),
    highload("Highload"),
    highloadV3("Highload V3"),
    multisig("Multisig"),
    /**
     * reserved for internal usage
     */
    master("Master"),
    /**
     * reserved for internal usage
     */
    config("Config"),
    /**
     * reserved for internal usage
     */
    unidentified("Unidentified");

    private final String value;

    WalletVersion(final String value) {
        this.value = value;
    }

    public static WalletVersion getKeyByValue(String value) {
        for (WalletVersion v : WalletVersion.values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }
}
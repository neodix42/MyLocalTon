package org.ton.wallet;

import org.ton.actions.MyLocalTon;

import java.io.Serializable;
import java.util.Map;

public enum WalletVersion implements Serializable {

    V1("Simple Wallet V1"),
    V2("Simple Wallet V2"),
    V3("Simple Wallet V3"),
    MASTER("Wallet Master"),
    CONFIG("Config Smc"),
    HIGHLOAD_WALLET_V2("Highload Wallet V2"),
    RESTRICTED_WALLET_V3("Restricted Wallet V3");

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

    public static String getKeyByValueInMap(String value) {
        for (Map.Entry<String, String> entry : MyLocalTon.walletVersions.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
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

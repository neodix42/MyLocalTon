package org.ton.mylocalton.executors.liteclient.constants;

import java.io.Serializable;

public final class TonConstants implements Serializable {

    public static final long MASTER_WORCHAIN = -1;
    public static final String MASTER_SHARD = "8000000000000000";

    private TonConstants() {
        //this prevents even the native class from calling this actor as well :
        throw new AssertionError();
    }

    public enum InMsgType {

        EXTERNAL((byte) 0),
        IHR((byte) 1),
        IMMEDIATELLY((byte) 2),
        FINAL((byte) 3),
        TRANSIT((byte) 4),
        DISCARDED_FINAL((byte) 5),
        DISCARDED_TRANSIT((byte) 6);

        private byte msgType;

        InMsgType(byte i) {
            this.msgType = i;
        }

        public byte getMsgType() {
            return msgType;
        }
    }

    public enum TonTransactionType {

        ORDINARY((byte) 0),
        STORAGE((byte) 1),
        TICK((byte) 2),
        TOCK((byte) 3),
        SPLIT_PREPARE((byte) 4),
        SPLIT_INSTALL((byte) 5),
        MERGE_PREPARE((byte) 6),
        MERGE_INSTALL((byte) 7);

        private byte txType;

        TonTransactionType(byte i) {
            this.txType = i;
        }

        public byte getTxType() {
            return txType;
        }
    }

    public enum TonBlockProcessingStatus {

        UNKNOWN((byte) 0),
        PROPOSED((byte) 1),
        FINALIZED((byte) 2),
        REFUSED((byte) 3);

        private byte blockProcessingStatus;

        TonBlockProcessingStatus(byte i) {
            this.blockProcessingStatus = i;
        }

        public byte getBlockProcessingStatus() {
            return blockProcessingStatus;
        }
    }

    public enum TonTransactionStatus {

        UNKNOWN((byte) 0),
        PRELIMINARY((byte) 1),
        PROPOSED((byte) 2),
        FINALIZED((byte) 3),
        REFUSED((byte) 4);

        private byte txStatus;

        TonTransactionStatus(byte i) {
            this.txStatus = i;
        }

        public byte getTxStatus() {
            return txStatus;
        }
    }

    public enum TonMessageStatus {

        UNKNOWN((byte) 0),
        QUEUED((byte) 1),
        PROCESSING((byte) 2),
        PRELIMINARY((byte) 3),
        PROPOSED((byte) 4),
        FINALIZED((byte) 5),
        REFUSED((byte) 6),
        TRANSITING((byte) 7);

        private byte msgStatus;

        TonMessageStatus(byte i) {
            this.msgStatus = i;
        }

        public byte getMessageStatus() {
            return msgStatus;
        }
    }

    public enum TonOrigStatus {

        UNINIT((byte) 0),
        ACTIVE((byte) 1),
        FROZEN((byte) 2),
        NON_EXIST((byte) 3);

        private byte origStatus;

        TonOrigStatus(byte i) {
            this.origStatus = i;
        }

        public byte getOrigStatus() {
            return origStatus;
        }
    }

    public enum TonAccountStatusChange {

        UNCHANGED((byte) 0),
        FROZEN((byte) 1),
        DELETED((byte) 2);

        private byte accStatusChange;

        TonAccountStatusChange(byte i) {
            this.accStatusChange = i;
        }

        public byte getAccStatusChange() {
            return accStatusChange;
        }
    }

    public enum TonOutMsgType {

        EXTERNAL((byte) 0),
        IMMEDIATELY((byte) 1),
        OUT_MSG_NEW((byte) 2),
        TRANSIT((byte) 3),
        DEQUEUE_IMMEDIATELLY((byte) 4),
        DEQUEUE((byte) 5),
        TRANSIT_REQUIRED((byte) 6),
        NONE((byte) -1);

        private byte outMsgStatus;

        TonOutMsgType(byte i) {
            this.outMsgStatus = i;
        }

        public byte getOutMsgStatus() {
            return outMsgStatus;
        }
    }
}

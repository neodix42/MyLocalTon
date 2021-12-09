package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultListParticipants implements Serializable {
    private String pubkey;
    private String weight;
}

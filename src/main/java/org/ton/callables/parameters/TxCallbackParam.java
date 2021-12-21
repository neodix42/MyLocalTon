package org.ton.callables.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.db.DB2;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;

import java.util.List;

@Builder
@Getter
@Setter
public class TxCallbackParam {
    DB2 db;
    TxPk txPk;
    TxEntity foundTx;
    List<TxEntity> foundTxs;
    Long datetimeFrom;
    String searchText;
}

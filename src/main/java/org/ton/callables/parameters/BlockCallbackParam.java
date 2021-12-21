package org.ton.callables.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.db.DB2;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.BlockPk;

import java.util.List;

@Builder
@Getter
@Setter
public class BlockCallbackParam {
    DB2 db;
    BlockPk blockPk;
    BlockEntity foundBlock;
    List<BlockEntity> foundBlocks;
    Long datetimeFrom;
    String searchText;
}

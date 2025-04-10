package org.ton.mylocalton.ui.custom.events.event;

import org.ton.mylocalton.ui.custom.events.CustomEvent;
import org.ton.mylocalton.ui.custom.media.NoBlocksTransactionsController;

public class CustomRefreshEvent extends CustomEvent {

  private NoBlocksTransactionsController.ViewType viewType;

  public CustomRefreshEvent(Type eventType, NoBlocksTransactionsController.ViewType viewType) {
    super(eventType);
    this.viewType = viewType;
  }

  public NoBlocksTransactionsController.ViewType getViewType() {
    return viewType;
  }
}

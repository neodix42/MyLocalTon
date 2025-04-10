package org.ton.actions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;

class TreeNode {
  String name;
  List<TreeNode> children = new ArrayList<>();
  double subtreeWidth;
  double subtreeHeight;
  double centerX;
  double centerY;
  BigDecimal amount;
  String opCode;
  Message message;
  Transaction transaction;

  Circle circle;
  Text text;

  TreeNode(String name) {
    this.name = name;
  }

  TreeNode addChild(TreeNode child) {
    children.add(child);
    return this;
  }
}

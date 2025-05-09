package org.ton.mylocalton.actions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

class TreeNode {
  String name;
  List<TreeNode> children = new ArrayList<>();
  double subtreeWidth;
  double subtreeHeight;
  double centerX;
  double centerY;
  BigInteger amount;
  String opCode;
  org.ton.java.tlb.Message message;
  org.ton.java.tlb.Transaction transaction;

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

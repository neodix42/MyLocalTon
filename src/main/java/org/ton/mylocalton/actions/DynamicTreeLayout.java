package org.ton.mylocalton.actions;

import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.db.entities.TxEntity;
import org.ton.mylocalton.executors.liteclient.api.block.Message;
import org.ton.mylocalton.executors.liteclient.api.block.Transaction;
import org.ton.mylocalton.ui.controllers.TxController;

@Slf4j
public class DynamicTreeLayout {

  private static final char[] ALPHABET =
      new char[] {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
        'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
      };
  private static final Map<String, Character> ALPHABET_MAP = new HashMap<>();
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private final double NODE_RADIUS = 20;
  private final double X_GAP = 100;
  private final double Y_GAP = 25;
  public Pane pane;
  private Label infoLabel;
  private Stage detailsStage;
  private VBox detailsBox;
  private TreeNode selectedNode;

  public void showTree(TxEntity txEntity, Transaction tx) {
    TreeNode root = buildTree(txEntity);

    pane = new Pane();

    infoLabel = new Label("Click node to get its name");
    infoLabel.setFont(Font.font(14));
    infoLabel.setLayoutX(20);
    infoLabel.setLayoutY(20);
    pane.getChildren().add(infoLabel);

    measureSubtree(root);

    layoutSubtree(root, 50, 120, pane, txEntity);

    var scrollPane = new ScrollPane(pane);
    generateStage(scrollPane, txEntity);

    createDetailsStage(txEntity);
    selectedNode = root;
    applySelectedStyle(root);
    //    showNodeDetails(root);
  }

  private void createDetailsStage(TxEntity txEntity) {
    detailsStage = new Stage();
    detailsStage.setTitle("Event overview");

    detailsBox = new VBox(10);
    detailsBox.setFillWidth(true);

    Scene scene = new Scene(detailsBox, 600, 800);

    detailsStage.setScene(scene);

    var screenBounds = Screen.getPrimary().getVisualBounds();
    var offsetX = 400.0;
    var stageWidth = detailsBox.getPrefWidth();
    var stageHeight = detailsBox.getPrefHeight();

    var x = (screenBounds.getWidth() - stageWidth) / 2 + offsetX;
    var y = (screenBounds.getHeight() - stageHeight) / 2;

    detailsStage.setX(x);
    detailsStage.setY(y);

    detailsStage.setOnCloseRequest(e -> detailsStage = null);
  }

  private void showNodeDetails(TreeNode node) {
    VBox contentBox = new VBox();
    contentBox.setPadding(new Insets(5, 5, 5, 5));

    Label nameLabel = new Label("Name: \n" + node.name);
    Label amountLabel =
        new Label("Amount: \n" + (nonNull(node.amount) ? Utils.formatCoins(node.amount) : "N/A"));
    Label opCodeLabel = new Label("opCode: \n" + (node.opCode != null ? node.opCode : "N/A"));
    Label detailsLabel =
        new Label(
            "Details: \n\n\n"
                + (nonNull(node.transaction) ? node.transaction.toJson() : "N/A")
                + "\n\n\n"
                + (nonNull(node.message) ? node.message.toJson() : "N/A"));
    detailsLabel.setWrapText(true);

    contentBox.getChildren().addAll(nameLabel, amountLabel, opCodeLabel, detailsLabel);

    ScrollPane scrollPane = new ScrollPane(contentBox);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    // Enable scrollPane to grow inside VBox
    VBox.setVgrow(scrollPane, Priority.ALWAYS);
    detailsBox.getChildren().clear();
    detailsBox.getChildren().add(scrollPane);
  }

  private void generateStage(ScrollPane pane, TxEntity txEntity) {
    var stage = new Stage();
    stage.setTitle("Tx hash " + txEntity.getTxHash());
    stage.initStyle(StageStyle.DECORATED);

    try {
      var icon =
          new Image(
              Objects.requireNonNull(
                  DynamicTreeLayout.class
                      .getClassLoader()
                      .getResourceAsStream("org/ton/mylocalton/images/logo.png")));
      stage.getIcons().add(icon);
    } catch (NullPointerException e) {
      log.error("Icon not found: {}", e.getMessage(), e);
    }

    var stageWidth = 800;
    var stageHeight = 600;

    var scene = new Scene(pane, stageWidth, stageHeight);
    scene
        .getStylesheets()
        .add(
            TxController.class
                .getClassLoader()
                .getResource("org/ton/mylocalton/css/global-font.css")
                .toExternalForm());

    scene.setOnKeyPressed(
        keyEvent -> {
          if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            stage.close();
          }
        });

    stage.setScene(scene);

    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

    stage.setX((screenBounds.getWidth() - stageWidth) / 2);
    stage.setY((screenBounds.getHeight() - stageHeight) / 2);

    stage.show();
  }

  private void measureSubtree(TreeNode node) {
    if (node.children.isEmpty()) {
      node.subtreeWidth = 2 * NODE_RADIUS;
      node.subtreeHeight = 2 * NODE_RADIUS;
      return;
    }

    double totalChildrenHeight = 0;
    double maxChildWidth = 0;

    for (var child : node.children) {
      measureSubtree(child);
    }

    for (int i = 0; i < node.children.size(); i++) {
      var child = node.children.get(i);
      totalChildrenHeight += child.subtreeHeight;
      if (i < node.children.size() - 1) {
        totalChildrenHeight += Y_GAP;
      }
      if (child.subtreeWidth > maxChildWidth) {
        maxChildWidth = child.subtreeWidth;
      }
    }

    node.subtreeWidth = (2 * NODE_RADIUS) + X_GAP + maxChildWidth;
    node.subtreeHeight = Math.max(2 * NODE_RADIUS, totalChildrenHeight);
  }

  private void layoutSubtree(TreeNode node, double x, double y, Pane pane, TxEntity txEntity) {
    node.centerX = x + NODE_RADIUS;
    node.centerY = y + node.subtreeHeight / 2.0;

    var parentCircle = new Circle(node.centerX, node.centerY, NODE_RADIUS);
    parentCircle.setFill(Color.WHITE);
    parentCircle.setStroke(Color.BLACK);
    parentCircle.setStrokeWidth(2.0);

    var nodeText = new Text(node.centerX - (node.name.length() * 3), node.centerY + 3, node.name);
    nodeText.setFill(Color.BLACK);
    nodeText.setTextAlignment(TextAlignment.CENTER);
    nodeText.setStyle("-fx-font-weight: bold;");
    nodeText.setMouseTransparent(true);

    node.circle = parentCircle;
    node.text = nodeText;
    //        node.tx = txEntity.getTx(); // todo.. uncomment

    if (node == selectedNode) {
      applySelectedStyle(node);
    }

    parentCircle.setOnMouseClicked(
        e -> {
          if (detailsStage == null) {
            createDetailsStage(txEntity);
            detailsStage.show();
          }
          infoLabel.setText("Clicked node: " + node.name);

          if (selectedNode != null && selectedNode != node) {
            applyDefaultStyle(selectedNode);
          }

          selectedNode = node;
          applySelectedStyle(node);
          var screenBounds = Screen.getPrimary().getVisualBounds();
          var offsetX = 400.0;
          var stageWidth = detailsBox.getPrefWidth();
          var stageHeight = detailsBox.getPrefHeight();
          var x1 = (screenBounds.getWidth()) / 2 + offsetX;
          var y1 = (screenBounds.getHeight()) / 4;
          detailsStage.setX(x1);
          detailsStage.setY(y1);
          detailsStage.show();
          showNodeDetails(node);
        });

    pane.getChildren().addAll(parentCircle, nodeText);

    if (node.children.isEmpty()) {
      return;
    }

    var junctionX = node.centerX + (NODE_RADIUS * 2.5) + 50;
    var horizontalLineFromParent =
        new Line(node.centerX + NODE_RADIUS, node.centerY, junctionX, node.centerY);
    pane.getChildren().add(horizontalLineFromParent);

    var currentY = y;
    for (var child : node.children) {
      layoutSubtree(child, junctionX + X_GAP, currentY, pane, txEntity);
      currentY += child.subtreeHeight + Y_GAP;
    }

    var minChildY = Double.POSITIVE_INFINITY;
    var maxChildY = Double.NEGATIVE_INFINITY;
    for (var child : node.children) {
      if (child.centerY < minChildY) {
        minChildY = child.centerY;
      }
      if (child.centerY > maxChildY) {
        maxChildY = child.centerY;
      }
    }

    var verticalBus = new Line(junctionX, minChildY, junctionX, maxChildY);
    pane.getChildren().add(verticalBus);

    for (var child : node.children) {
      var childLeftX = child.centerX - NODE_RADIUS;
      var busToChild = new Line(junctionX, child.centerY, childLeftX, child.centerY);
      pane.getChildren().add(busToChild);

      var midX = (junctionX + childLeftX) / 2;
      var midY = child.centerY;

      var json = gson.toJson(txEntity.getTx());
      //            var json = gson.toJson(child.tx); // todo revert the changes
      var hooverHelper = new TraceHooverHelper();
      hooverHelper.assignTwoLabels(
          pane,
          midX - 40,
          midY,
          Utils.formatNanoValue(child.amount.toBigInteger(), 2) + " TON",
          child.opCode,
          json);
    }
  }

  private void applySelectedStyle(TreeNode node) {
    node.circle.setStroke(null);
    node.circle.setFill(Color.SKYBLUE);
    node.text.setFill(Color.WHITE);
    node.text.setTextAlignment(TextAlignment.CENTER);
  }

  private void applyDefaultStyle(TreeNode node) {
    node.circle.setStroke(Color.BLACK);
    node.circle.setFill(Color.WHITE);
    node.circle.setStrokeWidth(2.0);
    node.text.setFill(Color.BLACK);
    node.text.setTextAlignment(TextAlignment.CENTER);
  }

  private TreeNode buildTree(TxEntity txEntity) {
    var index = 0;

    //    char nodeChar;
    //    if (ALPHABET_MAP.containsKey(txEntity.getAccountAddress())) {
    //      nodeChar = ALPHABET_MAP.get(txEntity.getTxHash());
    //    } else {
    //      nodeChar = ALPHABET[index++];
    //      ALPHABET_MAP.put(txEntity.getAccountAddress(), nodeChar);
    //    }

    var root = new TreeNode(ALPHABET[index++] + "");
    for (Message msg : txEntity.getTx().getOutMsgs()) {
      TreeNode child = new TreeNode(ALPHABET[index++] + "");
      child.amount = msg.getValue().getToncoins();
      child.opCode = "transfer";
      child.text = new Text("text");
      child.transaction = txEntity.getTx();
      child.message = msg;
      root.addChild(child);
    }

    return root;
  }
}

package org.ton.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
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
import org.ton.db.entities.TxEntity;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.ui.controllers.TxController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class DynamicTreeLayout {

    private final double NODE_RADIUS = 20;
    private final double X_GAP = 100;
    private final double Y_GAP = 25;

    private Label infoLabel;
    private Stage detailsStage;
    private VBox detailsBox;

    private TreeNode selectedNode;

    private static final char[] ALPHABET = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
            'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private static final Map<String, Character> ALPHABET_MAP = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void showTree(TxEntity txEntity, Transaction tx) {
//        TreeNode root = buildTree(txEntity);
        var root = buildSampleTree();

        var pane = new Pane();

        infoLabel = new Label("Нажмите на узел, чтобы узнать его название.");
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
        showNodeDetails(root);
    }

    private void createDetailsStage(TxEntity txEntity) {
        detailsStage = new Stage();
        detailsStage.setTitle("Event overview");

        detailsBox = new VBox(10);
        detailsBox.setPrefSize(600, 800);

        var scene = new Scene(detailsBox);
        detailsStage.setScene(scene);

        var screenBounds = Screen.getPrimary().getVisualBounds();
        var offsetX = 400.0;
        var stageWidth = detailsBox.getPrefWidth();
        var stageHeight = detailsBox.getPrefHeight();

        var x = (screenBounds.getWidth() - stageWidth) / 2 + offsetX;
        var y = (screenBounds.getHeight() - stageHeight) / 2;

        detailsStage.setX(x);
        detailsStage.setY(y);

        detailsStage.show();

        detailsStage.setOnCloseRequest(e -> detailsStage = null);
    }

    private void showNodeDetails(TreeNode node) {
        var contentBox = new VBox();
        contentBox.setPadding(new Insets(5, 5, 5, 5));

        var nameLabel = new Label("Name: \n" + node.name);
        var amountLabel = new Label("Amount: \n" + (node.amount != null ? node.amount : "N/A"));
        var opCodeLabel = new Label("opCode: \n" + (node.opCode != null ? node.opCode : "N/A"));
        var detailsLabel = new Label("Details: \n" + obj + "\n" + obj + "\n" + obj);
        detailsLabel.setWrapText(true);

        contentBox.getChildren().addAll(nameLabel, amountLabel, opCodeLabel, detailsLabel);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        detailsBox.getChildren().clear();
        detailsBox.getChildren().add(scrollPane);
    }


    private void generateStage(ScrollPane pane, TxEntity txEntity) {
        var stage = new Stage();
        stage.setTitle("Tx hash " + txEntity.getTxHash());
        stage.initStyle(StageStyle.DECORATED);

        try {
            var icon = new Image(Objects.requireNonNull(
                    DynamicTreeLayout.class.getClassLoader().getResourceAsStream("org/ton/images/logo.png")
            ));
            stage.getIcons().add(icon);
        } catch (NullPointerException e) {
            log.error("Icon not found: {}", e.getMessage(), e);
        }

        var stageWidth = 800;
        var stageHeight = 600;

        var scene = new Scene(pane, stageWidth, stageHeight);
        scene.getStylesheets().add(
                TxController.class
                        .getClassLoader()
                        .getResource("org/ton/css/global-font.css")
                        .toExternalForm()
        );

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                stage.close();
            }
        });

        stage.setScene(scene);

        var screenBounds = Screen.getPrimary().getVisualBounds();
        var offsetX = 400.0;

        var x = (screenBounds.getWidth() - stageWidth) / 2 - offsetX;
        var y = (screenBounds.getHeight() - stageHeight) / 2;

        stage.setX(x);
        stage.setY(y);

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

        var nodeText = new Text(
                node.centerX - (node.name.length() * 3),
                node.centerY + 3,
                node.name
        );
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

        parentCircle.setOnMouseClicked(e -> {
            if (detailsStage == null) {
                createDetailsStage(txEntity);
            }
            infoLabel.setText("Нажат узел: " + node.name);

            if (selectedNode != null && selectedNode != node) {
                applyDefaultStyle(selectedNode);
            }

            selectedNode = node;
            applySelectedStyle(node);
            showNodeDetails(node);
        });

        pane.getChildren().addAll(parentCircle, nodeText);

        if (node.children.isEmpty()) {
            return;
        }

        var junctionX = node.centerX + (NODE_RADIUS * 2.5) + 50;
        var horizontalLineFromParent = new Line(
                node.centerX + NODE_RADIUS,
                node.centerY,
                junctionX,
                node.centerY
        );
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
            hooverHelper.assignTwoLabels(pane, midX - 40, midY, child.amount + " TON", child.opCode, json);
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

        // todo.. complete the logic
//        char nodeChar;
//        if (ALPHABET_MAP.containsKey(txEntity.getAccountAddress())) {
//            nodeChar = ALPHABET_MAP.get(txEntity.getTxHash());
//        }
//        else {
//            nodeChar = ALPHABET[index++];
//            ALPHABET_MAP.put(txEntity.getAccountAddress(), nodeChar);
//        }

        var root = new TreeNode(ALPHABET[index++] + "");
        var b = new TreeNode(ALPHABET[index++] + "");
        b.amount = BigDecimal.valueOf(0.44);
        b.opCode = "transfer";
        var c = new TreeNode(ALPHABET[index++] + "");
        c.amount = BigDecimal.valueOf(25.21);
        c.opCode = "Jetton Transfer";
        var d = new TreeNode(ALPHABET[index++] + "");
        d.amount = BigDecimal.valueOf(0.01);
        d.opCode = "Jetton Internal Transfer";
        root.addChild(b).addChild(c).addChild(d);

        return root;
    }

    private TreeNode buildSampleTree() {
        TreeNode root = new TreeNode("0");

        // Корень -> A, B, C, D, E, F
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");
        TreeNode e = new TreeNode("E");
        TreeNode f = new TreeNode("F");
        root.addChild(a).addChild(b).addChild(c).addChild(d).addChild(e).addChild(f);

        // A -> A1, A2, A3
        TreeNode a1 = new TreeNode("A1");
        TreeNode a2 = new TreeNode("A2");
        TreeNode a3 = new TreeNode("A3");
        a.addChild(a1).addChild(a2).addChild(a3);

        // A1 -> A4, A5
        a1.addChild(new TreeNode("A4"))
                .addChild(new TreeNode("A5"));

        // A2 -> A6, A7
        a2.addChild(new TreeNode("A6"))
                .addChild(new TreeNode("A7"));

        // A3 -> A8, A9
        a3.addChild(new TreeNode("A8"))
                .addChild(new TreeNode("A9"));

        // B -> B1, B2, B3
        TreeNode b1 = new TreeNode("B1");
        TreeNode b2 = new TreeNode("B2");
        TreeNode b3 = new TreeNode("B3");
        b.addChild(b1).addChild(b2).addChild(b3);

        // B1 -> B4, B5
        b1.addChild(new TreeNode("B4"))
                .addChild(new TreeNode("B5"));

        // B2 -> B5
        b2.addChild(new TreeNode("B5"));

        // B3 -> B6, B7
        b3.addChild(new TreeNode("B6"))
                .addChild(new TreeNode("B7"));

        // C -> C1, C2
        TreeNode c1 = new TreeNode("C1");
        TreeNode c2 = new TreeNode("C2");
        c.addChild(c1).addChild(c2);

        // C1 -> C3
        c1.addChild(new TreeNode("C3"));

        // C2 -> C4, C5, C6, C7
        c2.addChild(new TreeNode("C4"))
                .addChild(new TreeNode("C5"))
                .addChild(new TreeNode("C6"))
                .addChild(new TreeNode("C7"));

        // D -> D1, D2, D3, D4
        TreeNode d1 = new TreeNode("D1");
        TreeNode d2 = new TreeNode("D2");
        TreeNode d3 = new TreeNode("D3");
        TreeNode d4 = new TreeNode("D4");
        d.addChild(d1).addChild(d2).addChild(d3).addChild(d4);

        // E -> E1, E2, E3
        TreeNode e1 = new TreeNode("E1");
        TreeNode e2 = new TreeNode("E2");
        TreeNode e3 = new TreeNode("E3");
        e.addChild(e1).addChild(e2).addChild(e3);

        // E2 -> E4, E5
        TreeNode e2a = new TreeNode("E4");
        TreeNode e2b = new TreeNode("E5");
        e2.addChild(e2a).addChild(e2b);

        // E2a -> E6, E7
        e2a.addChild(new TreeNode("E6"))
                .addChild(new TreeNode("E7"));

        // E3 -> E8
        e3.addChild(new TreeNode("E8"));

        // F -> F1, F2, F3
        TreeNode f1 = new TreeNode("F1");
        TreeNode f2 = new TreeNode("F2");
        TreeNode f3 = new TreeNode("F3");
        f.addChild(f1).addChild(f2).addChild(f3);

        // F1 -> F4, F5
        TreeNode f1a = new TreeNode("F4");
        TreeNode f1b = new TreeNode("F5");
        f1.addChild(f1a).addChild(f1b);

        // F1a -> F6, F7, F8
        f1a.addChild(new TreeNode("F6"))
                .addChild(new TreeNode("F7"))
                .addChild(new TreeNode("F8"));

        // F1b -> F9
        f1b.addChild(new TreeNode("F9"));

        return root;
    }

    private static class TreeNode {
        String name;
        List<TreeNode> children = new ArrayList<>();
        double subtreeWidth;
        double subtreeHeight;
        double centerX;
        double centerY;
        List<Transaction> tx = List.of(
                gson.fromJson(obj, Transaction.class),
                gson.fromJson(obj, Transaction.class),
                gson.fromJson(obj, Transaction.class)
        ); // todo... update the object to be show on ui
        BigDecimal amount = BigDecimal.valueOf(2.1); // todo... remove after completing the object above
        String opCode = "Jetton Internal Transfer"; // todo... remove after completing the object above

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

    private static final String obj = "{\n" +
            "  \"createdAt\" : 1741103012,\n" +
            "  \"seqno\" : 1457,\n" +
            "  \"wc\" : -1,\n" +
            "  \"shard\" : \"8000000000000000\",\n" +
            "  \"txHash\" : \"184F376A218E4F9E18EE5A8B5385D1BAACA1AF758E7170291E6D770EC1C4D842\",\n" +
            "  \"typeTx\" : \"Tx\",\n" +
            "  \"typeMsg\" : \"Ordinary\",\n" +
            "  \"accountAddress\" : \"3333333333333333333333333333333333333333333333333333333333333333\",\n" +
            "  \"txLt\" : 1765000002,\n" +
            "  \"status\" : \"Active\",\n" +
            "  \"from\" : {\n" +
            "    \"wc\" : -1,\n" +
            "    \"addr\" : \"0000000000000000000000000000000000000000000000000000000000000000\"\n" +
            "  },\n" +
            "  \"to\" : {\n" +
            "    \"wc\" : -1,\n" +
            "    \"addr\" : \"3333333333333333333333333333333333333333333333333333333333333333\"\n" +
            "  },\n" +
            "  \"blockRootHash\" : \"CA2E40D64EA166299703FD5AB178C03F13D5301FC66F09CDCBC762241A659200\",\n" +
            "  \"blockFileHash\" : \"00B2C6C3A7119472336ED77A32EC5ED17332FFC5E40901A3C31BE9CE376C89EA\",\n" +
            "  \"fromForSearch\" : \"0000000000000000000000000000000000000000000000000000000000000000\",\n" +
            "  \"toForSearch\" : \"3333333333333333333333333333333333333333333333333333333333333333\",\n" +
            "  \"amount\" : 2200000000,\n" +
            "  \"fees\" : 0,\n" +
            "  \"tx\" : {\n" +
            "    \"origStatus\" : \"Active\",\n" +
            "    \"endStatus\" : \"Active\",\n" +
            "    \"accountAddr\" : \"3333333333333333333333333333333333333333333333333333333333333333\",\n" +
            "    \"outMsgsCount\" : 0,\n" +
            "    \"inMsg\" : {\n" +
            "      \"srcAddr\" : {\n" +
            "        \"wc\" : -1,\n" +
            "        \"addr\" : \"0000000000000000000000000000000000000000000000000000000000000000\"\n" +
            "      },\n" +
            "      \"destAddr\" : {\n" +
            "        \"wc\" : -1,\n" +
            "        \"addr\" : \"3333333333333333333333333333333333333333333333333333333333333333\"\n" +
            "      },\n" +
            "      \"type\" : \"Internal\",\n" +
            "      \"value\" : {\n" +
            "        \"toncoins\" : 2200000000,\n" +
            "        \"otherCurrencies\" : [ ]\n" +
            "      },\n" +
            "      \"createdAt\" : 1741103012,\n" +
            "      \"createdLt\" : 1765000000,\n" +
            "      \"ihrDisabled\" : 1,\n" +
            "      \"bounce\" : 1,\n" +
            "      \"bounced\" : 0,\n" +
            "      \"body\" : {\n" +
            "        \"cells\" : [ ]\n" +
            "      },\n" +
            "      \"init\" : {\n" +
            "        \"code\" : [ ],\n" +
            "        \"data\" : [ ],\n" +
            "        \"library\" : [ ]\n" +
            "      },\n" +
            "      \"fwdFee\" : 0,\n" +
            "      \"importFee\" : 0,\n" +
            "      \"ihrFee\" : 0\n" +
            "    },\n" +
            "    \"outMsgs\" : [ ],\n" +
            "    \"prevTxHash\" : \"4FF3BFB463ED8B6D17ED3214D165CAE5E80C1492414FED02B25C45BFF21E69B0\",\n" +
            "    \"lt\" : 1765000002,\n" +
            "    \"prevTxLt\" : 1765000001,\n" +
            "    \"now\" : 1741103012,\n" +
            "    \"totalFees\" : {\n" +
            "      \"toncoins\" : 0,\n" +
            "      \"otherCurrencies\" : [ ]\n" +
            "    },\n" +
            "    \"oldHash\" : \"AB4B9EB70001A2D33FB46F91F87C83337D74CB0A1E2D0F8D60F376D9A17E1AF6\",\n" +
            "    \"newHash\" : \"184FAC9C1B2572DA2654C5E41A111DF7E2AF1FB44D84B9276BD175AB6211970B\",\n" +
            "    \"description\" : {\n" +
            "      \"type\" : \"Ordinary\",\n" +
            "      \"storage\" : {\n" +
            "        \"feesCollected\" : 0,\n" +
            "        \"feesDue\" : 0,\n" +
            "        \"statusChange\" : \"acst_unchanged\"\n" +
            "      },\n" +
            "      \"credit\" : {\n" +
            "        \"dueFeesCollected\" : 0,\n" +
            "        \"credit\" : {\n" +
            "          \"toncoins\" : 2200000000,\n" +
            "          \"otherCurrencies\" : [ ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"compute\" : {\n" +
            "        \"gasFees\" : 0,\n" +
            "        \"gasUsed\" : 3004,\n" +
            "        \"gasLimit\" : 10000000,\n" +
            "        \"gasCredit\" : 0,\n" +
            "        \"vmInitStateHash\" : \"0000000000000000000000000000000000000000000000000000000000000000\",\n" +
            "        \"vmFinalStateHash\" : \"0000000000000000000000000000000000000000000000000000000000000000\",\n" +
            "        \"accountActivated\" : 0,\n" +
            "        \"msgStateUsed\" : 0,\n" +
            "        \"success\" : 1,\n" +
            "        \"exitArg\" : \"nothing\",\n" +
            "        \"exitCode\" : 0,\n" +
            "        \"vmSteps\" : 62,\n" +
            "        \"mode\" : 0\n" +
            "      },\n" +
            "      \"action\" : {\n" +
            "        \"success\" : 1,\n" +
            "        \"valid\" : 1,\n" +
            "        \"noFunds\" : 0,\n" +
            "        \"statusChange\" : \"acst_unchanged\",\n" +
            "        \"totalFwdFee\" : 0,\n" +
            "        \"totalActionFee\" : 0,\n" +
            "        \"resultArg\" : 0,\n" +
            "        \"resultCode\" : 0,\n" +
            "        \"totActions\" : 0,\n" +
            "        \"specActions\" : 0,\n" +
            "        \"skippedActions\" : 0,\n" +
            "        \"msgsCreated\" : 0,\n" +
            "        \"totalMsgSizeCells\" : 0,\n" +
            "        \"totalMsgSizeBits\" : 0,\n" +
            "        \"actionListHash\" : \"96A296D224F285C67BEE93C30F8A309157F0DAA35DC5B87E410B78630A09CFC7\"\n" +
            "      },\n" +
            "      \"aborted\" : 0,\n" +
            "      \"destroyed\" : 0,\n" +
            "      \"creditFirst\" : 0\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
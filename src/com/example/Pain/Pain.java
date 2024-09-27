package com.example.Pain;

// Imports
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.SnapshotParameters;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Stack;

public class Pain extends Application {

    //Initial Declarations
    private final HashMap<Tab, Canvas> canvasMap = new HashMap<>();
    private final HashMap<Tab, GraphicsContext> gcMap = new HashMap<>();
    private final ImageView imageView = new ImageView();
    private File currentFile = null;
    private final Canvas canvas = new Canvas(1200, 800);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private double startX, startY, endX, endY;
    private int sideCount;
    private String textInput;
    private boolean isSaved,isDashedLine = false;
    private WritableImage canvasSnapshot, selectSnapshot, copySnapshot;
    ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    private boolean isSelecting, selectedArea, isPasting = false;
    private double selectionStartX, selectionStartY, selectionEndX, selectionEndY;
    private TabPane tabPane;

    //Undo/Redo Stacks
    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();

    private enum Tool { COPY_PASTE, FREE_DRAW, LINE, SQUARE, RECTANGLE, PENTAGON, CIRCLE, ELLIPSE, TRIANGLE, STAR, POLYGON, TEXTBOX}
    private Tool currentTool = Tool.FREE_DRAW;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        tabPane = new TabPane();
        root.setCenter(canvas);

        MenuBar menuBar = new MenuBar();

        isSaved = false;
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem insertImage = new MenuItem("Insert Image");
        MenuItem save = new MenuItem("Save");
        MenuItem saveAs = new MenuItem("Save As");
        MenuItem clearCanvas = new MenuItem("Clear Canvas");
        fileMenu.getItems().addAll(insertImage, save, saveAs, clearCanvas);

        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        MenuItem lineWidth = new MenuItem("Line Width");
        MenuItem pickColor = new MenuItem("Color Picker");
        MenuItem straightLine = new MenuItem("Straight Line");
        MenuItem square = new MenuItem("Square");
        MenuItem rectangle = new MenuItem("Rectangle");
        MenuItem pentagon = new MenuItem("Pentagon");
        MenuItem circle = new MenuItem("Circle");
        MenuItem ellipse = new MenuItem("Ellipse");
        MenuItem triangle = new MenuItem("Triangle");
        MenuItem star = new MenuItem("Star");
        MenuItem polygon = new MenuItem("Custom Polygon");
        MenuItem textPrompt = new MenuItem("Add Text");
        MenuItem toggleDashedLine = new MenuItem("Toggle Dashed Line");
        MenuItem toggleLineType = new MenuItem("Toggle Free Draw / Straight Line");
        MenuItem copyPaste = new MenuItem("Select and Copy-Paste");
        toolsMenu.getItems().addAll(copyPaste, lineWidth, pickColor, toggleLineType, toggleDashedLine, straightLine, square, rectangle, pentagon, circle, ellipse, triangle, star, polygon, textPrompt);

        toggleLineType.setOnAction(e -> {
            if (currentTool == Tool.FREE_DRAW) {
                currentTool = Tool.LINE; // Switch to Straight Line
                toggleLineType.setText("Straight Line (Click to switch to Free Draw)");
            } else {
                currentTool = Tool.FREE_DRAW; // Switch to Free Draw
                toggleLineType.setText("Free Draw (Click to switch to Straight Line)");
            }
        });

        toggleDashedLine.setOnAction(e -> {
            isDashedLine = !isDashedLine;  // Toggle the state

            // Set the line style based on the state
            if (isDashedLine) {
                gc.setLineDashes(10);  // Create dashed lines (10 units of dash length)
            } else {
                gc.setLineDashes(0);  // Reset to solid lines
            }
        });

        straightLine.setOnAction(e -> currentTool = Tool.LINE);
        square.setOnAction(e -> currentTool = Tool.SQUARE);
        rectangle.setOnAction(e -> currentTool = Tool.RECTANGLE);
        pentagon.setOnAction(e -> currentTool = Tool.PENTAGON);
        circle.setOnAction(e -> currentTool = Tool.CIRCLE);
        ellipse.setOnAction(e -> currentTool = Tool.ELLIPSE);
        triangle.setOnAction(e -> currentTool = Tool.TRIANGLE);
        star.setOnAction(e -> currentTool = Tool.STAR);
        copyPaste.setOnAction(e -> currentTool = Tool.COPY_PASTE);
        polygon.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Polygon Sides");
            dialog.setHeaderText("Enter the number of sides for the polygon:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(sides -> sideCount = Integer.parseInt(sides));
            currentTool = Tool.POLYGON;
        });
        textPrompt.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Text Input");
            dialog.setHeaderText("Enter the text to be displayed:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> textInput = text);
            currentTool = Tool.TEXTBOX;
        });

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem help = new MenuItem("Help");
        MenuItem about = new MenuItem("About");
        MenuItem javaPun = new MenuItem("Java Pun");
        helpMenu.getItems().addAll(help, about, javaPun);

        // Menu Retrieval
        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);

        // Create a VBox for Menu Bar
        VBox topContainer = new VBox(menuBar);
        root.setTop(topContainer);


        // Set initial drawing settings
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        // File menu actions
        insertImage.setOnAction(e -> insertImage(primaryStage));
        save.setOnAction(e -> saveImage());
        saveAs.setOnAction(e -> saveImageAs(primaryStage));
        clearCanvas.setOnAction(e -> clearCanvas(primaryStage));

        // Tools menu actions
        lineWidth.setOnAction(e -> showLineWidthDialog(primaryStage));
        pickColor.setOnAction(e -> showColorPickerDialog(primaryStage));

        // Help menu actions
        help.setOnAction(e -> showHelpDialog(primaryStage));
        about.setOnAction(e -> showAboutDialog(primaryStage));
        javaPun.setOnAction(e -> showPunDialog(primaryStage));

        // Set up drawing events
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            saveCanvasState();
            startX = e.getX();
            startY = e.getY();
            selectedArea = false;

            if (isPasting) {
                gc.drawImage(copySnapshot, startX, startY);
            }

            // Begin a path in Free Draw mode
            if (currentTool == Tool.FREE_DRAW) {
                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.stroke();  // Start drawing right away
            }

            if (currentTool == Tool.COPY_PASTE) {
                isSelecting = true;
                selectionStartX = e.getX();
                selectionStartY = e.getY();
            }

            canvasSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, canvasSnapshot);
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            endX = e.getX();
            endY = e.getY();

            checkAndResizeCanvas(endX, endY); // Ensure canvas resizes when needed

            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());  // Clear everything
            gc.drawImage(canvasSnapshot, 0, 0);

            switch (currentTool) {
                case LINE:
                    if (currentTool == Tool.LINE) {
                        gc.strokeLine(startX, startY, endX, endY);  // Draw a straight line
                    } else if (currentTool == Tool.FREE_DRAW) {
                        gc.lineTo(endX, endY);  // Draw freehand line
                        gc.stroke();  // Continue drawing as mouse moves
                    }
                    break;
                case COPY_PASTE:
                    double rectX = Math.min(selectionStartX, endX);
                    double rectY = Math.min(selectionStartY, endY);
                    double rectWidth = Math.abs(selectionStartX - endX);
                    double rectHeight = Math.abs(selectionStartY - endY);

                    gc.setStroke(Color.RED);  // Red border to visualize selection
                    gc.strokeRect(rectX, rectY, rectWidth, rectHeight);
                    break;
                case SQUARE:
                    drawSquare(startX, startY, endX, endY);
                    break;
                case RECTANGLE:
                    drawRectangle(startX, startY, endX, endY);
                    break;
                case PENTAGON:
                    drawPentagon(startX, startY, endX, endY);
                    break;
                case CIRCLE:
                    drawCircle(startX, startY, endX, endY);
                    break;
                case ELLIPSE:
                    drawEllipse(startX, startY, endX, endY);
                    break;
                case TRIANGLE:
                    drawTriangle(startX, startY, endX, endY);
                    break;
                case STAR:
                    drawStar(startX, startY, endX, endY);
                    break;
                case POLYGON:
                    drawPolygon(startX, startY, endX, endY, sideCount);
                    break;
                case TEXTBOX:
                    drawText(startX, startY, textInput);
                    break;
                default:
                    gc.lineTo(endX, endY);  // Free draw
                    gc.stroke();
                    break;
            }
            isSaved = false;
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            endX = e.getX();
            endY = e.getY();

            checkAndResizeCanvas(endX, endY); // Ensure canvas resizes when needed

            // Draw the final shape when mouse is released
            switch (currentTool) {
                case COPY_PASTE:
                    SnapshotParameters params = new SnapshotParameters();
                    double rectX = Math.min(selectionStartX, endX);
                    double rectY = Math.min(selectionStartY, endY);
                    double rectWidth = Math.abs(selectionStartX - endX);
                    double rectHeight = Math.abs(selectionStartY - endY);

                    params.setViewport(new Rectangle2D(rectX, rectY, rectWidth, rectHeight));  // Set selection viewport

                    selectSnapshot = new WritableImage((int) rectWidth, (int) rectHeight);
                    canvas.snapshot(params, selectSnapshot);  // Take snapshot of selected area
                    selectedArea = true;
                    break;
                case LINE:
                    if (currentTool == Tool.LINE) {
                        gc.strokeLine(startX, startY, endX, endY);  // Draw final straight line
                    } else if (currentTool == Tool.FREE_DRAW) {
                        gc.lineTo(endX, endY);  // Final free draw line
                        gc.stroke();  // Finish the freehand drawing
                    }
                    break;
                case SQUARE:
                    drawSquare(startX, startY, endX, endY);
                    break;
                case RECTANGLE:
                    drawRectangle(startX, startY, endX, endY);
                    break;
                case PENTAGON:
                    drawPentagon(startX, startY, endX, endY);
                    break;
                case CIRCLE:
                    drawCircle(startX, startY, endX, endY);
                    break;
                case ELLIPSE:
                    drawEllipse(startX, startY, endX, endY);
                    break;
                case TRIANGLE:
                    drawTriangle(startX, startY, endX, endY);
                    break;
                case STAR:
                    drawStar(startX, startY, endX, endY);
                    break;
                case POLYGON:
                    drawPolygon(startX, startY, endX, endY, sideCount);
                    break;
                case TEXTBOX:
                    drawText(startX, startY, textInput);
                    break;
                default:
                    break;
            }

            isSaved = false;  // Mark the drawing as unsaved
        });


        // Scene Initialization
        Scene scene = new Scene(root, 1600, 900);

        scene.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                saveImage();
            } else if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN).match(event)) {
                undoAction();
            } else if (new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN).match(event)) {
                redoAction();
            }
            /*
            else if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event)) {
                copyAction();
            } else if (new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN).match(event)) {
                pasteAction();
            }
            */
        });

        primaryStage.setTitle("Pain");
        primaryStage.setScene(scene);

        // Add close confirmation if unsaved changes exist
        primaryStage.setOnCloseRequest(event -> {
            if (!isSaved) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes. Do you want to save before exiting?");
                ButtonType saveAndExit = new ButtonType("Save and Exit");
                ButtonType exitWithoutSaving = new ButtonType("Exit Without Saving");
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(saveAndExit, exitWithoutSaving, cancel);

                alert.showAndWait().ifPresent(response -> {
                    if (response == saveAndExit) {
                        saveImage();
                    } else if (response == cancel) {
                        event.consume();
                    }
                });
            }
        });

        primaryStage.show();
    }

    // Image Insertion
    private void insertImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                Image image = new Image(new FileInputStream(file));

                // Adjust canvas size if image is larger
                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();
                checkAndResizeCanvas(imageWidth, imageHeight);

                imageView.setImage(image);
                gc.drawImage(image, 0, 0);
                currentFile = file;

                isSaved = false;

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void checkAndResizeCanvas(double newWidth, double newHeight) {
        double currentWidth = canvas.getWidth();
        double currentHeight = canvas.getHeight();

        // Always resize dynamically when the drawn area exceeds the current canvas size
        if (newWidth > currentWidth || newHeight > currentHeight) {
            // Capture the current state of the canvas
            WritableImage snapshot = new WritableImage((int) currentWidth, (int) currentHeight);
            canvas.snapshot(null, snapshot);

            // Set the new canvas dimensions, allowing expansion but not shrinkage
            canvas.setWidth(Math.max(newWidth, currentWidth));
            canvas.setHeight(Math.max(newHeight, currentHeight));

            // Redraw the snapshot on the resized canvas
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); // Clear the canvas
            gc.drawImage(snapshot, 0, 0); // Redraw previous content
        }
    }

    // Clear Canvas Function
    private void clearCanvas(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Clearing Canvas...");
        alert.setHeaderText("You are about to delete everything on your canvas. Are you sure?");
        ButtonType clearConfirm = new ButtonType("Clear Canvas");
        ButtonType cancelClear = new ButtonType("Exit Without Saving", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(clearConfirm, cancelClear);

        alert.showAndWait().ifPresent(response -> {
            if (response == clearConfirm) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.WHITE);
                gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
                isSaved = false;
            }
        });
    }

    // Save Function
    private void saveImage() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveImageAs(null);
        }
        isSaved = true;
    }

    // Save As Function
    private void saveImageAs(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP Files", "*.bmp")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            saveToFile(file);
            currentFile = file;
            isSaved = true;
        }
    }

    // Save Background Function
    private void saveToFile(File file) {
        try {
            String format = getFileExtension(file.getName());
            if (format == null || !(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpg") ||
                    format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("bmp"))) {
                return;
            }
            RenderedImage renderedImage = SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null);
            ImageIO.write(renderedImage, format, file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Extension Retrieval
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? null : fileName.substring(dotIndex + 1);
    }

    // Dialog to adjust line width
    private void showLineWidthDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Adjust Line Width");

        Label widthLabel = new Label("Current Width: " + gc.getLineWidth());

        Slider slider = new Slider(1, 20, gc.getLineWidth());
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> gc.setLineWidth(newVal.doubleValue()));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            gc.setLineWidth(newVal.doubleValue());
            widthLabel.setText("Current Width: " + String.format("%.2f", newVal.doubleValue()));
        });

        VBox layout = new VBox(10, widthLabel, slider);
        layout.setSpacing(10);
        layout.setStyle("-fx-padding: 20;");

        Scene scene = new Scene(layout, 300, 150);
        dialog.setScene(scene);
        dialog.show();
    }

    // Dialog to select line color
    private void showColorPickerDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Select Line Color");

        ColorPicker picker = new ColorPicker((Color) gc.getStroke());
        picker.setOnAction(e -> gc.setStroke(picker.getValue()));

        Scene scene = new Scene(picker, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    // Undo the last action
    private void undoAction() {
        if (!undoStack.isEmpty()) {
            // Save the current state to the redo stack before undoing
            WritableImage currentSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, currentSnapshot);
            redoStack.push(currentSnapshot);

            // Restore the last state from the undo stack
            WritableImage lastImage = undoStack.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(lastImage, 0, 0);
            isSaved = false;
        }
    }

    /**

     private void copyAction() {
        if (selectedArea) {
            // Assign the selected snapshot to copySnapshot
            copySnapshot = new WritableImage(selectSnapshot.getPixelReader(),
                    (int) selectSnapshot.getWidth(),
                    (int) selectSnapshot.getHeight());
            isPasting = false;  // Reset pasting flag until the user presses paste
        }
    }


    private void pasteAction() {
        if (copySnapshot != null) {
            isPasting = true;  // Activate pasting mode
            canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (isPasting) {
                    double pasteX = e.getX();
                    double pasteY = e.getY();

                    gc.drawImage(copySnapshot, pasteX, pasteY);  // Paste the copied snapshot
                    isPasting = false;  // Deactivate pasting after one paste operation
                }
            });
        }
    }

    **/
    
    // Redo the last undone action
    private void redoAction() {
        if (!redoStack.isEmpty()) {
            // Save the current state to the undo stack before redoing
            WritableImage currentSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, currentSnapshot);
            undoStack.push(currentSnapshot);

            // Restore the last state from the redo stack
            WritableImage nextImage = redoStack.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(nextImage, 0, 0);
            isSaved = false;
        }
    }


    private void drawSquare(double startX, double startY, double endX, double endY) {
        double side = Math.min(Math.abs(endX - startX), Math.abs(endY - startY));
        gc.strokeRect(startX, startY, side, side);
    }

    private void drawRectangle(double startX, double startY, double endX, double endY) {
        gc.strokeRect(startX, startY, Math.abs(endX - startX), Math.abs(endY - startY));
    }

    private void drawPentagon(double startX, double startY, double endX, double endY) {
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;

        double[] xPoints = new double[5];
        double[] yPoints = new double[5];

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72);
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }

        gc.strokePolygon(xPoints, yPoints, 5);
    }

    private void drawCircle(double startX, double startY, double endX, double endY) {
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        gc.strokeOval(startX - radius, startY - radius, radius * 2, radius * 2);
    }

    private void drawEllipse(double startX, double startY, double endX, double endY) {
        gc.strokeOval(startX, startY, Math.abs(endX - startX), Math.abs(endY - startY));
    }

    private void drawTriangle(double startX, double startY, double endX, double endY) {
        double midX = (startX + endX) / 2;
        gc.strokePolygon(new double[]{startX, endX, midX}, new double[]{endY, endY, startY}, 3);
    }

    private void drawStar(double startX, double startY, double endX, double endY) {
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;

        double[] xPoints = new double[10];
        double[] yPoints = new double[10];

        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(i * 36);
            double length = (i % 2 == 0) ? radius : radius / 2;
            xPoints[i] = centerX + length * Math.cos(angle);
            yPoints[i] = centerY - length * Math.sin(angle);
        }

        gc.strokePolygon(xPoints, yPoints, 10);
    }

    private void drawPolygon(double startX, double startY, double endX, double endY, int sides) {
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;

        double[] xPoints = new double[sides];
        double[] yPoints = new double[sides];

        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(i * ((double) 360 /sides));
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }

        gc.strokePolygon(xPoints, yPoints, sides);
    }

    private void drawText(double startX, double startY, String textDisplay) {
        gc.strokeText(textDisplay, startX, startY);
    }

    // Save the current canvas state for undo/redo
    private void saveCanvasState() {
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, snapshot);
        undoStack.push(snapshot);
    }

    // Dialog to show "About" information
    private void showAboutDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("About");

        Label aboutLabel = new Label("Pain Application\nHave mercy on us Rosasco\nWe beg before you.");
        Scene scene = new Scene(aboutLabel, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    // Dialog to show "Pun" information
    private void showPunDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Pun");

        Label aboutLabel = new Label("Why do Java Developers wear glasses?\nThey don't see sharp!");
        Scene scene = new Scene(aboutLabel, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    // Dialog to show "Help" information
    private void showHelpDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Help");

        Label helpLabel = new Label("Dude it's so simple. Add an image (or don't)\n Draw if you want: you can change color and width\nDon't forget to save STUPID");
        Scene scene = new Scene(helpLabel, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

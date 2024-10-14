package com.example.Pain;

// Imports
import javafx.application.*;
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
import javafx.stage.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * The Pain class is effectively the main command
 * console of my Pain project; Scene setup,
 * actions, and most of my declarations are
 * within this field
 */

public class Pain extends Application {

    //Initial Declarations
    private Timer autosaveTimer;
    private final long AUTOSAVE = 60000;
    private long countdown = AUTOSAVE / 1000;
    private Label timerLabel;
    private ToggleButton timerButton;
    private final HashMap<Tab, Canvas> canvasMap = new HashMap<>();
    private final HashMap<Tab, GraphicsContext> gcMap = new HashMap<>();
    private final ImageView imageView = new ImageView();
    private File currentFile = null;
    private final Canvas canvas = new Canvas(1200, 800);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private double startX, startY, endX, endY;
    private int sideCount, pointCount;
    private String textInput;
    private boolean isSaved,isDashedLine = false;
    private WritableImage canvasSnapshot, selectSnapshot, copySnapshot;
    private boolean isSelecting, selectedArea, isPasting = false;
    private double selectionStartX, selectionStartY, selectionEndX, selectionEndY;
    private TabPane tabPane;
    //private int tabCounter = 1;

    //Undo/Redo Stacks
    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();

    //Thread Setup
    private static BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static Logger logger = Logger.getLogger("Paint Thread Log");

    static {
        try {
            FileHandler fileHandler = new FileHandler("paint_log.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to set up text file", e);
        }
    }
    // Start the logging thread
    static Thread loggingThread = new Thread(new LoggerTask());

    static {
        try {
            loggingThread.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to set up logger", e);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private enum Tool { COPY_PASTE, FREE_DRAW, LINE, SQUARE, RECTANGLE, PENTAGON, CIRCLE, ELLIPSE, TRIANGLE, STAR, CUSTOMSTAR, POLYGON, TEXTBOX}
    private Tool currentTool = Tool.FREE_DRAW;


    /**
     *
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet.
     * Applications may create other stages, if needed, but they will not be
     * primary stages and will not be embedded in the browser.
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * Shout out IntelliJ for doing this one on its own; basically this is what
     *                     everything is called within
     */
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        //tabPane = new TabPane();
        root.setCenter(canvas);
        autosaveTimer = new Timer();
        MenuBar menuBar = new MenuBar();

        isSaved = false;
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        // File Menu
        Menu fileMenu = new Menu("File");
        Label insertImageLabel = new Label("Insert Image");
        Tooltip insertImageTooltip = new Tooltip("Click to insert an image");
        Tooltip.install(insertImageLabel, insertImageTooltip);
        CustomMenuItem insertImage = new CustomMenuItem(insertImageLabel);
        Label saveLabel = new Label("Save");
        Tooltip saveTooltip = new Tooltip("Click to save the same way as before");
        Tooltip.install(saveLabel, saveTooltip);
        CustomMenuItem save = new CustomMenuItem(saveLabel);
        Label saveAsLabel = new Label("Save As");
        Tooltip saveAsTooltip = new Tooltip("Click to save in a different way than before");
        Tooltip.install(saveAsLabel, saveAsTooltip);
        CustomMenuItem saveAs = new CustomMenuItem(saveAsLabel);
        Label rotateLabel = new Label("Rotate 90 Degrees");
        Tooltip rotateTooltip = new Tooltip("Click to rotate canvas 90 Degrees");
        Tooltip.install(rotateLabel, rotateTooltip);
        CustomMenuItem rotate = new CustomMenuItem();
        Label mirrorHLabel = new Label("mirrorH");
        Tooltip mirrorHTooltip = new Tooltip("Click to flip canvas horizontally over its origin");
        Tooltip.install(mirrorHLabel, mirrorHTooltip);
        CustomMenuItem mirrorHorizontally = new CustomMenuItem(mirrorHLabel);
        Label mirrorVLabel = new Label("mirrorV");
        Tooltip mirrorVTooltip = new Tooltip("Click to flip canvas vertically over its origin");
        Tooltip.install(mirrorVLabel, mirrorVTooltip);
        CustomMenuItem mirrorVertically = new CustomMenuItem(mirrorVLabel);
        Label clearCanvasLabel = new Label("clearCanvas");
        Tooltip clearCanvasTooltip = new Tooltip("Click to erase everything on your canvas");
        Tooltip.install(clearCanvasLabel, clearCanvasTooltip);
        CustomMenuItem clearCanvas = new CustomMenuItem(clearCanvasLabel);
        fileMenu.getItems().addAll(insertImage, save, saveAs, rotate, mirrorHorizontally, mirrorVertically, clearCanvas);

        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        Label lineWidthLabel = new Label("Line Width");
        try {
            ImageView lineWidthIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\lineWidthImage.png")));
            lineWidthIcon.setFitHeight(50);
            lineWidthIcon.setFitWidth(50);
            lineWidthLabel.setGraphic(lineWidthIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip lineWidthTooltip = new Tooltip("Change width of drawn lines or shape outlines");
        Tooltip.install(lineWidthLabel, lineWidthTooltip);
        CustomMenuItem lineWidth = new CustomMenuItem(lineWidthLabel);

        Label colorPickerLabel = new Label("Color Picker");
        try {
            ImageView colorPickerIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\colorPickerImage.png")));
            colorPickerIcon.setFitHeight(50);
            colorPickerIcon.setFitWidth(50);
            colorPickerLabel.setGraphic(colorPickerIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip colorPickerTooltip = new Tooltip("Change color of lines and shapes drawn");
        Tooltip.install(colorPickerLabel, colorPickerTooltip);
        CustomMenuItem pickColor = new CustomMenuItem(colorPickerLabel);

        Label squareLabel = new Label("Square");
        try {
            ImageView squareIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\squareImage.png")));
            squareIcon.setFitHeight(50);
            squareIcon.setFitWidth(50);
            squareLabel.setGraphic(squareIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip squareTooltip = new Tooltip("Drag to draw a Square");
        Tooltip.install(squareLabel, squareTooltip);
        CustomMenuItem square = new CustomMenuItem(squareLabel);

        Label rectangleLabel = new Label("Rectangle");
        try {
            ImageView rectangleIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\rectangleImage.png")));
            rectangleIcon.setFitHeight(50);
            rectangleIcon.setFitWidth(50);
            rectangleLabel.setGraphic(rectangleIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip rectangleTooltip = new Tooltip("Drag to draw a Rectangle");
        Tooltip.install(rectangleLabel, rectangleTooltip);
        CustomMenuItem rectangle = new CustomMenuItem(rectangleLabel);

        Label pentagonLabel = new Label("Pentagon");
        try {
            ImageView pentagonIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\pentagonImage.png")));
            pentagonIcon.setFitHeight(50);
            pentagonIcon.setFitWidth(50);
            pentagonLabel.setGraphic(pentagonIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip pentagonTooltip = new Tooltip("Drag to draw a Pentagon");
        Tooltip.install(pentagonLabel, pentagonTooltip);
        CustomMenuItem pentagon = new CustomMenuItem(pentagonLabel);

        Label circleLabel = new Label("Circle");
        try {
            ImageView circleIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\circleImage.png")));
            circleIcon.setFitHeight(50);
            circleIcon.setFitWidth(50);
            circleLabel.setGraphic(circleIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip circleTooltip = new Tooltip("Drag to draw a Circle");
        Tooltip.install(circleLabel, circleTooltip);
        CustomMenuItem circle = new CustomMenuItem(circleLabel);

        Label ellipseLabel = new Label("Ellipse");
        try {
            ImageView ellipseIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\ellipseImage.png")));
            ellipseIcon.setFitHeight(50);
            ellipseIcon.setFitWidth(50);
            ellipseLabel.setGraphic(ellipseIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip ellipseTooltip = new Tooltip("Drag to draw an Ellipse");
        Tooltip.install(ellipseLabel, ellipseTooltip);
        CustomMenuItem ellipse = new CustomMenuItem(ellipseLabel);

        Label triangleLabel = new Label("Triangle");
        try {
            ImageView triangleIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\triangleImage.png")));
            triangleIcon.setFitHeight(50);
            triangleIcon.setFitWidth(50);
            triangleLabel.setGraphic(triangleIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip triangleTooltip = new Tooltip("Drag to draw a Triangle");
        Tooltip.install(triangleLabel, triangleTooltip);
        CustomMenuItem triangle = new CustomMenuItem(triangleLabel);

        Label starLabel = new Label("Star");
        try {
            ImageView starIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\starImage.png")));
            starIcon.setFitHeight(50);
            starIcon.setFitWidth(50);
            starLabel.setGraphic(starIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip starTooltip = new Tooltip("Drag to draw a 5-pointed Star");
        Tooltip.install(starLabel, starTooltip);
        CustomMenuItem star = new CustomMenuItem(starLabel);

        Label starCustomLabel = new Label("Custom Star");
        try {
            ImageView starCustomIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\starCustomImage.png")));
            starCustomIcon.setFitHeight(50);
            starCustomIcon.setFitWidth(50);
            starCustomLabel.setGraphic(starCustomIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip starCustomTooltip = new Tooltip("Drag to draw a Star with a custom number of points");
        Tooltip.install(starCustomLabel, starCustomTooltip);
        CustomMenuItem starCustom = new CustomMenuItem(starCustomLabel);

        Label polygonLabel = new Label("Custom Polygon");
        try {
            ImageView polygonIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\polygonImage.png")));
            polygonIcon.setFitHeight(50);
            polygonIcon.setFitWidth(50);
            polygonLabel.setGraphic(polygonIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip polygonTooltip = new Tooltip("Drag to draw a Polygon with a custom number of sides");
        Tooltip.install(polygonLabel, polygonTooltip);
        CustomMenuItem polygon = new CustomMenuItem(polygonLabel);

        Label textPromptLabel = new Label("Add Text");
        try {
            ImageView textPromptIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\textPromptImage.png")));
            textPromptIcon.setFitHeight(50);
            textPromptIcon.setFitWidth(50);
            textPromptLabel.setGraphic(textPromptIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip textPromptTooltip = new Tooltip("Click to add a textbox");
        Tooltip.install(textPromptLabel, textPromptTooltip);
        CustomMenuItem textPrompt = new CustomMenuItem(textPromptLabel);

        Label toggleDashedLineLabel = new Label("Toggle Dashed Line");
        try {
            ImageView toggleDashedLineIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\dashedLineImage.png")));
            toggleDashedLineIcon.setFitHeight(50);
            toggleDashedLineIcon.setFitWidth(50);
            toggleDashedLineLabel.setGraphic(toggleDashedLineIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip toggleDashedLineTooltip = new Tooltip("Click to change between a solid and dashed line");
        Tooltip.install(toggleDashedLineLabel, toggleDashedLineTooltip);
        CustomMenuItem toggleDashedLine = new CustomMenuItem(toggleDashedLineLabel);

        Label toggleLineTypeLabel = new Label("Toggle Free Draw/Straight Line");
        try {
            ImageView toggleLineTypeIcon = new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\lineTypeImage.png")));
            toggleLineTypeIcon.setFitHeight(50);
            toggleLineTypeIcon.setFitWidth(50);
            toggleLineTypeLabel.setGraphic(toggleLineTypeIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip toggleLineTypeTooltip = new Tooltip("Click to change between drawing a straight line and a scribbled line");
        Tooltip.install(toggleLineTypeLabel, toggleLineTypeTooltip);
        CustomMenuItem toggleLineType = new CustomMenuItem(toggleLineTypeLabel);

        /*
        Label selectAreaLabel = new Label("Select Area");
        try {
            squareLabel.setGraphic(new ImageView(new Image(new FileInputStream("C:\\CS250\\CS_Scary\\buttonImages\\squareImage.png"))));
            squareIcon.setFitHeight(50);
            squareIcon.setFitWidth(50);
            squareLabel.setGraphic(squareIcon);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Tooltip selectAreaTooltip = new Tooltip("Select an area (useful for copy/paste and rotate custom area)");
        Tooltip.install(selectAreaLabel, selectAreaTooltip);
        CustomMenuItem selectArea = new CustomMenuItem(selectAreaLabel);
         */

        // selectArea has been removed
        toolsMenu.getItems().addAll(lineWidth, pickColor, toggleLineType, toggleDashedLine, square, rectangle, pentagon, circle, ellipse, triangle, star, starCustom, polygon, textPrompt);

        /*
          The next several sets of lines establish actions to be
          taken whenever a tool is changed to or when the
          "action to set it" is taken
         */

        toggleLineType.setOnAction(e -> {
            if (currentTool == Tool.FREE_DRAW) {
                log("Tool selected: Straight Line");
                currentTool = Tool.LINE; // Switch to Straight Line
                toggleLineType.setText("Straight Line (Click to switch to Free Draw)");
            } else {
                log("Tool selected: Free Draw");
                currentTool = Tool.FREE_DRAW; // Switch to Free Draw
                toggleLineType.setText("Free Draw (Click to switch to Straight Line)");
            }
        });

        toggleDashedLine.setOnAction(e -> {
            isDashedLine = !isDashedLine;  // Toggle the state
            // Set the line style based on the state
            if (isDashedLine) {
                log("Tool selected: Dashed Line");
                gc.setLineDashes(10);  // Create dashed lines (10 units of dash length)
            } else {
                log("Tool selected: No Dashed Line");
                gc.setLineDashes(0);  // Reset to solid lines
            }
        });

        square.setOnAction(e -> {
            log("Tool selected: Square");
            currentTool = Tool.SQUARE;
        });
        rectangle.setOnAction(e -> {
            log("Tool selected: Rectangle");
            currentTool = Tool.RECTANGLE;
        });
        pentagon.setOnAction(e -> {
            log("Tool selected: Pentagon");
            currentTool = Tool.PENTAGON;
        });
        circle.setOnAction(e -> {
            log("Tool selected: Circle");
            currentTool = Tool.CIRCLE;
        });
        ellipse.setOnAction(e -> {
            log("Tool selected: Ellipse");
            currentTool = Tool.ELLIPSE;
        });
        triangle.setOnAction(e -> {
            log("Tool selected: Triangle");
            currentTool = Tool.TRIANGLE;
        });
        star.setOnAction(e -> {
            log("Tool selected: Star");
            currentTool = Tool.STAR;
        });
        /*
        selectArea.setOnAction(e -> {
            log("Tool selected: Copy/Paste");
            currentTool = Tool.COPY_PASTE
        });
         */

        starCustom.setOnAction(e -> {
            log("Tool selected: Custom Star");
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Star Points");
            dialog.setHeaderText("Enter the number of points for the star:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(points -> pointCount = Integer.parseInt(points));
            currentTool = Tool.CUSTOMSTAR;
        });

        polygon.setOnAction(e -> {
            log("Tool selected: Polygon");
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Polygon Sides");
            dialog.setHeaderText("Enter the number of sides for the polygon:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(sides -> sideCount = Integer.parseInt(sides));
            currentTool = Tool.POLYGON;
        });

        textPrompt.setOnAction(e -> {
            log("Tool selected: Text Box");
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Text Input");
            dialog.setHeaderText("Enter the text to be displayed:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> textInput = text);
            currentTool = Tool.TEXTBOX;
        });

        // Help Menu
        Menu helpMenu = new Menu("Help");
        Label helpLabel = new Label("Help");
        Tooltip helpTooltip = new Tooltip("Click to get help");
        Tooltip.install(helpLabel, helpTooltip);
        CustomMenuItem help = new CustomMenuItem(helpLabel);
        Label aboutLabel = new Label("About");
        Tooltip aboutTooltip = new Tooltip("Click for info about Pain(t)");
        Tooltip.install(aboutLabel, aboutTooltip);
        CustomMenuItem about = new CustomMenuItem(aboutLabel);
        Label javaPunLabel = new Label("Java Pun");
        Tooltip javaPunTooltip = new Tooltip("Click to get a pun about Java");
        Tooltip.install(javaPunLabel, javaPunTooltip);
        CustomMenuItem javaPun = new CustomMenuItem(javaPunLabel);
        helpMenu.getItems().addAll(help, about, javaPun);

        // Menu Retrieval
        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);

        // Create a VBox for Menu Bar
        VBox topContainer = new VBox(menuBar);
        root.setTop(topContainer);

        // Timer setup
        timerLabel = new Label("Autosave in: " + countdown + "s");
        timerButton = new ToggleButton("Show Autosave Timer");
        Tooltip timerButtonTooltip = new Tooltip("Click to activate autosave timer countdown");
        timerButton.setTooltip(timerButtonTooltip);
        VBox timerBox = new VBox(timerLabel, timerButton);
        root.setRight(timerBox);
        timerLabel.setVisible(false);
        setupCountdown();

        // Timer Button setup
        timerButton.setOnAction(e -> {
            timerLabel.setVisible(timerButton.isSelected());
        });

        // Set initial drawing settings
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        // File menu actions
        insertImage.setOnAction(e -> insertImage(primaryStage));
        save.setOnAction(e -> saveImage());
        saveAs.setOnAction(e -> saveImageAs(primaryStage));
        rotate.setOnAction(e -> rotateScreen(primaryStage));
        mirrorHorizontally.setOnAction(e -> mirrorHorizontally(gc, canvas));
        mirrorVertically.setOnAction(e -> mirrorVertically(gc, canvas));
        clearCanvas.setOnAction(e -> clearCanvas(primaryStage));


        // Tools menu actions
        lineWidth.setOnAction(e -> showLineWidthDialog(primaryStage));
        pickColor.setOnAction(e -> showColorPickerDialog(primaryStage));

        // Help menu actions
        help.setOnAction(e -> showHelpDialog(primaryStage));
        about.setOnAction(e -> showAboutDialog(primaryStage));
        javaPun.setOnAction(e -> showPunDialog(primaryStage));

        /*
         * The next section contains all the mouse interaction
         * code including hovering, clicking, dragging, and releasing
         */

        // Set up drawing events
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            saveCanvasState();
            startX = e.getX();
            startY = e.getY();
            log("Mouse Pressed: X: " + startX + " Y: " + startY);
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
                    gc.strokeLine(startX, startY, endX, endY);  // Draw a straight line
                    gc.lineTo(endX, endY);  // Draw freehand line
                    gc.stroke();  // Continue drawing as mouse moves
                    break;
                case COPY_PASTE:
                    selectionEndX = endX;
                    selectionEndY = endY;

                    double rectX = Math.min(selectionStartX, selectionEndX);
                    double rectY = Math.min(selectionStartY, selectionEndY);
                    double rectWidth = Math.abs(selectionStartX - selectionEndX);
                    double rectHeight = Math.abs(selectionStartY - selectionEndY);

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
                case CUSTOMSTAR:
                    drawCustomStar(startX, startY, endX, endY, pointCount);
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
            log("Mouse Released: X: " + endX + " Y: " + endY);
            checkAndResizeCanvas(endX, endY); // Ensure canvas resizes when needed

            // Draw the final shape when mouse is released
            switch (currentTool) {
                case COPY_PASTE:
                    selectionEndX = endX;
                    selectionEndY = endY;

                    SnapshotParameters params = new SnapshotParameters();
                    double rectX = Math.min(selectionStartX, selectionEndX);
                    double rectY = Math.min(selectionStartY, selectionEndY);
                    double rectWidth = Math.abs(selectionStartX - selectionEndX);
                    double rectHeight = Math.abs(selectionStartY - selectionEndY);

                    params.setViewport(new Rectangle2D(rectX, rectY, rectWidth, rectHeight));  // Set selection viewport
                    selectSnapshot = new WritableImage((int) rectWidth, (int) rectHeight);
                    canvas.snapshot(params, selectSnapshot);  // Take snapshot of selected area
                    selectedArea = true;
                    isSelecting = false;
                    break;
                case LINE:
                    log("Line Drawn");
                    gc.strokeLine(startX, startY, endX, endY);  // Draw final straight line
                    gc.lineTo(endX, endY);  // Final free draw line
                    gc.stroke();  // Finish the freehand drawing
                break;
                case SQUARE:
                    log("Square Drawn");
                    drawSquare(startX, startY, endX, endY);
                    break;
                case RECTANGLE:
                    log("Rectangle Drawn");
                    drawRectangle(startX, startY, endX, endY);
                    break;
                case PENTAGON:
                    log("Pentagon Drawn");
                    drawPentagon(startX, startY, endX, endY);
                    break;
                case CIRCLE:
                    log("Circle Drawn");
                    drawCircle(startX, startY, endX, endY);
                    break;
                case ELLIPSE:
                    log("Ellipse Drawn");
                    drawEllipse(startX, startY, endX, endY);
                    break;
                case TRIANGLE:
                    log("Triangle Drawn");
                    drawTriangle(startX, startY, endX, endY);
                    break;
                case STAR:
                    log("Star Drawn");
                    drawStar(startX, startY, endX, endY);
                    break;
                case CUSTOMSTAR:
                    log(pointCount + "-pointed Star Drawn");
                    drawCustomStar(startX, startY, endX, endY, pointCount);
                    break;
                case POLYGON:
                    log(sideCount + "-sided Polygon Drawn");
                    drawPolygon(startX, startY, endX, endY, sideCount);
                    break;
                case TEXTBOX:
                    log("Text Drawn with Content: " + textInput);
                    drawText(startX, startY, textInput);
                    break;
                default:
                    break;
            }

            isSaved = false;  // Mark the drawing as unsaved
        });

        /*
         * This section handles ensuring that there is confirmation when
         * the user attempts to close their unsaved work
         */
        primaryStage.setOnCloseRequest(event -> {
            log("Attempt to close paint...");
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
                        log("Closing Paint...");
                        shutdownLogging();
                    } else if (response == cancel) {
                        event.consume();
                        log("Canceled Closing");
                    } else if (response == exitWithoutSaving) {
                        log("Closing Paint...");
                        if (autosaveTimer != null) {
                            autosaveTimer.cancel();
                        }
                        shutdownLogging();
                    }
                });
            }
            else {
                shutdownLogging();
                log("Project Saved. Exiting Paint...");
            }
        });

        setupCountdown();

        // Scene Initialization
        Scene scene = new Scene(root, 1600, 900);

        /*
         * This section controls keyboard shortcuts
         */

        scene.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                saveImage();
            } else if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN).match(event)) {
                undoAction();
            } else if (new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN).match(event)) {
                redoAction();
            } /*else if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event)) {
                copyAction();
            } else if (new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN).match(event)) {
                pasteAction();
            }*/
        });

        primaryStage.setTitle("Pain");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Rotates the canvas and gc
     * @param owner
     */
    private void rotateScreen(Stage owner) {
        log("Rotating Screen...");
        // Snapshot the current canvas content before rotating
        WritableImage canvasSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, canvasSnapshot);

        // Clear the canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Save the current GraphicsContext state
        gc.save();

        // Translate and rotate the canvas
        gc.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
        gc.rotate(90);
        gc.translate(-canvas.getWidth() / 2, -canvas.getHeight() / 2);

        // Redraw the snapshot of the canvas content after rotation
        gc.drawImage(canvasSnapshot, 0, 0);

        // Restore the GraphicsContext state
        gc.restore();
    }

    /**
     * Flips (mirrors) the canvas horizontally.
     *
     * @param gc     The GraphicsContext of the canvas.
     * @param canvas The Canvas object to flip horizontally.
     */
    private void mirrorHorizontally(GraphicsContext gc, Canvas canvas) {
        log("Mirroring Screen Horizontally...");
        // Take a snapshot of the current canvas content
        WritableImage canvasSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, canvasSnapshot);

        // Clear the canvas before flipping
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Save the current state of the GraphicsContext
        gc.save();

        // Flip horizontally: scale X-axis by -1, Y-axis by 1
        gc.translate(canvas.getWidth(), 0);  // Move the origin to the right edge
        gc.scale(-1, 1);  // Flip horizontally

        // Redraw the image on the flipped canvas
        gc.drawImage(canvasSnapshot, 0, 0);

        // Restore the original GraphicsContext state
        gc.restore();
    }

    /**
     * Flips (mirrors) the canvas vertically.
     *
     * @param gc     The GraphicsContext of the canvas.
     * @param canvas The Canvas object to flip vertically.
     */
    private void mirrorVertically(GraphicsContext gc, Canvas canvas) {
        log("Mirroring Screen Vertically...");
        // Take a snapshot of the current canvas content
        WritableImage canvasSnapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, canvasSnapshot);

        // Clear the canvas before flipping
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Save the current state of the GraphicsContext
        gc.save();

        // Flip vertically: scale X-axis by 1, Y-axis by -1
        gc.translate(0, canvas.getHeight());  // Move the origin to the bottom edge
        gc.scale(1, -1);  // Flip vertically

        // Redraw the image on the flipped canvas
        gc.drawImage(canvasSnapshot, 0, 0);

        // Restore the original GraphicsContext state
        gc.restore();
    }

    /**
     * Image Insertion
     * @param stage
     */
    private void insertImage(Stage stage) {
        log("Inserting Image...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            currentFile = file;
            try {
                Image image = new Image(Files.newInputStream(file.toPath()));

                // Adjust canvas size if image is larger
                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();
                checkAndResizeCanvas(imageWidth, imageHeight);

                imageView.setImage(image);
                gc.drawImage(image, 0, 0);
                currentFile = file;

                isSaved = false;
                log("Inserted Image: " + currentFile.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else {log("Insert Image Canceled");}
    }

    /**
     * This section handles automatic resize of the canvas
     * when the user draws off the side of the screen
     * @param newWidth
     * @param newHeight
     */
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

            log("Resized Canvas to " + canvas.getWidth() + "x" + canvas.getHeight());
        }
    }

    /**
     * This section handles clearing the canvas when
     * the clear canvas button is interacted with
     * @param owner
     */
    private void clearCanvas(Stage owner) {
        log("Clearing Canvas...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Clearing Canvas...");
        alert.setHeaderText("You are about to delete everything on your canvas. Are you sure?");
        ButtonType clearConfirm = new ButtonType("Clear Canvas");
        ButtonType cancelClear = new ButtonType("Exit Without Saving", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(clearConfirm, cancelClear);

        alert.showAndWait().ifPresent(response -> {
            if (response == clearConfirm) {
                log("Canvas Cleared");
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.WHITE);
                gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
                isSaved = false;
            }
            else{log("Canvas Clear Canceled");}
        });
    }


    /**
     * This section handles the Save function
     */
    private void saveImage() {
        if (currentFile != null) {
            saveToFile(currentFile);
            log("Saved to File " + currentFile);
        } else {
            saveImageAs(null);
        }
        isSaved = true;
        countdown = AUTOSAVE / 1000;
    }

    /**
     * This section handles the SaveAs function
     *      Note: not the same as the save function, though it is called
     */
    private void saveImageAs(Stage stage) {
        log("Attempting new save...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP Files", "*.bmp")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            if (currentFile != null) {
                String currentFormat = getFileExtension(currentFile.getName());
                String newFormat = getFileExtension(file.getName());

                // If formats are different, warn the user
                if (!currentFormat.equalsIgnoreCase(newFormat)) {
                    boolean confirmed = showFormatWarning(currentFormat, newFormat);
                    if (!confirmed) {
                        System.out.println("Save canceled due to format change.");
                        log("Save canceled due to format change");
                        return;  // User canceled save
                    }
                }
            }
            saveToFile(file);
            currentFile = file;
            isSaved = true;
            log("Saved to new file " + currentFile);
        }
    }

    /**
     * This section handles the actual writing of the image
     * to the file format and saving bit data
     * @param file
     */
    private void saveToFile(File file) {
        try {
            String format = getFileExtension(file.getName());
            if (format == null || !(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpg") ||
                    format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("bmp"))) {
                return;
            }
            RenderedImage renderedImage = SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null);
            ImageIO.write(renderedImage, format, file);
            countdown = AUTOSAVE / 1000;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This section gets the file format based on the file's name
     * @param fileName
     * @return
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        } else {
            return null;
        }
    }

    /**
     * This section displays the dialog for line width
     * @param owner
     */
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
            log("Line Width Changed to " + newVal.doubleValue());
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

    /**
     * This section handles the color picker dialog
     * @param owner
     */
    private void showColorPickerDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Select Line Color");

        ColorPicker picker = new ColorPicker((Color) gc.getStroke());
        picker.setOnAction(e -> {
            log("Color changed to " + picker.getValue());
            gc.setStroke(picker.getValue());
        });

        Scene scene = new Scene(picker, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * This section handles the undo action shortcut
     */
    private void undoAction() {
        if (!undoStack.isEmpty()) {
            log("Undid Last Action");
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
        else{log("Attempted to Undo Last Action");}
    }

    /**
     * This section handles the copy action shortcut
     */
    private void copyAction() {
        if (selectedArea) {
            log("Copied Area");
            // Assign the selected snapshot to copySnapshot
            copySnapshot = new WritableImage(selectSnapshot.getPixelReader(),
                    (int) selectSnapshot.getWidth(),
                    (int) selectSnapshot.getHeight());
            isPasting = false;  // Reset pasting flag until the user presses paste
        }
    }

    /**
     * This section handles the paste action shortcut
     */
    private void pasteAction() {
        if (copySnapshot != null) {
            log("Pasted Area");
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

    /**
     * This section handles the redo action shortcut
     */
    private void redoAction() {
        if (!redoStack.isEmpty()) {
            log("Redid Last Action");
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
        else{log("Attempted to Redo Last Action");}
    }

    /**
     * This section handles the construction of a square
     * when the user interacts with the square button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawSquare(double startX, double startY, double endX, double endY) {
        double side = Math.min(Math.abs(endX - startX), Math.abs(endY - startY));
        gc.strokeRect(startX, startY, side, side);
    }

    /**
     * This section handles the construction of a rectangle
     * when the user interacts with the rectangle button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawRectangle(double startX, double startY, double endX, double endY) {
        gc.strokeRect(startX, startY, Math.abs(endX - startX), Math.abs(endY - startY));
    }

    /**
     * This section handles the construction of a pentagon
     * when the user interacts with the pentagon button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
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

    /**
     * This section handles the construction of a circle
     * when the user interacts with the circle button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawCircle(double startX, double startY, double endX, double endY) {
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        gc.strokeOval(startX - radius, startY - radius, radius * 2, radius * 2);
    }

    /**
     * This section handles the construction of an ellipse
     * when the user interacts with the ellipse button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawEllipse(double startX, double startY, double endX, double endY) {
        gc.strokeOval(startX, startY, Math.abs(endX - startX), Math.abs(endY - startY));
    }

    /**
     * This section handles the construction of a triangle
     * when the user interacts with the triangle button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawTriangle(double startX, double startY, double endX, double endY) {
        double midX = (startX + endX) / 2;
        gc.strokePolygon(new double[]{startX, endX, midX}, new double[]{endY, endY, startY}, 3);
    }

    /**
     * This section handles the construction of a star
     * when the user interacts with the star button
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
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

    /**
     * This section handles the construction of a polygon
     * when the user interacts with the polygon button
     * and chooses how many sides they want it to have
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param points
     */
    private void drawCustomStar(double startX, double startY, double endX, double endY, int points) {
        if (points < 4) return;
        double radius = Math.min(Math.abs(endX - startX), Math.abs(endY - startY)) / 2;
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;

        double[] xPoints = new double[points * 2];
        double[] yPoints = new double[points * 2];

        for (int i = 0; i < points * 2; i++) {
            double angle = i * (Math.PI/points);
            double length = (i % 2 == 0) ? radius : radius / 2;
            xPoints[i] = centerX + length * Math.cos(angle);
            yPoints[i] = centerY - length * Math.sin(angle);
        }

        gc.strokePolygon(xPoints, yPoints, points * 2);
    }

    /**
     * This section handles the construction of a polygon
     * when the user interacts with the polygon button
     * and chooses how many sides they want it to have
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param sides
     */
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

    /**
     * This section handles the display of text boxes
     * @param startX
     * @param startY
     * @param textDisplay
     */
    private void drawText(double startX, double startY, String textDisplay) {
        gc.strokeText(textDisplay, startX, startY);
    }

    /**
     * This section handles the saving of the canvas for
     * while dragging a shape and undo/redo functions
     */
    private void saveCanvasState() {
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, snapshot);
        undoStack.push(snapshot);
    }

    /**
     * Dialog to show "About" information
     * @param owner
     */
    private void showAboutDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("About");

        Label aboutInfoLabel = new Label(" Pain Application\n Have mercy on us Rosasco\n We beg before you\n (Should I tell you that the schedule on this is painful?\n Maybe I'll just add it at the end of the paint log?)");
        Scene scene = new Scene(aboutInfoLabel, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Dialog to show "Pun" information
     * @param owner
     */
    private void showPunDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Pun");

        Label punInfoLabel = new Label(" Why do Java Developers wear glasses?\n They don't see sharp!");
        Scene scene = new Scene(punInfoLabel, 225, 50);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Dialog to show "Help" information
     * @param owner
     */
    private void showHelpDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Help");

        Label helpLabel = new Label(" Dude it's so simple. Add an image (or don't)\n Draw if you want: you can change color and width\n (also works for shapes)\n Don't forget to save Lukasz\n (this includes logs after running obviously)");
        Scene scene = new Scene(helpLabel, 300, 100);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Shows a warning dialog if the user is attempting to save the file in a different format.
     *
     * @param currentFormat
     * @param newFormat
     * @return
     */
    private boolean showFormatWarning(String currentFormat, String newFormat) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("File Format Warning");
        alert.setHeaderText("You are saving in a different format.");
        alert.setContentText("The current format is: ." + currentFormat + "\nYou are trying to save as: ." + newFormat +
                "\nThis may cause loss of quality or data.\nDo you want to proceed?");

        // Display the warning and wait for the user's response
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Dialog to show and implement autosave function
     */
    private void setupCountdown() {
        autosaveTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    countdown--;
                    timerLabel.setText("Autosave in: " + countdown + "s");

                    if (countdown <= 0) {
                        if (currentFile != null) {
                            saveImage();
                        }
                        countdown = AUTOSAVE / 1000;
                    }
                });
            }
        }, 1000, 1000);
    }

    public static void log(String message) {
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Failed to log message", e);
        }
    }

    private static class LoggerTask implements Runnable {
        @Override
        public void run() {
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    String logMessage = logQueue.take();
                    logger.log(Level.INFO, logMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Logging thread interrupted, shutting down");
            }
        }
    }

    public static void shutdownLogging() {
        boolean shutdownSuccess = true;
        if (loggingThread != null && loggingThread.isAlive()) {
            loggingThread.interrupt();
            try {
                loggingThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Logger shutdown interrupted", e);
                shutdownSuccess = false;
            }
        }
        for (java.util.logging.Handler handler : logger.getHandlers()) {
            handler.flush();  // Flush any remaining logs
            handler.close();  // Close the handler to release resources
        }
        if(shutdownSuccess) {
            logger.log(Level.INFO, "Logger shutdown");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

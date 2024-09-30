/**
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class PainTest {

    private Pain painApp;  // Your main app class
    private Canvas canvas;  // Mock canvas for testing
    private GraphicsContext gc;

    @BeforeEach
    public void setup() {
        painApp = new Pain();  // Initialize the Pain app before each test
        canvas = new Canvas(1200, 800);  // Set up a mock canvas
        gc = canvas.getGraphicsContext2D();
    }

    // Test 1: Testing getFileExtension()
    @Test
    public void testGetFileExtension() {
        // Test for a file with an extension
        String fileName = "example.png";
        String extension = painApp.getFileExtension(fileName);
        assertEquals("png", extension, "File extension should be 'png'");

        // Test for a file without an extension
        String noExtensionFile = "example";
        String noExtension = painApp.getFileExtension(noExtensionFile);
        assertNull(noExtension, "File without extension should return null");
    }

    // Test 2: Testing insertImage() (without actual file access)
    @Test
    public void testInsertImage() {
        Stage stage = new Stage();  // Create a dummy stage for testing

        // Mock behavior: Assume the file is selected and an image is inserted
        File mockImageFile = new File("mockImage.png");

        // Since actual file selection opens a window, we assume file is selected
        assertDoesNotThrow(() -> painApp.insertImage(stage), "insertImage should handle file insertion properly");

        // In a real test, you could mock FileChooser and check if the image is drawn on the canvas
    }

    // Test 3: Testing clearCanvas()
    @Test
    public void testClearCanvas() {
        Stage stage = new Stage();  // Create a dummy stage for testing
        gc.setFill(javafx.scene.paint.Color.BLACK);  // Assume the canvas has some drawing

        // Call the clearCanvas() method to clear the canvas
        painApp.clearCanvas(stage);

        // Check if the canvas was cleared (filled with white color)
        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, image);  // Capture the state of the canvas
        assertEquals(javafx.scene.paint.Color.WHITE, gc.getFill(), "Canvas should be filled with white after clearing");
    }
}
**/
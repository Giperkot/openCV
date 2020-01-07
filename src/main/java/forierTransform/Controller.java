package forierTransform;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {

    @FXML
    private Button loadFileBtn;

    @FXML
    private Button transformButton;

    @FXML
    private Button antitransformButton;

    @FXML
    private ImageView originalImage;

    @FXML
    private ImageView transformedImage;

    @FXML
    private ImageView antitransformedImage;

    private FileChooser fileChooser;

    private Main main;

    private Mat image;

    private Mat complexImage;

    private List<Mat> planes;

    @FXML
    private void onLoadFile () {
        File file = new File("./images/");
        this.fileChooser.setInitialDirectory(file);
        file = this.fileChooser.showOpenDialog(this.main.getStage());

        if (file != null) {
            // read the image in gray scale
            this.image = Imgcodecs.imread(file.getAbsolutePath());
            Imgproc.cvtColor(this.image, this.image, Imgproc.COLOR_BGR2GRAY);

            // show the image
            this.updateImageView(originalImage, Utils.mat2Image(this.image));
            // set a fixed width
            this.originalImage.setFitWidth(250);
            // preserve image ratio
            this.originalImage.setPreserveRatio(true);
            // update the UI
            this.transformButton.setDisable(false);

            // empty the image planes and the image views if it is not the first
            // loaded image
            if (!this.planes.isEmpty()) {
                this.planes.clear();
                this.transformedImage.setImage(null);
                this.antitransformedImage.setImage(null);
            }

        }
    }

    /**
     * Init the needed variables
     */
    protected void init()
    {
        this.fileChooser = new FileChooser();
        this.image = new Mat();
        this.planes = new ArrayList<>();
        this.complexImage = new Mat();
    }

    /**
     * Optimize the image dimensions
     *
     * @param image
     *            the {@link Mat} to optimize
     * @return the image whose dimensions have been optimized
     */
    private Mat optimizeImageDim(Mat image)
    {
        // init
        Mat padded = new Mat();
        // get the optimal rows size for dft
        int addPixelRows = Core.getOptimalDFTSize(image.rows());
        // get the optimal cols size for dft
        int addPixelCols = Core.getOptimalDFTSize(image.cols());
        // apply the optimal cols and rows size to the image
        Core.copyMakeBorder(image, padded, 0, addPixelRows - image.rows(), 0, addPixelCols - image.cols(),
                Core.BORDER_CONSTANT, Scalar.all(0));

        return padded;
    }

    /**
     * The action triggered by pushing the button for apply the dft to the
     * loaded image
     */
    @FXML
    protected void transformImage()
    {
        // optimize the dimension of the loaded image
        Mat padded = this.optimizeImageDim(this.image);
        padded.convertTo(padded, CvType.CV_32F);
        // prepare the image planes to obtain the complex image
        this.planes.add(padded);
        this.planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
        // prepare a complex image for performing the dft
        Core.merge(this.planes, this.complexImage);

        // dft
        Core.dft(this.complexImage, this.complexImage);

        // optimize the image resulting from the dft operation
        Mat magnitude = this.createOptimizedMagnitude(this.complexImage);

        // show the result of the transformation as an image
        this.updateImageView(transformedImage, Utils.mat2Image(magnitude));
        // set a fixed width
        this.transformedImage.setFitWidth(250);
        // preserve image ratio
        this.transformedImage.setPreserveRatio(true);

        // enable the button for performing the antitransformation
        this.antitransformButton.setDisable(false);
        // disable the button for applying the dft
        this.transformButton.setDisable(true);
    }

    /**
     * The action triggered by pushing the button for apply the inverse dft to
     * the loaded image
     */
    @FXML
    protected void antitransformImage()
    {
        Core.idft(this.complexImage, this.complexImage);

        Mat restoredImage = new Mat();
        Core.split(this.complexImage, this.planes);
        Core.normalize(this.planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);

        // move back the Mat to 8 bit, in order to proper show the result
        restoredImage.convertTo(restoredImage, CvType.CV_8U);

        this.updateImageView(antitransformedImage, Utils.mat2Image(restoredImage));
        // set a fixed width
        this.antitransformedImage.setFitWidth(250);
        // preserve image ratio
        this.antitransformedImage.setPreserveRatio(true);

        // disable the button for performing the antitransformation
        this.antitransformButton.setDisable(true);
    }


    /**
     * Optimize the magnitude of the complex image obtained from the DFT, to
     * improve its visualization
     *
     * @param complexImage
     *            the complex image obtained from the DFT
     * @return the optimized image
     */
    private Mat createOptimizedMagnitude(Mat complexImage)
    {
        // init
        List<Mat> newPlanes = new ArrayList<>();
        Mat mag = new Mat();
        // split the comples image in two planes
        Core.split(complexImage, newPlanes);
        // compute the magnitude
        Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);

        // move to a logarithmic scale
        Core.add(Mat.ones(mag.size(), CvType.CV_32F), mag, mag);
        Core.log(mag, mag);
        // optionally reorder the 4 quadrants of the magnitude image
        this.shiftDFT(mag);
        // normalize the magnitude image for the visualization since both JavaFX
        // and OpenCV need images with value between 0 and 255
        // convert back to CV_8UC1
        mag.convertTo(mag, CvType.CV_8UC1);
        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);

        // you can also write on disk the resulting image...
        // Imgcodecs.imwrite("../magnitude.png", mag);

        return mag;
    }

    /**
     * Reorder the 4 quadrants of the image representing the magnitude, after
     * the DFT
     *
     * @param image
     *            the {@link Mat} object whose quadrants are to reorder
     */
    private void shiftDFT(Mat image)
    {
        image = image.submat(new Rect(0, 0, image.cols() & -2, image.rows() & -2));
        int cx = image.cols() / 2;
        int cy = image.rows() / 2;

        Mat q0 = new Mat(image, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(image, new Rect(cx, 0, cx, cy));
        Mat q2 = new Mat(image, new Rect(0, cy, cx, cy));
        Mat q3 = new Mat(image, new Rect(cx, cy, cx, cy));

        Mat tmp = new Mat();
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);

        q1.copyTo(tmp);
        q2.copyTo(q1);
        tmp.copyTo(q2);
    }


    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view
     *            the {@link ImageView} to update
     * @param image
     *            the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image)
    {
        Utils.onFXThread(view.imageProperty(), image);
    }


    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }
}

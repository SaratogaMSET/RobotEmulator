package org._649mset;

import dialogfx.DialogFX;
import dialogfx.DialogFXBuilder;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.camera.AxisCamera;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.DirectoryChooserBuilder;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.tools.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;

public class RobotEmulator extends Application {
    private static final int CANCELLED = -1;
    private static final int UNACCEPTABLE = 0;
    private static final int ACCEPTABLE = 1;
    public static final String SRC_PREFIX = "src";
    private static RobotEmulator instance;
    private boolean robotEnabled = false;
    private int teamId = 0;
    private RobotMode robotMode = RobotMode.TELEOP;
    private Point joystickCircleDragDelta = new Point();
    private final byte[][] joystickAxes = new byte[6][];
    private short[] joystickButtons = new short[]{0, 0, 0, 0};
    private int selectedJoystick = 0;
    private VBox analogChannelBox;
    private VBox digitalChannelBox;
    private VBox encoderChannelBox;
    private VBox speedControllerBox;
    private VBox solenoidBox;
    private VBox servosBox;
    private VBox relaysBox;
    private HashMap<Encoder, Integer> encoderValues = new HashMap<>();
    private HashMap<AnalogChannel, Double> analogValues = new HashMap<>();
    private HashMap<DigitalInput, Boolean> digitalInputMap = new HashMap<>();
    private HashMap<SafePWM, Text> speedControllerMap = new HashMap<>();
    private HashMap<Integer, Text> solenoidMap = new HashMap<>();
    private HashMap<Servo, Text> servosMap = new HashMap<>();
    private HashMap<Relay, Text> relaysMap = new HashMap<>();
    private Parent parent;
    private Stage stage;
    public static final String OUTPUT_LOCATION = "externalBuild";
    private File outputDirectory;

    public static void main(String[] args) {
        launch(args);
    }

    public RobotEmulator() {
        super();
    }

    @Override
    public void start(final Stage stage) throws Exception {
        this.stage = stage;
        instance = this;
        stage.setTitle("FRC Robot Emulator");
        VBox grid = new VBox();
        grid.setAlignment(Pos.CENTER);
        Scene scene = new Scene(grid);
        stage.setScene(scene);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
        final File jdkHomeFile = new File("jdkHome");
        Text sceneTitle = new Text("Robot Emulator");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.getChildren().add(sceneTitle);
        final Button fileChooserButton = new Button("Choose Base Folder");

        fileChooserButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                DirectoryChooser chooser = DirectoryChooserBuilder.create().build();
                Path codeDirectory = Paths.get(System.getProperty("user.home"));
                int resultCode;
                boolean first = true;
                do {
                    try {
                        chooser.setInitialDirectory(first ? codeDirectory.toFile() : codeDirectory.getParent().toFile());
                        codeDirectory = chooser.showDialog(stage).toPath();
                        resultCode = checkCodeDirectory(codeDirectory);

                    } catch (Exception | Error e) {
                        resultCode = CANCELLED;
                        codeDirectory = null;
                    }
                    first = false;
                } while (resultCode == UNACCEPTABLE);
                if (resultCode == CANCELLED)
                    return;
                startRobotEmulator(stage, codeDirectory);
            }
        });
        Button jdkChooserButton = new Button("Choose JDK Folder");
        jdkChooserButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                getJdkHomeFromUser(stage, jdkHomeFile, fileChooserButton);
            }
        });
        grid.getChildren().add(fileChooserButton);
        grid.getChildren().add(jdkChooserButton);
        grid.setSpacing(25);
        grid.setPadding(new Insets(25, 25, 25, 25));
        stage.show();
        if (!setJdkHome(jdkHomeFile))
            fileChooserButton.setDisable(true);
        else if (getParameters().getUnnamed().size() > 0) {
            Path codeDirectory = new File(getParameters().getUnnamed().get(0)).toPath();
            if (checkCodeDirectory(codeDirectory) == ACCEPTABLE)
                startRobotEmulator(stage, codeDirectory);
        }
    }

    private int checkCodeDirectory(Path codeDirectory) {
        int resultCode;
        resultCode = isAcceptableCodeFilePath(codeDirectory);
        if (resultCode == UNACCEPTABLE) {
            DialogFXBuilder builder = DialogFXBuilder.create();
            builder.type(DialogFX.Type.QUESTION);
            builder.message("Invalid directory selected.");
            ArrayList<String> buttons = new ArrayList<>();
            buttons.add("Cancel");
            buttons.add("Retry");
            builder.buttons(buttons, 1, 2);
            resultCode = builder.build().showDialog() == 1 ? UNACCEPTABLE : CANCELLED;
        }
        return resultCode;
    }

    private boolean setJdkHome(File jdkHomeFile) {
        try {
            System.setProperty("java.home", new Scanner(jdkHomeFile).nextLine());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean getJdkHomeFromUser(Stage stage, File prefs, Button codeButton) {
        DirectoryChooser chooser = DirectoryChooserBuilder.create().build();
        File codeDirectory = new File(System.getProperty("user.home"));
        int resultCode;
        boolean first = true;
        do {
            try {
                chooser.setInitialDirectory(first ? codeDirectory : codeDirectory.getParentFile());
                codeDirectory = chooser.showDialog(stage);
                resultCode = isAcceptableJDKHome(codeDirectory);
                if (resultCode == UNACCEPTABLE) {
                    DialogFXBuilder builder = DialogFXBuilder.create();
                    builder.type(DialogFX.Type.QUESTION);
                    builder.message("Invalid directory selected.");
                    ArrayList<String> buttons = new ArrayList<>();
                    buttons.add("Cancel");
                    buttons.add("Retry");
                    builder.buttons(buttons, 1, 2);
                    resultCode = builder.build().showDialog() == 1 ? UNACCEPTABLE : CANCELLED;
                }

            } catch (Exception | Error e) {
                resultCode = CANCELLED;
                codeDirectory = null;
            }
            first = false;
        } while (resultCode == UNACCEPTABLE);
        if (resultCode != CANCELLED) {
            try {
                prefs.delete();
                FileWriter writer = new FileWriter(prefs);
                writer.write(codeDirectory.getAbsolutePath());
                writer.close();
                codeButton.setDisable(false);
                setJdkHome(prefs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultCode != CANCELLED;
    }

    private int isAcceptableCodeFilePath(Path codeDirectory) {
        if (codeDirectory == null)
            return CANCELLED;
        Path mainFilePath = getMainFilePath(codeDirectory);
        return Files.exists(mainFilePath) ? ACCEPTABLE : UNACCEPTABLE;
    }

    private int isAcceptableJDKHome(File codeDirectory) {
        return codeDirectory.getName().startsWith("jdk") ? ACCEPTABLE : UNACCEPTABLE;
    }

    public static RobotEmulator getInstance() {
        return instance;
    }

    private void startRobotEmulator(Stage stage, Path codeDirectory) {
        try {
            ArrayList<JavaFileObject> filesToCompile = copyFiles(codeDirectory);
            if (compileFiles(filesToCompile)) {
                Path mainFilePath = getMainFilePath(codeDirectory);
                Path mainFile = codeDirectory.resolve(SRC_PREFIX).relativize(mainFilePath);
                try {
                    Class<RobotBase> cls = loadClass(mainFile.toString().replace(File.separator, "."));
                    initWindow(stage);
                    final RobotBase instance = cls.newInstance();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                instance.startApp();
                            } catch (Throwable e) {
                                showErrorDialog(e);
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                DialogFXBuilder.create().message("Compilation failed.").type(DialogFX.Type.ERROR).build().showDialog();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Class<RobotBase> loadClass(String mainFile) {
        Class<RobotBase> cls = null;
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{outputDirectory.toURI().toURL()}, getClass().getClassLoader());
            cls = (Class<RobotBase>) classLoader.loadClass(mainFile.substring(0, mainFile.lastIndexOf('.')));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return cls;
    }

    private void initWindow(Stage stage) throws IOException {
        String resourceUrl = "/res/RobotEmulator.fxml";
        URL file = getClass().getResource(resourceUrl);
        final Parent parent = FXMLLoader.load(file);
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        setBorderStyle(parent);
        initGui(parent);
    }

    private ArrayList<JavaFileObject> copyFiles(final Path codeDirectory) throws IOException {
        final Path externalSources = Files.createTempDirectory("externalSources");
        final ArrayList<JavaFileObject> javaFileObjects = new ArrayList<>();
        Files.walkFileTree(codeDirectory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = externalSources.resolve(codeDirectory.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().toLowerCase().endsWith(".java"))
                    javaFileObjects.add(new JavaSourceFromPath(Files.copy(file, externalSources.resolve(codeDirectory.relativize(file)), StandardCopyOption.REPLACE_EXISTING)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return javaFileObjects;
    }

    private boolean compileFiles(ArrayList<JavaFileObject> filesToCompile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        List<String> optionList = new ArrayList<>();
        optionList.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path")));
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        outputDirectory = Files.createTempDirectory(Paths.get(""), OUTPUT_LOCATION).toFile();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Files.walkFileTree(Paths.get(outputDirectory.getAbsolutePath()), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            file.toFile().delete();
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            dir.toFile().delete();
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                Arrays.asList(outputDirectory));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, optionList, null, filesToCompile);
        return task.call();
    }

    public static void showErrorDialog(Throwable t) {
        final StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        final Stage stage = RobotEmulator.getInstance().getStage();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                DialogFXBuilder.create().message(sw.toString()).type(DialogFX.Type.ERROR).build().showDialog(stage.getX() + stage.getWidth() / 2, stage.getY() + stage.getHeight() / 2);
            }
        });
    }

    private Stage getStage() {
        return stage;
    }

    private void setBorderStyle(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Parent)
                setBorderStyle((Parent) node);
            if (node instanceof Pane) {
                node.getStyleClass().add("pane");
            }
        }
    }

    private void initGui(final Parent parent) {
        this.parent = parent;
        final RadioButton disabledButton = (RadioButton) parent.lookup("#disabledButton");
        disabledButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (disabledButton.isSelected())
                    robotEnabled = false;
            }
        });
        final RadioButton enabledButton = (RadioButton) parent.lookup("#enabledButton");
        enabledButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (enabledButton.isSelected())
                    robotEnabled = true;
            }
        });
        final TextField teamNumberField = (TextField) parent.lookup("#teamNumberField");
        setTextFieldNumeric(teamNumberField);
        teamNumberField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                try {
                    teamId = Integer.parseInt(teamNumberField.getText());
                } catch (Exception ignored) {
                }
            }
        });
        final RadioButton teleopModeButton = (RadioButton) parent.lookup("#teleopMode");
        teleopModeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (teleopModeButton.isSelected())
                    robotMode = RobotMode.TELEOP;
                disabledButton.setSelected(true);
                robotEnabled = false;
            }
        });
        final RadioButton autonomousModeButton = (RadioButton) parent.lookup("#autonomousMode");
        autonomousModeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (autonomousModeButton.isSelected())
                    robotMode = RobotMode.AUTONOMOUS;
                disabledButton.setSelected(true);
                robotEnabled = false;
            }
        });
        final RadioButton testModeButton = (RadioButton) parent.lookup("#testMode");
        testModeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (testModeButton.isSelected())
                    robotMode = RobotMode.TEST;
                disabledButton.setSelected(true);
                robotEnabled = false;
            }
        });
        initJoysticks(parent);
        this.analogChannelBox = (VBox) parent.lookup("#analogSensorBox");
        this.digitalChannelBox = (VBox) parent.lookup("#digitalSensorBox");
        this.encoderChannelBox = (VBox) parent.lookup("#encodersBox");
        this.speedControllerBox = (VBox) parent.lookup("#speedControllerBox");
        this.solenoidBox = (VBox) parent.lookup("#solenoidBox");
        this.servosBox = (VBox) parent.lookup("#servosBox");
        this.relaysBox = (VBox) parent.lookup("#relaysBox");
    }

    private void initJoysticks(Parent parent) {
        synchronized (this) {
            initJoystickButtons(parent);
            for (int i = 0; i < 4; i++) {
                initJoystickAxes(parent, i);
                initJoystickButtonGroups(parent, i);
            }
        }
    }

    private void initJoystickButtons(Parent parent) {
        for (int i = 0; i < 12; i++) {
            final CheckBox checkBox = (CheckBox) parent.lookup("#joystickButton" + (i + 1));
            final int finalI = i;
            checkBox.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    joystickButtons[selectedJoystick] += (checkBox.isSelected() ? 1 : -1) * Math.pow(2, finalI);
                }
            });
        }
    }

    private void initJoystickButtonGroups(final Parent parent, final int joystickIndex) {
        ((RadioButton) parent.lookup("#joystickButtonGroup" + (joystickIndex + 1))).setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                selectedJoystick = joystickIndex;
                for (int i = 0; i < 12; i++)
                    ((CheckBox) parent.lookup("#joystickButton" + (i + 1))).setSelected((0x1 << i & joystickButtons[joystickIndex]) != 0);
            }
        });
    }

    private void initJoystickAxes(Parent parent, int i) {
        joystickAxes[i] = new byte[6];
        final int finalI = i;
        final Circle joystickCircle = (Circle) parent.lookup("#joystickCircle" + (i + 1));
        final Pane joystickCircleParent = (Pane) joystickCircle.getParent();
        joystickCircle.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                joystickCircleDragDelta.x = (int) (joystickCircle.getCenterX() - mouseEvent.getSceneX());
                joystickCircleDragDelta.y = (int) (joystickCircle.getCenterY() - mouseEvent.getSceneY());
                joystickCircle.setCursor(Cursor.CLOSED_HAND);
            }
        });
        joystickCircle.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                double joystickCircleParentWidth = joystickCircleParent.getWidth();

                double layoutX = Math.max(Math.min(mouseEvent.getSceneX() + joystickCircleDragDelta.x, joystickCircleParentWidth), 0);
                if (Math.abs(joystickCircleParentWidth / 2 - layoutX) < 0.1 * joystickCircleParentWidth) {
                    layoutX = joystickCircleParentWidth / 2;
                }
                joystickCircle.setCenterX(layoutX);
                joystickAxes[finalI][Joystick.AxisType.kX.value] = (byte) ((layoutX - joystickCircleParentWidth / 2) * 255. / joystickCircleParentWidth);

                double joystickCircleParentHeight = joystickCircleParent.getHeight();
                double layoutY = Math.max(Math.min(mouseEvent.getSceneY() + joystickCircleDragDelta.y, joystickCircleParentHeight), 0);
                if (Math.abs(joystickCircleParentHeight / 2 - layoutY) < 0.1 * joystickCircleParentHeight) {
                    layoutY = joystickCircleParentHeight / 2;
                }
                joystickCircle.setCenterY(layoutY);
                joystickAxes[finalI][Joystick.AxisType.kY.value] = (byte) ((layoutY - joystickCircleParentHeight / 2) * 255. / joystickCircleParentHeight);


                joystickCircle.setCursor(Cursor.HAND);
            }
        });
        joystickCircle.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double joystickCircleParentWidth = joystickCircleParent.getWidth();
                double layoutX = Math.max(Math.min(mouseEvent.getSceneX() + joystickCircleDragDelta.x, joystickCircleParentWidth), 0);
                joystickCircle.setCenterX(layoutX);
                joystickAxes[finalI][Joystick.AxisType.kX.value] = (byte) ((layoutX - joystickCircleParentWidth / 2) * 255. / joystickCircleParentWidth);
                double joystickCircleParentHeight = joystickCircleParent.getHeight();
                double layoutY = Math.max(Math.min(mouseEvent.getSceneY() + joystickCircleDragDelta.y, joystickCircleParentHeight), 0);
                joystickCircle.setCenterY(layoutY);
                joystickAxes[finalI][Joystick.AxisType.kY.value] = (byte) ((layoutY - joystickCircleParentHeight / 2) * 255. / joystickCircleParentHeight);
            }
        });
        joystickCircle.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                joystickCircle.setCursor(Cursor.HAND);
            }
        });
    }

    private Path getMainFilePath(Path codeDirectory) {
        Path manifestFile = codeDirectory.resolve("resources" + java.io.File.separator + "META-INF" + java.io.File.separator + "MANIFEST.MF");
        if (!Files.exists(manifestFile)) {
            System.out.println("manifest does not exist");
            return null;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(Files.newInputStream(manifestFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String mainFilePath = "";
        if (in != null) {
            try {
                while (in.ready()) {
                    String line = in.readLine();
                    if (line.startsWith("MIDlet-1: ")) {
                        mainFilePath = line.substring(line.indexOf(',') + 4);
                        break;
                    }
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return codeDirectory.toAbsolutePath().resolve(Paths.get(SRC_PREFIX + File.separator + mainFilePath.replace(".", File.separator) + ".java"));
    }

    public RobotMode getRobotMode() {
        return robotMode;
    }

    //TODO set up analog and digital ins
    public short getDSDigitalIn() {
        return 0;
    }

    public short getDSAnalogIn(int i) {
        return 0;
    }

    public short getButtons(int i) {
        return joystickButtons[i];
    }

    public byte[] getAxes(int i) {
        if (joystickAxes[i] == null)
            return new byte[6];
        else
            return joystickAxes[i];
    }

    public char getAlliance() {
        return 'B';
    }

    public char getPosition() {
        return '1';
    }

    public int getTeamId() {
        return teamId;
    }

    public boolean isRobotEnabled() {
        return robotEnabled;
    }

    public void addAnalogChannel(final AnalogChannel analogChannel) {
        final HBox hBox = new HBox(10);
        analogValues.put(analogChannel, 0.);
        Text text = new Text(analogChannel.getChannel() + "");
        text.setId("text");
        hBox.getChildren().add(text);
        TextField textField = new TextField();
        textField.setText("0");
        setTextFieldNumeric(textField);
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                try {
                    analogValues.put(analogChannel, Double.parseDouble(s2));
                } catch (Exception ignored) {
                }
            }
        });
        hBox.getChildren().add(textField);
        final ObservableList<Node> children = analogChannelBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            if (analogChannel.getChannel() < Integer.parseInt(((Text) box.lookup("#text")).getText())) {
                break;
            }
        }
        addToParent(hBox, children, addIndex);
    }

    public void addDigitalInput(final DigitalInput digitalInput) {
        digitalInputMap.put(digitalInput, false);
        final CheckBox checkBox = new CheckBox(digitalInput.getChannel() + "");
        checkBox.setId("checkBox");
        checkBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                digitalInputMap.put(digitalInput, checkBox.isSelected());
            }
        });
        final ObservableList<Node> children = digitalChannelBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof CheckBox)) {
                continue;
            }
            CheckBox compareBox = (CheckBox) children.get(addIndex);
            if (digitalInput.getChannel() < Integer.parseInt(compareBox.getText())) {
                break;
            }
        }
        addToParent(checkBox, children, addIndex);
    }

    public void addAxisCamera(AxisCamera axisCamera) {

    }


    public void addEncoder(final Encoder encoder) {
        encoderValues.put(encoder, 0);

        final HBox hBox = new HBox(10);
        Text text = new Text(encoder.getAChannel() + "," + encoder.getBChannel());
        text.setId("text");
        hBox.getChildren().add(text);
        TextField textField = new TextField();
        textField.setText("0");
        setTextFieldNumeric(textField);
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                try {
                    encoderValues.put(encoder, Integer.parseInt(s2));
                } catch (Exception ignored) {
                }
            }
        });
        hBox.getChildren().add(textField);
        final ObservableList<Node> children = encoderChannelBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            String textText = ((Text) box.lookup("#text")).getText();
            if (encoder.getAChannel() < Integer.parseInt(textText.substring(0, textText.indexOf(',')))) {
                break;
            }
        }
        addToParent(hBox, children, addIndex);
    }

    public void addSpeedController(final SafePWM safePWM) {
        final HBox hBox = new HBox(10);
        Text text = new Text(safePWM.getChannel() + "");
        text.setId("text");
        hBox.getChildren().add(text);
        Text value = new Text();
        value.setId("motorValue");
        value.setText("");
        speedControllerMap.put(safePWM, value);
        setTextFieldNumeric(value);
        hBox.getChildren().add(value);
        final ObservableList<Node> children = speedControllerBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            if (safePWM.getChannel() < Integer.parseInt(((Text) box.lookup("#text")).getText())) {
                break;
            }
        }
        addToParent(hBox, children, addIndex);
    }

    private void setTextFieldNumeric(final Text text) {
        text.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                try {
                    if (newValue.equals(""))
                        return;
                    Double.parseDouble(newValue);
                    if (newValue.endsWith("f") || newValue.endsWith("d")) {
                        text.setText(newValue.substring(0, newValue.length() - 1));
                    }
                } catch (Exception e) {
                    text.setText(oldValue);
                }
            }
        });
    }

    public void addServo(Servo servo) {
        final HBox hBox = new HBox(10);
        Text text = new Text(servo.getChannel() + "");
        text.setId("text");
        hBox.getChildren().add(text);
        Text value = new Text();
        value.setText("");
        servosMap.put(servo, value);
        setTextFieldNumeric(value);
        hBox.getChildren().add(value);
        final ObservableList<Node> children = servosBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            if (servo.getChannel() < Integer.parseInt(((Text) box.lookup("#text")).getText())) {
                break;
            }
        }
        addToParent(hBox, children, addIndex);
    }

    public void addRelay(Relay relay) {
        final HBox hBox = new HBox(10);
        Text text = new Text(relay.getChannel() + "");
        text.setId("text");
        hBox.getChildren().add(text);
        Text value = new Text();
        value.setText("");
        relaysMap.put(relay, value);
        setTextFieldNumeric(value);
        hBox.getChildren().add(value);
        final ObservableList<Node> children = relaysBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            if (relay.getChannel() < Integer.parseInt(((Text) box.lookup("#text")).getText())) {
                break;
            }
        }
        addToParent(hBox, children, addIndex);

    }

    private static void setTextFieldNumeric(final TextField text) {
        text.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                try {
                    if (newValue.equals(""))
                        return;
                    Double.parseDouble(newValue);
                    if (newValue.endsWith("f") || newValue.endsWith("d")) {
                        text.setText(newValue.substring(0, newValue.length() - 1));
                    }
                } catch (Exception e) {
                    text.setText(oldValue);
                }
            }
        });
    }

    private void addToParent(final Node node, final ObservableList<Node> children, int addIndex) {
        final int finalAddIndex = addIndex;
        final boolean[] added = new boolean[1];

        if (Platform.isFxApplicationThread()) {
            children.add(finalAddIndex, node);

        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    children.add(finalAddIndex, node);
                    added[0] = true;
                }
            });
            while (!added[0]) try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void addSolenoid(Integer... solenoids) {
        Arrays.sort(solenoids);
        final HBox hBox = new HBox(10);
        String label = "";
        for (int solenoid : solenoids) label += solenoid + ", ";
        label = label.substring(0, label.length() - 2);
        Text text = new Text(label);
        text.setId("text");
        hBox.getChildren().add(text);
        Text value = new Text("Not set");
        int mask = 0;
        for (Integer solenoid : solenoids) {
            mask += Math.pow(2, solenoid - 1);
        }
        solenoidMap.put(mask, value);
        hBox.getChildren().add(value);
        final ObservableList<Node> children = solenoidBox.getChildren();
        int addIndex;
        for (addIndex = 0; addIndex < children.size(); addIndex++) {
            if (!(children.get(addIndex) instanceof HBox))
                continue;
            HBox box = (HBox) children.get(addIndex);
            String textText = ((Text) box.lookup("#text")).getText();
            try {
                if (solenoids[0] < Integer.parseInt(textText.substring(0, textText.indexOf(',')))) {
                    break;
                }
            } catch (Exception e) {
                if (solenoids[0] < Integer.parseInt(textText)) {
                    break;
                }
            }
        }
        addToParent(hBox, children, addIndex);

    }

    public int getEncoderValue(Encoder encoder) {
        Integer val = encoderValues.get(encoder);
        return val != null ? val : 0;
    }


    public boolean getDigitalInput(DigitalInput digitalInput) {
        Boolean val = digitalInputMap.get(digitalInput);
        return val != null ? val : false;
    }

    public double getAnalogChannel(AnalogChannel analogChannel) {
        Double val = analogValues.get(analogChannel);
        return val != null ? val : 0;
    }

    public void setSpeedControllerSpeed(PWM pwm, double speed) {
        String s = speed + "";
        speedControllerMap.get(pwm).setText(s.substring(0, Math.min(6, s.length())));
    }

    public void setServoPosition(Servo servo, double pos) {
        String s = pos + "";
        servosMap.get(servo).setText(s.substring(0, Math.min(6, s.length())));
    }

    public void setRelayPosition(Relay relay, Relay.Value pos) {
        String s = "Unset";
        switch (pos.value) {
            case Relay.Value.kOff_val:
                s = "Off";
                break;
            case Relay.Value.kOn_val:
                s = "On";
                break;
            case Relay.Value.kForward_val:
                s = "Forward";
                break;
            case Relay.Value.kReverse_val:
                s = "Reverse";
                break;
        }
        relaysMap.get(relay).setText(s);
    }

    public void setSolenoid(int mask, int value) {
        int forwardChannel = 1;
        int count = 0;
        for (int i = 0; i < 12; i++) {
            if ((mask >> i & 1) != 0) {
                forwardChannel = i;
                count++;
            }
        }
        String text = "";
        if (count == 1)
            switch (value) {
                case 0:
                    text = "Off";
                    break;
                case 0xFF:
                    text = "Forward";
            }
        else if (value == 0)
            text = "Off";
        else if (value == Math.pow(2, forwardChannel))
            text = "Forward";
        else
            text = "Reverse";

        solenoidMap.get(mask).setText(text);
    }

    public void printDriverStationLCDLine(byte[] m_textBuffer) {
        try {
            int lineIndex = -1;
            String[] text = new String[6];

            for (int i = 2; i < m_textBuffer.length; i++) {
                if ((i - 2) % DriverStationLCD.kLineLength == 0) {
                    lineIndex++;
                    text[lineIndex] = "";
                }
                byte b = m_textBuffer[i];
                text[lineIndex] += (char) b;
            }

            parent.getChildrenUnmodifiable();
            for (int i = 0; i < 6; i++)
                ((Text) parent.lookup("#driverStationLine" + (i + 1))).setText((i + 1) + ": " + text[i]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class JavaSourceFromPath extends SimpleJavaFileObject {
        String code;

        JavaSourceFromPath(Path path) {
            super(path.toUri(), Kind.SOURCE);
            code = "";
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(Files.newInputStream(path)));
                while (in.ready())
                    code += in.readLine() + "\n";
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

}

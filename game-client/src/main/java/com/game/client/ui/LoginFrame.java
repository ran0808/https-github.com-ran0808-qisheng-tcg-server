package com.game.client.ui;

import com.game.client.handler.ProtocolHandler;
import com.game.client.service.ClientValidationService;
import io.netty.channel.ChannelFuture;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginFrame extends Application {
    private final ClientValidationService validationService = new ClientValidationService();
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button connectButton;
    private Button loginButton;
    private Button registerButton;
    private TextArea logArea;
    private ProtocolHandler protocolHandler;
    private boolean connected = false;

    private Label usernameHint;
    private Label passwordHint;
    private Label confirmPasswordHint;

    private VBox connectionPanel;
    private VBox authPanel;
    private StackPane mainContainer;

    // 注册表单的字段
    private TextField regUsernameField;
    private PasswordField regPasswordField;
    private Label regUsernameHint;
    private Label regPasswordHint;
    private Stage matchmakingDialog;
    private ProgressIndicator matchmakingProgress;
    private Label matchmakingLabel;
    private Button cancelMatchmakingButton;
    private String currentPlayerId;
    private String currentToken;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("七圣召唤 - 登录/注册");
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(550);
        // 初始化协议处理器
        protocolHandler = new ProtocolHandler(this);
        // 创建主容器
        mainContainer = new StackPane();
        mainContainer.setPadding(new Insets(20));
        // 修改为更亮的背景颜色
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #c3cfe2);");

        // 创建连接面板
        connectionPanel = createConnectionPanel();

        // 创建认证面板
        authPanel = createAuthPanel();
        authPanel.setVisible(false);

        mainContainer.getChildren().addAll(connectionPanel, authPanel);

        // 创建场景
        Scene scene = new Scene(mainContainer, 500, 550);
        primaryStage.setScene(scene);
        primaryStage.show();

        // 尝试自动连接
        attemptAutoConnect();
    }

    private VBox createConnectionPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30));
        // 修改为更亮的背景颜色
        panel.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // 标题
        Label title = new Label("七圣召唤");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#2c3e50"));

        // 服务器连接表单
        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);

        Label formTitle = new Label("服务器连接");
        formTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        formTitle.setTextFill(Color.web("#2c3e50"));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // 服务器地址
        Label hostLabel = new Label("服务器地址:");
        hostLabel.setFont(Font.font(14));
        hostLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(hostLabel, 0, 0);

        hostField = new TextField("localhost");
        hostField.setPrefWidth(200);
        hostField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(hostField, 1, 0);

        // 端口
        Label portLabel = new Label("端口:");
        portLabel.setFont(Font.font(14));
        portLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(portLabel, 0, 1);

        portField = new TextField("8888");
        portField.setPrefWidth(200);
        portField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(portField, 1, 1);

        // 连接按钮
        connectButton = new Button("连接服务器");
        connectButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 25; -fx-background-color: #3498db; -fx-text-fill: white;");
        connectButton.setOnAction(e -> connectToServer());

        HBox buttonBox = new HBox(connectButton);
        buttonBox.setAlignment(Pos.CENTER);

        form.getChildren().addAll(formTitle, grid, buttonBox);
        panel.getChildren().addAll(title, form);
        return panel;
    }

    private VBox createAuthPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30));
        // 修改为更亮的背景颜色
        panel.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // 标题
        Label title = new Label("七圣召唤");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#2c3e50"));

        // 选项卡面板
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 登录选项卡
        Tab loginTab = new Tab("登录");
        loginTab.setContent(createLoginForm());

        // 注册选项卡
        Tab registerTab = new Tab("注册");
        registerTab.setContent(createRegisterForm());

        tabPane.getTabs().addAll(loginTab, registerTab);

        panel.getChildren().addAll(title, tabPane);
        return panel;
    }

    private VBox createLoginForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // 用户名
        Label usernameLabel = new Label("用户名:");
        usernameLabel.setFont(Font.font(14));
        usernameLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(usernameLabel, 0, 0);

        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefWidth(200);
        usernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(usernameField, 1, 0);

        usernameHint = new Label();
        usernameHint.setStyle("-fx-font-size: 11px;");
        grid.add(usernameHint, 1, 1);

        // 密码
        Label passwordLabel = new Label("密码:");
        passwordLabel.setFont(Font.font(14));
        passwordLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(passwordLabel, 0, 2);

        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefWidth(200);
        passwordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(passwordField, 1, 2);

        passwordHint = new Label();
        passwordHint.setStyle("-fx-font-size: 11px;");
        grid.add(passwordHint, 1, 3);

        // 登录按钮
        loginButton = new Button("登录");
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 30; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        loginButton.setDisable(true);
        loginButton.setOnAction(e -> login());

        HBox buttonBox = new HBox(loginButton);
        buttonBox.setAlignment(Pos.CENTER);

        // 输入验证监听
        setupValidationListeners();

        form.getChildren().addAll(grid, buttonBox);
        return form;
    }

    private VBox createRegisterForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // 用户名
        Label usernameLabel = new Label("用户名:");
        usernameLabel.setFont(Font.font(14));
        usernameLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(usernameLabel, 0, 0);

        regUsernameField = new TextField();
        regUsernameField.setPromptText("请输入用户名");
        regUsernameField.setPrefWidth(200);
        regUsernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(regUsernameField, 1, 0);

        regUsernameHint = new Label();
        regUsernameHint.setStyle("-fx-font-size: 11px;");
        grid.add(regUsernameHint, 1, 1);

        // 密码
        Label passwordLabel = new Label("密码:");
        passwordLabel.setFont(Font.font(14));
        passwordLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(passwordLabel, 0, 2);

        regPasswordField = new PasswordField();
        regPasswordField.setPromptText("请输入密码");
        regPasswordField.setPrefWidth(200);
        regPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(regPasswordField, 1, 2);

        regPasswordHint = new Label();
        regPasswordHint.setStyle("-fx-font-size: 11px;");
        grid.add(regPasswordHint, 1, 3);

        // 确认密码
        Label confirmLabel = new Label("确认密码:");
        confirmLabel.setFont(Font.font(14));
        confirmLabel.setTextFill(Color.web("#2c3e50"));
        grid.add(confirmLabel, 0, 4);

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("请再次输入密码");
        confirmPasswordField.setPrefWidth(200);
        confirmPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa;");
        grid.add(confirmPasswordField, 1, 4);

        confirmPasswordHint = new Label();
        confirmPasswordHint.setStyle("-fx-font-size: 11px;");
        grid.add(confirmPasswordHint, 1, 5);

        // 注册按钮
        registerButton = new Button("注册");
        registerButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 30; -fx-background-color: #9b59b6; -fx-text-fill: white;");
        registerButton.setDisable(true);
        registerButton.setOnAction(e -> register());

        HBox buttonBox = new HBox(registerButton);
        buttonBox.setAlignment(Pos.CENTER);

        // 输入验证监听
        setupRegisterValidationListeners();

        form.getChildren().addAll(grid, buttonBox);
        return form;
    }

    private void setupValidationListeners() {
        // 用户名校验
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            String error = validationService.validateUsername(newValue);
            if (error != null) {
                usernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: red;");
                usernameHint.setTextFill(Color.RED);
            } else {
                usernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: green;");
                usernameHint.setTextFill(Color.GREEN);
            }
            updateLoginButtonState();
        });

        // 密码校验
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            String error = validationService.validatePassword(newValue);
            if (error != null) {
                passwordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: red;");
                passwordHint.setTextFill(Color.RED);
            } else {
                passwordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: green;");
                passwordHint.setTextFill(Color.GREEN);
            }
            updateLoginButtonState();
        });
    }

    private void setupRegisterValidationListeners() {
        // 用户名校验
        regUsernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            String error = validationService.validateUsername(newValue);
            if (error != null) {
                regUsernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: red;");
                regUsernameHint.setText(error);
                regUsernameHint.setTextFill(Color.RED);
            } else {
                regUsernameField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: green;");
                regUsernameHint.setText("用户名格式正确");
                regUsernameHint.setTextFill(Color.GREEN);
            }
            updateRegisterButtonState();
        });

        // 密码校验
        regPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            String error = validationService.validatePassword(newValue);
            if (error != null) {
                regPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: red;");
                regPasswordHint.setText(error);
                regPasswordHint.setTextFill(Color.RED);
            } else {
                regPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: green;");
                regPasswordHint.setText("密码强度足够");
                regPasswordHint.setTextFill(Color.GREEN);
            }

            // 同时更新确认密码的验证
            if (!confirmPasswordField.getText().isEmpty()) {
                validatePasswordConfirmation();
            }
            updateRegisterButtonState();
        });

        // 确认密码校验
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePasswordConfirmation();
            updateRegisterButtonState();
        });
    }

    private void validatePasswordConfirmation() {
        if (!confirmPasswordField.getText().equals(regPasswordField.getText()) && confirmPasswordField != null) {
            confirmPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: red;");
            confirmPasswordHint.setText("两次输入的密码不一致");
            confirmPasswordHint.setTextFill(Color.RED);
        } else {
            confirmPasswordField.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: #f8f9fa; -fx-border-color: green;");
            confirmPasswordHint.setText("密码匹配");
            confirmPasswordHint.setTextFill(Color.GREEN);
        }
    }
    private void updateLoginButtonState() {
        String usernameError = validationService.validateUsername(usernameField.getText());
        String passwordError = validationService.validatePassword(passwordField.getText());
        loginButton.setDisable(usernameError != null || passwordError != null || !connected);
    }
    private void updateRegisterButtonState() {
        String usernameError = validationService.validateUsername(regUsernameField.getText());
        String passwordError = validationService.validatePassword(regPasswordField.getText());
        boolean passwordsMatch = confirmPasswordField.getText().equals(regPasswordField.getText());
        registerButton.setDisable(usernameError != null || passwordError != null || !passwordsMatch || !connected);
    }
    private void attemptAutoConnect() {
    }
    private void connectToServer() {
        String host = hostField.getText();
        int port;

        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            showAlert("端口号格式错误", "请输入有效的端口号");
            return;
        }
        connectButton.setText("连接中...");
        connectButton.setDisable(true);
        // 在后台线程中连接，避免阻塞UI线程
        new Thread(() -> {
            try {
                ChannelFuture future = protocolHandler.connect(host, port);
                future.sync(); // 等待连接完成

                // 连接成功后更新UI
                Platform.runLater(() -> {
                    connected = true;
                    connectionPanel.setVisible(false);
                    authPanel.setVisible(true);
                    connectButton.setText("连接服务器");
                    connectButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectButton.setText("连接服务器");
                    connectButton.setDisable(false);
                    showAlert("连接失败", "无法连接到服务器: " + e.getMessage());
                });
            }
        }).start();
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String usernameError = validationService.validateUsername(username);
        if (usernameError != null) {
            showAlert("输入错误", "用户名格式错误: " + usernameError);
            return;
        }
        String passwordError = validationService.validatePassword(password);
        if (passwordError != null) {
            showAlert("输入错误", "密码格式错误: " + passwordError);
            return;
        }
        loginButton.setText("登录中...");
        loginButton.setDisable(true);
        protocolHandler.login(username, password);
    }

    private void register() {
        String username = regUsernameField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String usernameError = validationService.validateUsername(username);
        if (usernameError != null) {
            showAlert("输入错误", "用户名格式错误: " + usernameError);
            return;
        }
        String passwordError = validationService.validatePassword(password);
        if (passwordError != null) {
            showAlert("输入错误", "密码格式错误: " + passwordError);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showAlert("输入错误", "两次输入的密码不一致");
            return;
        }
        registerButton.setText("注册中...");
        registerButton.setDisable(true);
        protocolHandler.register(username, password);
    }

    public void onLoginSuccess(String token,String playerId) {
        Platform.runLater(() -> {
            loginButton.setText("登录");
            loginButton.setDisable(false);
            this.currentToken = token;
            this.currentPlayerId = playerId;
            showMatchmakingDialog();
        });
    }

    public void onLoginFailed(String reason) {
        Platform.runLater(() -> {
            loginButton.setText("登录");
            loginButton.setDisable(false);
            showAlert("登录失败", reason);
        });
    }

    public void onRegisterSuccess(String token, String playerId) {
        Platform.runLater(() -> {
            registerButton.setText("注册");
            registerButton.setDisable(false);
            this.currentToken = token;
            this.currentPlayerId = playerId;
            showMatchmakingDialog();
        });
    }

    public void onRegisterFailed(String reason) {
        Platform.runLater(() -> {
            registerButton.setText("注册");
            registerButton.setDisable(false);
            showAlert("注册失败", reason);
        });
    }
    private void showMatchmakingDialog() {
        matchmakingDialog = new Stage();
        matchmakingDialog.initModality(Modality.APPLICATION_MODAL);
        matchmakingDialog.setTitle("七圣召唤 - 匹配对手");
        matchmakingDialog.initStyle(StageStyle.UTILITY);

        VBox dialogVBox = new VBox(20);
        dialogVBox.setAlignment(Pos.CENTER);
        dialogVBox.setPadding(new Insets(20));
        dialogVBox.setStyle("-fx-background-color: #ffffff;");

        matchmakingProgress = new ProgressIndicator();
        matchmakingProgress.setProgress(-1);

        matchmakingLabel = new Label("登录成功,正在匹配对手...");
        matchmakingLabel.setStyle("-fx-font-size: 16px;");

        cancelMatchmakingButton = new Button("取消匹配");
        cancelMatchmakingButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20;");
        cancelMatchmakingButton.setOnAction(e -> {
            protocolHandler.cancelMatchmaking(currentToken);
            matchmakingDialog.close();
        });
        dialogVBox.getChildren().addAll(matchmakingProgress, matchmakingLabel, cancelMatchmakingButton);
        Scene dialogScene = new Scene(dialogVBox, 300, 200);
        matchmakingDialog.setScene(dialogScene);
        matchmakingDialog.show();
        // 开始匹配
        protocolHandler.startMatchmaking(currentToken);
    }
    public void onMatchFound(String matchId) {
        Platform.runLater(() -> {
            matchmakingLabel.setText("匹配成功！准备开始游戏...");
            matchmakingProgress.setProgress(1); // 完成进度
            protocolHandler.readyForGame(matchId, currentToken);
        });
    }
    public void onMatchmakingFailed(String reason) {
        Platform.runLater(() -> {
            if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
                matchmakingDialog.close();
            }
            showAlert("匹配失败", reason);
        });
    }
    public void onGameStart(String gameData) {
        Platform.runLater(() -> {
            if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
                matchmakingDialog.close();
            }
            // 这里可以跳转到游戏界面
            showAlert("游戏开始", "游戏即将开始！");
            // 实际项目中，这里应该打开游戏主界面
            startGameInterface(gameData);
        });
    }
    private void startGameInterface(String gameData) {
        // 创建游戏主界面
        Stage gameStage = new Stage();
        gameStage.setTitle("七圣召唤 - 游戏进行中");
        // 这里添加游戏界面的UI组件
        VBox gameRoot = new VBox();
        gameRoot.setPadding(new Insets(20));
        gameRoot.setAlignment(Pos.CENTER);
        Label gameLabel = new Label("游戏进行中...");
        gameLabel.setStyle("-fx-font-size: 24px;");
        gameRoot.getChildren().add(gameLabel);
        Scene gameScene = new Scene(gameRoot, 800, 600);
        gameStage.setScene(gameScene);
        // 关闭登录窗口，显示游戏窗口
        Stage loginStage = (Stage) mainContainer.getScene().getWindow();
        loginStage.close();
        gameStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
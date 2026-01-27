package org.kp.chirkova;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Optional;

//Контроллер для управления умным домом

public class Controller {
    @FXML
    private Canvas canvas;

    @FXML
    private Label statusLabel;

    @FXML
    private Label energyLabel;
    
    private GraphicsContext gc;
    private SmartDevice[] devices;
    private Scenario[] scenarios;
    private AnimationTimer animationTimer;
    private long lastUpdateTime = 0;
    private long lastMouseMoveTime = 0;
    
    // Глобальное время суток
    private double globalTimeOfDay = 0.0;
    
    // Сценарии
    private Scenario activeScenario; // Текущий активный сценарий

    //Инициализация контроллера
    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        
        // Создаем устройства
        devices = new SmartDevice[] {
            new SecurityCamera("Камера Входная Дверь", 100, 250, 100.0), // Левая камера
            new Thermostat("Гостиная Термостат", 300, 150, 17.0),
            new Thermostat("Спальня Термостат", 300, 350, 17.0),
            new Light("Гостиная Свет", 500, 150, 0.0),
            new Light("Спальня Свет", 500, 350, 0.0),
            new SecurityCamera("Камера Задняя Дверь", 700, 250, 100.0) // Правая камера
        };

        // Создаем сценарии используя статический метод
        scenarios = Scenario.createKnownScenarios(devices);
        activeScenario = null; // по умолчанию нет активного сценария
        
        // Добавляем обработчик кликов на Canvas
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onCanvasClick);
        
        // Добавляем обработчик движения мыши для камер
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::onCanvasMouseMove);
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, this::onCanvasMouseExit);
        
        // Запускаем анимацию
        startAnimation();
    }

    //Обработчик движения мыши на Canvas - активирует камеры в зависимости от позиции
    private void onCanvasMouseMove(MouseEvent event) {
        lastMouseMoveTime = System.nanoTime();
        double mouseX = event.getX();
        double middleX = canvas.getWidth() / 2.0;
        boolean mouseInLeftZone = mouseX < middleX;
        
        for (SmartDevice device : devices) {
            if (device instanceof SecurityCamera camera) {
                boolean isLeftCamera = camera.getX() < middleX;
                camera.setMouseMovingInZone(isLeftCamera == mouseInLeftZone);
            }
        }
    }

     //Обработчик выхода мыши за пределы Canvas - останавливает камеры
    private void onCanvasMouseExit(MouseEvent event) {
        for (SmartDevice device : devices) {
            if (device instanceof SecurityCamera camera) {
                camera.setMouseMovingInZone(false);
            }
        }
    }

    //Обработчик клика на Canvas для ручного управления устройствами
    private void onCanvasClick(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        
        // Проверяем, кликнули ли на какое-то устройство
        for (SmartDevice device : devices) {
            double dx = mouseX - device.getX();
            double dy = mouseY - device.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Если клик в пределах устройства (радиус ~60 пикселей)
            if (distance < 60) {
                boolean wasOn = device.isOn();
                
                // Если устройство включено - выключаем его
                if (wasOn) {
                    device.setOn(false);
                    statusLabel.setText("Выключено: " + device.getName());
                } else {
                    // Если устройство выключено - включаем с запросом параметров
                    if (device instanceof Thermostat) {
                        showTemperatureInputDialog((Thermostat) device);
                    } else if (device instanceof Light) {
                        showLightInputDialog((Light) device);
                    } else {
                        // Для других устройств (камеры) просто включаем
                        device.setOn(true);
                        statusLabel.setText("Включено: " + device.getName());
                    }
                }
                
                // Сбрасываем активный сценарий при ручном управлении
                activeScenario = null;
                break;
            }
        }
    }

     //Запускает анимационный цикл
    private void startAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Обновляем устройства каждые 500мс
                if (now - lastUpdateTime >= 500_000_000) {
                    updateDevices();
                    lastUpdateTime = now;
                }
                draw();
            }
        };
        animationTimer.start();
    }

     //Обновляет состояние всех устройств
    private void updateDevices() {
        // Обновляем глобальное время суток
        globalTimeOfDay += 0.1; // 0.1 часа за обновление
        if (globalTimeOfDay > 24.0) globalTimeOfDay = 0.0;
        
        // Проверяем, было ли движение мыши недавно (в течение последних 500мс)
        long currentTime = System.nanoTime();
        if (lastMouseMoveTime > 0 && (currentTime - lastMouseMoveTime) > 500_000_000) {
            for (SmartDevice device : devices) {
                if (device instanceof SecurityCamera camera) {
                    camera.setMouseMovingInZone(false);
                }
            }
        }
        
        // Обновляем устройства
        for (SmartDevice device : devices) {
            // Передаем глобальное время в устройства света
            if (device instanceof Light) {
                ((Light) device).setGlobalTimeOfDay(globalTimeOfDay);
            }
            device.updateCurrentValue();
            device.analyzeAndAdjust();
        }
        
        // Обновляем энергопотребление сценариев и собираем статистику
        double totalEnergy = 0.0;
        double currentPower = 0.0;
        for (Scenario scenario : scenarios) {
            scenario.updateEnergyConsumption();
            totalEnergy += scenario.getTotalEnergyConsumed();
            currentPower += scenario.getCurrentPower();
        }
        
        // Обновляем информацию об энергопотреблении
        energyLabel.setText(String.format("Общая энергия: %.2f кВт*ч | Текущая мощность: %.2f кВт*ч",
            totalEnergy, currentPower / 1000.0));
    }

     //Отрисовывает все устройства на Canvas
    private void draw() {
        // Очищаем canvas
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Рисуем заголовок
        gc.setFill(Color.BLUE);
        gc.setFont(new Font("Arial", 18));
        gc.fillText("Система управления умным домом", 10, 25);
        // Рисуем время суток
        drawTimeOfDay();
        // Рисуем каждое устройство
        for (SmartDevice device : devices) {
            drawDevice(device);
        }
        // Рисуем информацию о сценариях
        drawScenariosInfo();
    }

     //Отрисовывает одно устройство
    private void drawDevice(SmartDevice device) {
        double x = device.getX();
        double y = device.getY();
        
        // Определяем, включено ли устройство и активно ли оно
        boolean isOn = device.isOn();
        boolean active = device.getPowerConsumption() > 0; // Активно, если потребляет энергию
        
        // Рисуем фон устройства (круг)
        Color bgColor = isOn ? Color.WHITE : Color.LIGHTGRAY;
        gc.setFill(bgColor);
        gc.fillOval(x - 50, y - 50, 100, 100);
        
        // Рамка устройства
        gc.setStroke(isOn ? Color.BLACK : Color.GRAY);
        gc.setLineWidth(isOn ? 3 : 2);
        gc.strokeOval(x - 50, y - 50, 100, 100);
        
        // Рисуем иконку устройства в зависимости от типа
        if (device instanceof Thermostat) {
            drawThermostatIcon(x, y, active);
        } else if (device instanceof Light light) {
            drawLightIcon(x, y, light.getCurrentValue() / 100.0, active);
        } else if (device instanceof SecurityCamera) {
            drawCameraIcon(x, y, device.getCurrentValue() == 100.0);
        }
        
        // Индикатор включения/выключения
        gc.setFill(isOn ? (active ? Color.GREEN : Color.ORANGE) : Color.DARKGRAY);
        gc.fillOval(x - 55, y - 55, 15, 15);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x - 55, y - 55, 15, 15);
        
        // Текст с именем устройства
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 11));
        String[] nameParts = device.getName().split(" ");
        gc.fillText(nameParts[0], x - 30, y + 65);
        if (nameParts.length > 1) {
            gc.fillText(nameParts[1], x - 30, y + 78);
        }
        
        // Текущее значение
        String valueText = device instanceof Thermostat
            ? String.format("%.1f°C", device.getCurrentValue())
            : String.format("%.0f%%", device.getCurrentValue());
        gc.setFont(new Font("Arial", 12));
        gc.setFill(Color.BLUE);
        gc.fillText(valueText, x - 20, y - 60);
        
        // Мощность
        gc.setFill(Color.RED);
        gc.setFont(new Font("Arial", 14));
        gc.fillText(String.format("%.0fВт", device.getPowerConsumption()), x - 20, y + 95);
    }

     //Рисует иконку термостата
    private void drawThermostatIcon(double x, double y, boolean active) {
        // Корпус термостата
        gc.setFill(active ? Color.RED : Color.LIGHTBLUE);
        gc.fillRoundRect(x - 25, y - 20, 50, 40, 10, 10);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x - 25, y - 20, 50, 40, 10, 10);
        
        // Дисплей с температурой
        gc.setFill(Color.WHITE);
        gc.fillRect(x - 20, y - 15, 40, 15);
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 10));
        gc.fillText("TEMP", x - 15, y - 3);
    }

     //Рисует иконку лампы
    private void drawLightIcon(double x, double y, double brightness, boolean active) {
        // Лампа (круг)
        Color lampColor = Color.color(1.0, 1.0, 0.3, brightness);
        gc.setFill(lampColor);
        gc.fillOval(x - 20, y - 20, 40, 40);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(x - 20, y - 20, 40, 40);
        
        // Цоколь лампы
        gc.setFill(Color.GRAY);
        gc.fillRect(x - 10, y + 15, 20, 10);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x - 10, y + 15, 20, 10);
    }

     //Рисует иконку камеры
    private void drawCameraIcon(double x, double y, boolean recording) {
        // Корпус камеры
        gc.setFill(recording ? Color.ORANGE : Color.DARKGRAY);
        gc.fillRect(x - 25, y - 15, 50, 30);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(x - 25, y - 15, 50, 30);
        
        // Объектив камеры
        gc.setFill(Color.BLACK);
        gc.fillOval(x - 15, y - 10, 30, 20);
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(x - 10, y - 5, 20, 10);

    }

     //Отрисовывает информацию о сценариях и текущем потреблении
    private void drawScenariosInfo() {
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 12));
        gc.fillText("Активный сценарий:", 10, canvas.getHeight() - 70);

        if (activeScenario != null) {
            gc.setFill(Color.BLUE);
            gc.fillText(activeScenario.getName(), 10, canvas.getHeight() - 55);
        } else {
            gc.setFill(Color.GRAY);
            gc.fillText("Нет активного сценария", 10, canvas.getHeight() - 55);
        }
    }

    //Отрисовывает время суток на экране
    private void drawTimeOfDay() {
        int hours = (int) globalTimeOfDay;
        int minutes = (int) ((globalTimeOfDay - hours) * 60);
        
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 14));
        String timeText = String.format("Время суток: %02d:%02d", hours, minutes);
        gc.fillText(timeText, canvas.getWidth() - 200, 25);
        
        // Определяем период суток
        String period;
        if (globalTimeOfDay >= 6.0 && globalTimeOfDay < 12.0) {
            period = "Утро";
        } else if (globalTimeOfDay >= 12.0 && globalTimeOfDay < 18.0) {
            period = "День";
        } else if (globalTimeOfDay >= 18.0 && globalTimeOfDay < 22.0) {
            period = "Вечер";
        } else {
            period = "Ночь";
        }
        gc.setFont(new Font("Arial", 12));
        gc.fillText("Период: " + period, canvas.getWidth() - 200, 45);
    }

     //Показывает диалог ввода температуры и активирует термостат
    private void showTemperatureInputDialog(Thermostat thermostat) {
        try {
            TextInputDialog dialog = new TextInputDialog(String.format("%.1f", thermostat.getTargetValue()));
            dialog.setTitle("Настройка температуры");
            dialog.setHeaderText("Введите желаемую температуру");
            dialog.setContentText("Температура (макс. 30°C):");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                try {
                    String input = result.get().trim().replace(",", ".");
                    double value = Double.parseDouble(input);
                    double temperature = Math.max(0, Math.min(value, 30.0));
                    thermostat.setTargetValue(temperature);
                    thermostat.setOn(true);
                    thermostat.analyzeAndAdjust();
                    statusLabel.setText("Включено: " + thermostat.getName() + " - " +
                            String.format("%.1f°C", thermostat.getTargetValue()));
                } catch (NumberFormatException e) {
                    statusLabel.setText("Ошибка: неверный формат температуры");
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка при открытии диалога температуры");
        }
    }

     //Показывает диалог ввода уровня освещения и активирует свет
    private void showLightInputDialog(Light light) {
        TextInputDialog dialog = new TextInputDialog(String.format("%.0f", light.getTargetValue()));
        dialog.setTitle("Настройка освещения");
        dialog.setHeaderText("Введите желаемый уровень освещения");
        dialog.setContentText("Уровень освещения (макс. 100%):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                String input = result.get().trim().replace(",", ".");
                double value = Double.parseDouble(input);
                double lightLevel = Math.max(0, Math.min(value, 100.0));
                light.setTargetValue(lightLevel);
                light.setOn(true);
                light.analyzeAndAdjust();
                statusLabel.setText("Включено: " + light.getName() + " - " + String.format("%.0f%%", light.getTargetValue()));
            } catch (NumberFormatException e) {
                statusLabel.setText("Ошибка: неверный формат уровня освещения");
            }
        }
    }

    @FXML
    protected void onNightScenarioClick() {
        activeScenario = scenarios[0];
        activeScenario.activate();
        statusLabel.setText("Сценарий Ночь Активен");
    }

    @FXML
    protected void onDayScenarioClick() {
        activeScenario = scenarios[1];
        activeScenario.activate();
        statusLabel.setText("Сценарий День Активен");
    }

    //Сбрасывает все устройства
    @FXML
    protected void onResetClick() {
        // Выключаем все устройства
        for (SmartDevice device : devices) {
            device.setOn(false);
        }
        activeScenario = null;
        statusLabel.setText("Система Сброшена");
    }
}
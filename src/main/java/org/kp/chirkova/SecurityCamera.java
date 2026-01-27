package org.kp.chirkova;

//Камера безопасности - отслеживает движение и записывает видео
public class SecurityCamera extends SmartDevice {
    // Флаг движения мыши в зоне этой камеры (устанавливается из Controller)
    private boolean mouseMovingInZone = false;

    public SecurityCamera(String name, double x, double y, double targetSensitivity) {
        super(name, x, y, targetSensitivity);
        setCurrentValue(0.0); // 0 = ожидание, 100 = запись
    }

    @Override
    protected void adjustValue() {
        // Камера работает только в двух состояниях (0 или 100), переключение в updateCurrentValue()
        // Метод пустой, так как логика переключения в updateCurrentValue()
    }

    @Override
    protected double getThreshold() {
        return targetValue; // Порог чувствительности
    }

    @Override
    protected double getMaxPower() {
        return getCurrentValue() == 100.0 ? 50.0 : 10.0; // 50 Вт при записи, 10 Вт в ожидании
    }

    @Override
    protected void updateCurrentValue() {
        // Если устройство выключено, сбрасываем значение в 0
        if (!isOn) {
            setCurrentValue(0.0);
            return;
        }
        // Если мышь двигается в зоне этой камеры - записываем, если нет - останавливаем запись
        if (mouseMovingInZone) {
            setCurrentValue(100.0); // Режим записи
        } else {
            setCurrentValue(0.0); // Режим ожидания
        }
    }
    
    @Override
    public void analyzeAndAdjust() {
        if (!isOn) {
            powerConsumption = 0.0;
            return;
        }
        // Учитываем энергопотребление включенной камеры
        powerConsumption = getMaxPower();
    }
    //Устанавливает флаг движения мыши в зоне этой камеры (вызывается из Controller)
    public void setMouseMovingInZone(boolean moving) {
        mouseMovingInZone = moving;
    }
    
}

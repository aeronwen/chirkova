package org.kp.chirkova;

//Базовый класс для всех умных устройств в системе управления домом

public abstract class SmartDevice {
    // Статус устройства (включено/выключено)
    protected boolean isOn;
    
    // Текущие показания устройства
    protected double currentValue;
    
    // Целевые параметры
    protected double targetValue;
    
    // Потребляемая мощность
    protected double powerConsumption;
    
    // Позиция для отрисовки на Canvas
    protected double x, y;
    
    // Имя устройства
    protected String name;

    public SmartDevice(String name, double x, double y, double targetValue) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.isOn = false; // По умолчанию устройство выключено
        this.currentValue = 0.0;
        this.targetValue = targetValue;
        this.powerConsumption = 0.0;
    }

    /**
     * Ключевой метод: анализирует текущие показания, сравнивает с целевыми
     * и корректирует статус работы (через powerConsumption) и мощность устройства,
     * рассчитывая энергопотребление
     */
    public void analyzeAndAdjust() {
        // Если устройство не включено (isOn=false), не работает
        if (!isOn) {
            powerConsumption = 0.0; // Статус работы: выключено
            return;
        }

        // Специальная логика для термостата
        if (this instanceof Thermostat) {
            double difference = getCurrentValue() - targetValue;
            // Включаем только если температура упала на 0.5 градуса ниже целевой
            if (difference < -getThreshold()) {
                powerConsumption = getMaxPower(); // Работает (нагревает)
                adjustValue();
            } else if (difference > 0) {
                // Если температура выше целевой, выключаем нагрев, но позволяем охлаждаться
                powerConsumption = 0.0; // Статус: не работает (охлаждается естественно)
                // Вызываем adjustValue() чтобы температура снижалась к целевой
                adjustValue();
            } else if (Math.abs(difference) < 0.01) {
                // Если температура точно равна целевой, выключаем
                powerConsumption = 0.0;
            } else {
                // Температура между целевой и целевой-0.5: продолжаем работу если уже работали
                if (powerConsumption > 0) {
                    powerConsumption = getMaxPower();
                    adjustValue();
                }
            }
        } else if (this instanceof Light) {
            // Логика для света
            double difference = Math.abs(getCurrentValue() - targetValue);
            if (difference > getThreshold()) {
                powerConsumption = getMaxPower();
                adjustValue();
            } else {
                // Достигли целевого значения - поддерживаем уровень
                powerConsumption = getMaxPower();
            }
        } else {
            // Логика для других устройств (камеры и т.д.)
            double difference = Math.abs(getCurrentValue() - targetValue);
            if (difference > getThreshold()) {
                powerConsumption = getMaxPower();
                adjustValue();
            } else {
                powerConsumption = 0.0; // Достигнута цель
            }
        }
    }

     //Корректирует текущее значение в сторону целевого
    protected abstract void adjustValue();

     //Возвращает порог, при котором устройство должно включиться
    protected abstract double getThreshold();

     //Возвращает максимальную мощность устройства
    protected abstract double getMaxPower();

     //Обновляет текущее значение (имитация внешних факторов)
    protected abstract void updateCurrentValue();

    // Геттеры и сеттеры
    public boolean isOn() {
        return isOn;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    protected void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public double getPowerConsumption() {
        return powerConsumption;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getName() {
        return name;
    }

     //Включает или выключает устройство
    public void setOn(boolean on) {
        this.isOn = on;
        if (!on) {
            powerConsumption = 0.0;
        }
    }
}

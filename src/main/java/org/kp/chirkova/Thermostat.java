package org.kp.chirkova;

//Термостат управляет температурой в помещении
public class Thermostat extends SmartDevice {
    // Скорость изменения температуры (градусов в секунду)
    private static final double TEMPERATURE_CHANGE_RATE = 0.05;
    
    // Естественное охлаждение/нагрев (имитация внешних факторов)
    private static final double NATURAL_CHANGE_RATE = 0.01;
    
    // Базовая температура (к которой стремится без управления)
    private double baseTemperature;

    public Thermostat(String name, double x, double y, double targetTemperature) {
        super(name, x, y, targetTemperature);
        setCurrentValue(17.0);
        this.baseTemperature = 17.0;
    }

    @Override
    protected void adjustValue() {
        // Направляем температуру к целевому значению (вызывается только когда isOn=true)
        if (getCurrentValue() < targetValue) {
            setCurrentValue(getCurrentValue() + TEMPERATURE_CHANGE_RATE);
        } else if (getCurrentValue() > targetValue) {
            setCurrentValue(getCurrentValue() - TEMPERATURE_CHANGE_RATE);
        }
    }

    @Override
    protected double getThreshold() {
        return 0.5; // Порог 0.5 градуса (термостат включается только если упало на 0,5°C)
    }

    @Override
    protected double getMaxPower() {
        return 500.0; // Максимальная мощность 500 Вт
    }

    @Override
    protected void updateCurrentValue() {
        if (!isOn) {
            // Температура стремится к базовой 17°C
            if (getCurrentValue() > baseTemperature) {
                setCurrentValue(getCurrentValue() - NATURAL_CHANGE_RATE);
            } else if (getCurrentValue() < baseTemperature) {
                setCurrentValue(getCurrentValue() + NATURAL_CHANGE_RATE);
            }
        } else {
            // Если температура выше чем (цель - 0.5), то охлаждаем
            if (getCurrentValue() > targetValue - getThreshold()) {
                setCurrentValue(getCurrentValue() - NATURAL_CHANGE_RATE);
            }
        }
    }
}

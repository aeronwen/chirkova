package org.kp.chirkova;


//Умное освещение - управляет уровнем света

public class Light extends SmartDevice {
    // Скорость изменения уровня света
    private static final double LIGHT_CHANGE_RATE = 2.0;
    
    // Глобальное время суток (устанавливается из контроллера)
    private double globalTimeOfDay = 0.0;

    public Light(String name, double x, double y, double targetLightLevel) {
        super(name, x, y, targetLightLevel);
        setCurrentValue(0.0);
    }

     //Устанавливает глобальное время суток (вызывается из контроллера)
    public void setGlobalTimeOfDay(double timeOfDay) {
        this.globalTimeOfDay = timeOfDay;
    }

    @Override
    protected void adjustValue() {
        // Направляем уровень света к целевому значению (вызывается только когда isOn=true)
        if (getCurrentValue() < targetValue) {
            setCurrentValue(Math.min(getCurrentValue() + LIGHT_CHANGE_RATE, targetValue));
        } else if (getCurrentValue() > targetValue) {
            setCurrentValue(Math.max(getCurrentValue() - LIGHT_CHANGE_RATE, targetValue));
        }
    }

    @Override
    protected double getThreshold() {
        return 0.1; // Порог для точного достижения целевого значения
    }

    @Override
    protected double getMaxPower() {
        // Потребление пропорционально уровню освещенности (вызывается только когда isOn=true)
        return 100.0 * (getCurrentValue() / 100.0);
    }

    @Override
    protected void updateCurrentValue() {
        if (!isOn) {
            setCurrentValue(calculateDayLight());
            return;
        }
        
        // Если устройство включено, но еще не достигло целевого значения
        double difference = Math.abs(getCurrentValue() - targetValue);
        if (difference > getThreshold()) {
            double dayLight = calculateDayLight();
            if (dayLight > getCurrentValue() && dayLight <= targetValue) {
                setCurrentValue(dayLight);
            }
        }
    }
    
    //Вычисляет естественную освещенность в зависимости от времени суток
    private double calculateDayLight() {
        if (globalTimeOfDay >= 6.0 && globalTimeOfDay <= 18.0) {
            return 30.0 + 20.0 * Math.sin((globalTimeOfDay - 6.0) / 12.0 * Math.PI);
        }
        return 0.0;
    }
}
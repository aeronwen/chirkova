package org.kp.chirkova;

//Сценарий - группа устройств, которые работают вместе

public class Scenario {
    private String name;
    private SmartDevice[] devices;
    
    // Общее энергопотребление устройств сценария (накопленное)
    private double totalEnergyConsumed;
    
    // Параметры сценария
    private double thermostatTarget;
    private double lightTarget;
    
    public Scenario(String name, double thermostatTarget, double lightTarget, SmartDevice[] devices) {
        this.name = name;
        this.devices = devices;
        this.totalEnergyConsumed = 0.0;
        this.thermostatTarget = thermostatTarget;
        this.lightTarget = lightTarget;
    }

     //Активирует сценарий - применяет настройки ко всем устройствам
    public void activate() {
        // Включаем устройства сценария и устанавливаем целевые значения
        for (SmartDevice device : devices) {
            device.setOn(true);
            if (device instanceof Thermostat) {
                device.setTargetValue(thermostatTarget);
            } else if (device instanceof Light) {
                device.setTargetValue(lightTarget);
            }
        }
    }
    
    //Рассчитывает и обновляет общее энергопотребление устройств сценария
    public void updateEnergyConsumption() {
        for (SmartDevice device : devices) {
            if (device.getPowerConsumption() > 0) {
                // Обновляем общее энергопотребление (конвертируем в кВт*ч за секунду)
                totalEnergyConsumed += device.getPowerConsumption() / 3600.0;
            }
        }
    }

     //Возвращает общее энергопотребление устройств сценария
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }

    //Возвращает текущую суммарную мощность устройств сценария
    public double getCurrentPower() {
        double currentPower = 0.0;
        for (SmartDevice device : devices) {
            currentPower += device.getPowerConsumption();
        }
        return currentPower;
    }
    
    public String getName() {
        return name;
    }

    //Создает и возвращает массив известных сценариев с предустановленными параметрами
    public static Scenario[] createKnownScenarios(SmartDevice[] allDevices) {
        return new Scenario[] {
            new Scenario("Ночь", 19.0, 25.0, allDevices),
            new Scenario("День", 22.0, 75.0, allDevices)
        };
    }
}

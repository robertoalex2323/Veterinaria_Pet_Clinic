package com.veterinariapetCcinic.veterinaria_pet_clinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada para las propiedades personalizadas del aplicativo.
 * Esto resuelve las advertencias de "Unknown property" en el IDE y proporciona tipado fuerte.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String cancellationPhone;
    private final Business business = new Business();

    public String getCancellationPhone() {
        return cancellationPhone;
    }

    public void setCancellationPhone(String cancellationPhone) {
        this.cancellationPhone = cancellationPhone;
    }

    public Business getBusiness() {
        return business;
    }

    public static class Business {
        private String startTime = "08:00";
        private String endTime = "22:00";
        private int defaultSlotDuration = 30;

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public int getDefaultSlotDuration() {
            return defaultSlotDuration;
        }

        public void setDefaultSlotDuration(int defaultSlotDuration) {
            this.defaultSlotDuration = defaultSlotDuration;
        }
    }
}
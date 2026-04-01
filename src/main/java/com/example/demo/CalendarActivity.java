package com.example.demo;

import java.time.ZonedDateTime;

public class CalendarActivity {
        private final  ZonedDateTime date;
        private final String clientName;
        private final Integer serviceNo;

        public CalendarActivity(ZonedDateTime date, String clientName, Integer serviceNo) {
            this.date = date;
            this.clientName = clientName;
            this.serviceNo = serviceNo;
        }

        public ZonedDateTime getDate() {
            return date;
        }
        public String getClientName() {
                return clientName;
        }
        @Override
        public String toString() {
            return "CalenderActivity{" +
                    "date=" + date +
                    ", clientName='" + clientName + '\'' +
                    ", serviceNo=" + serviceNo +
                    '}';
        }
    }


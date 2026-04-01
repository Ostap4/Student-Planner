package com.example.demo;

public class Exam {
        private String subject;
        private String date;
        private String time;
        private String location;

        public Exam(String subject, String date, String time, String location) {
            this.subject = subject;
            this.date = date;
            this.time = time;
            this.location = location;
        }

        // Gettery i settery
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }



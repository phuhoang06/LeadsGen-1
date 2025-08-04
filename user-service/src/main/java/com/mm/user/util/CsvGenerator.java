package com.mm.user.util;

import com.github.javafaker.Faker;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class CsvGenerator {
    
    public static void main(String[] args) {
        String csvFile = "users_10000.csv";
        int numUsers = 10000;
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            // Write CSV header
            String[] header = {"username", "name", "email", "password"};
            writer.writeNext(header);
            
            Faker faker = new Faker(new Locale("vi"));
            
            for (int i = 1; i <= numUsers; i++) {
                String username = "user" + i;
                String name = faker.name().fullName();
                String email = "user" + i + "@example.com";
                String password = "P@ssw0rd";
                
                String[] data = {username, name, email, password};
                writer.writeNext(data);
                
                if (i % 1000 == 0) {
                    System.out.println("Generated " + i + " users...");
                }
            }
            
            System.out.println("CSV file generated successfully with " + numUsers + " users: " + csvFile);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

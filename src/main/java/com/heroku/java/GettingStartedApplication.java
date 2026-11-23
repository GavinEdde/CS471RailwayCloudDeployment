package com.heroku.java;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

@SpringBootApplication
@Controller
public class GettingStartedApplication {
    private final DataSource dataSource;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Autowired
    public GettingStartedApplication(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/database")
    String database(Map<String, Object> model) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {

            // Create table if it doesn't already exist
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string " +
                                "(tick timestamp, random_string varchar(50))"
                );
            }

            // Insert a timestamp and randomly generated string
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO table_timestamp_and_random_string VALUES (now(), ?)")) {

                statement.setString(1, getRandomString());
                statement.executeUpdate();
            }

            // Retrieve both columns from the new table
            final var output = new ArrayList<String>();

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT tick, random_string FROM table_timestamp_and_random_string")) {

                while (resultSet.next()) {
                    output.add(
                            "Read from DB: " +
                                    resultSet.getTimestamp("tick") +
                                    " " +
                                    resultSet.getString("random_string")
                    );
                }
            }

            model.put("records", output);
            return "database";
        }
    }


    @GetMapping("/dbinput")
    String dbinput() {
        return "dbinput";
    }

    @PostMapping("/dbinput")
    String submitDatabaseInput(@RequestParam("randomString") String randomString) throws SQLException {
        randomString = randomString.trim();

        if (randomString.isEmpty() || randomString.length() > 50) {
            throw new IllegalArgumentException(
                    "Input must contain between 1 and 50 characters."
            );
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO table_timestamp_and_random_string " +
                             "(tick, random_string) VALUES (now(), ?)")) {

            statement.setString(1, randomString);
            statement.executeUpdate();
        }

        return "redirect:/database";
    }


    private String getRandomString() {
        final int length = 20;
        final StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }

        return result.toString();
    }

    public static void main(String[] args) {
        SpringApplication.run(GettingStartedApplication.class, args);
    }
}

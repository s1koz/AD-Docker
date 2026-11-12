
package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class Main {

    public static void main(String[] args) {

        Properties p = new Properties();

        try {
            p.load(Files.newBufferedReader(
                Path.of("src", "main", "resources", "conn.properties")
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }

        HikariConfig hkcfg = new HikariConfig();

        hkcfg.setJdbcUrl(p.getProperty("DB_URL"));
        hkcfg.setUsername(p.getProperty("DB_USER"));
        hkcfg.setPassword(p.getProperty("DB_PASSWORD"));
        hkcfg.setMaximumPoolSize(
            Integer.parseInt(p.getProperty("DB_MAX_POOL", "5"))
        );

        try (HikariDataSource hkds = new HikariDataSource(hkcfg)) {

            try (Connection cnn = hkds.getConnection()) {

                System.out.println("Connectat!");

                for (String resultat : readAllStatements(cnn)) {
                    System.out.println("moviment amb id: " + resultat);
                }

            } catch (SQLException e) {
                System.out.println("No connectat!");
                e.printStackTrace();
            }

        } catch (PoolInitializationException e) {
            System.out.println("No s'ha pogut connectar a la base de dades: " + e.getMessage());
        }
    }

    public static List<String> readAllStatements(Connection conn) throws SQLException {

        List<String> resultatQuery = new ArrayList<String>();

        String query = "SELECT * FROM statements";

        PreparedStatement ps = conn.prepareStatement(query);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String resultat = String.join(";",
                    rs.getString("id_statement"),
                    rs.getString("tipus_moviment"),
                    rs.getString("import"),
                    rs.getString("divisa")
                );

                resultatQuery.add(resultat);
            }
        }

        ps.close();

        return resultatQuery;
    }
}

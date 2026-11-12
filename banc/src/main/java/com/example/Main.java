
package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

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
            return;
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

                // Exercici anterior: llegir tots els statements
                for (String resultat : readAllStatements(cnn)) {
                    System.out.println("moviment amb id: " + resultat);
                }

                // Exercici 1: statements amb import mínim
                readStatementsByImport(cnn, 100.0);

                // Exercici 2: inserir un client
                Scanner sc = new Scanner(System.in);

                String[] dadesClient = new String[3];

                System.out.println("Nom:");
                dadesClient[0] = sc.nextLine();

                System.out.println("Cognoms:");
                dadesClient[1] = sc.nextLine();

                System.out.println("ID fiscal:");
                dadesClient[2] = sc.nextLine();

                insertClient(cnn, dadesClient);

            } catch (SQLException e) {
                System.out.println("No connectat!");
                e.printStackTrace();
            }

        } catch (PoolInitializationException e) {
            System.out.println("No s'ha pogut connectar a la base de dades: " + e.getMessage());
        }
    }

    // Llegir tots els statements
    public static List<String> readAllStatements(Connection conn) throws SQLException {

        List<String> resultatQuery = new ArrayList<String>();

        String query = "SELECT * FROM statements";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

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

        return resultatQuery;
    }

    // Llegir statements amb un import determinat
    public static void readStatementsByImport(Connection conn, double quantitat) throws SQLException {

        String query = "SELECT * FROM statements WHERE import >= ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setDouble(1, quantitat);

            try (ResultSet rs = ps.executeQuery()) {

                ResultSetMetaData meta = rs.getMetaData();

                String header = String.join(";",
                    meta.getColumnName(1),
                    meta.getColumnName(3),
                    meta.getColumnName(8),
                    meta.getColumnName(10)
                );

                System.out.println(header);

                while (rs.next()) {

                    String resultat = String.join(";",
                        rs.getString(1),
                        rs.getString(3),
                        rs.getString(8),
                        rs.getString(10)
                    );

                    System.out.println(resultat);
                }
            }
        }
    }

    // Comprovar si el client existeix
    public static boolean clientExists(Connection conn, String idFiscal) throws SQLException {

        String query = "SELECT * FROM clients WHERE id_fiscal = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, idFiscal);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next();
            }
        }
    }

    // Inserir un client nou
    public static void insertClient(Connection conn, String[] dades) throws SQLException {

        conn.setAutoCommit(false);

        try {

            if (clientExists(conn, dades[2])) {

                System.out.println("El client ja existeix!");

            } else {

                String query = "INSERT INTO clients (nom,cognoms,id_fiscal) VALUES (?,?,?)";

                try (PreparedStatement ps = conn.prepareStatement(query)) {

                    ps.setString(1, dades[0]);
                    ps.setString(2, dades[1]);
                    ps.setString(3, dades[2]);

                    int resultat = ps.executeUpdate();

                    if (resultat > 0) {
                        conn.commit();
                        System.out.println("Client inserit correctament!");
                    }
                }
            }

        } catch (SQLException e) {

            conn.rollback();
            throw e;

        } finally {

            conn.setAutoCommit(true);
        }
    }
}

package org.example;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final SQLiteDataSource dataSource = new SQLiteDataSource();


    public static void main(String[] args) {
        createDB();
        int intInput = 1;
        while(intInput != 0) {
            System.out.println("1. Create an account\n" +
                    "2. Log into account\n" +
                    "0. Exit");
            intInput = scanner.nextInt();
            switch (intInput) {
                case 1:
                    createAccount();
                    break;
                case 2:
                    loginToAccount();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Unknown action");
            }
        }
    }

    private static void createDB() {
        String url = "jdbc:sqlite:card.s3db";
        String sql = "CREATE TABLE IF NOT EXISTS card(id INTEGER PRIMARY KEY, number TEXT, pin TEXT, balance INTEGER DEFAULT 0)";

        connectDB(url, sql);
    }

    private static void insertToDB(String cardNumber, String cardPIN) {
        String url = "jdbc:sqlite:card.s3db";
        String sql = "INSERT INTO card (number, pin) VALUES " + String.format("('%s', '%s')", cardNumber, cardPIN);

        connectDB(url, sql);
    }

    private static void connectDB(String url, String sql) {
        dataSource.setUrl(url);
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAccount() {
        Random random = new Random();

        String cardPrefix = "400000";
        String cardSuffix = String.format("%09d", random.nextInt(1000000000));
        String cardNumber = cardPrefix.concat(cardSuffix).replaceAll("\\s+","");
        String checkDigit = lastDigit(cardNumber);
        cardNumber = cardPrefix.concat(cardSuffix).concat(checkDigit).replaceAll("\\s+","");

        System.out.println();
        System.out.println("Your card has been created\n" +
                "Your card number:");
        System.out.println(cardNumber);

        System.out.println("Your card PIN:");
        String pin = String.format("%04d", random.nextInt(10000));
        System.out.println(pin);
        System.out.println();

        insertToDB(cardNumber, pin);
    }

    private static String lastDigit(String card) {
        String digit;
        /* convert to array of int for simplicity */
        int[] digits = new int[card.length()];
        for (int i = 0; i < card.length(); i++) {
            digits[i] = Character.getNumericValue(card.charAt(i));
        }

        /* double every other starting from right - jumping from 2 in 2 */
        for (int i = digits.length - 1; i >= 0; i -= 2)	{
            digits[i] += digits[i];

            /* taking the sum of digits grater than 10 - simple trick by substract 9 */
            if (digits[i] >= 10) {
                digits[i] = digits[i] - 9;
            }
        }
        int sum = 0;
        for (int j : digits) {
            sum += j;
        }
        /* multiply by 9 step */
        sum = sum * 9;

        /* convert to string to be easier to take the last digit */
        digit = sum + "";
        return digit.substring(digit.length() - 1);
    }

    private static void loginToAccount() {
        System.out.println("Enter your card number: ");
        String cardNumber = scanner.next();
        System.out.println("Enter your PIN: ");
        String pin = scanner.next();

        String sql = "SELECT number, pin FROM card WHERE number = ? AND pin = ?";
        String url = "jdbc:sqlite:card.s3db";
        boolean isLogged = false;
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setString(1, cardNumber);
                statement.setString(2, pin);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        isLogged = true;
                        System.out.println("You have successfully logged in!");
                        System.out.println();
                    } else {
                        System.out.println();
                        System.out.println("Wrong card number or PIN!");
                        System.out.println();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (isLogged) {
            loggedMenu(cardNumber, pin);
        }
    }


    private static void doTransfer(String cardNumber, String pin) {
        String url = "jdbc:sqlite:card.s3db";
        String sql1 = "UPDATE card SET balance = balance - ? WHERE number = ? AND pin = ?";
        String sql2 = "UPDATE card SET balance = balance + ? WHERE number = ?";
        dataSource.setUrl(url);

        System.out.println();
        System.out.println("Transfer\n" +
                "Enter card number: ");
        String targetAccount = scanner.next();


        String targetAccountLastChar = Character.toString(targetAccount.charAt(targetAccount.length() - 1));
        String targetAccountWithoutLastChar = targetAccount.substring(0, targetAccount.length()-1);

        if (targetAccount.equals(cardNumber)) {
            System.out.println("You can't transfer money to the same account!");
            return;
        }

        if (!targetAccountLastChar.equals(lastDigit(targetAccountWithoutLastChar))) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            return;
        }

        if (!doesAccountExist(targetAccount)) {
            System.out.println("Such a card does not exist.");
            return;
        }

        System.out.println("Enter how much money you want to transfer:");
        int transferAmount = scanner.nextInt();

        if (Integer.parseInt(getBalance(cardNumber, pin)) < transferAmount) {
            System.out.println("Not enough money!");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement substractMoney = connection.prepareStatement(sql1);
                 PreparedStatement addMoney = connection.prepareStatement(sql2)) {
                substractMoney.setInt(1, transferAmount);
                substractMoney.setString(2, cardNumber);
                substractMoney.setString(3, pin);
                substractMoney.executeUpdate();

                addMoney.setInt(1, transferAmount);
                addMoney.setString(2, targetAccount);
                addMoney.executeUpdate();

                connection.commit();
                System.out.println("Success!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean doesAccountExist(String cardNumber) {
        boolean exists = false;
        String url = "jdbc:sqlite:card.s3db";
        String sql = "SELECT number FROM card WHERE number = ?";
        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardNumber);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    exists = resultSet.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exists;
    }

    private static void closeAccount(String cardNumber, String pin) {
        String url = "jdbc:sqlite:card.s3db";
        String sql = "DELETE FROM card WHERE number = ? AND pin = ?";

        System.out.println();
        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardNumber);
                preparedStatement.setString(2, pin);
                preparedStatement.executeUpdate();
                System.out.println("The account has been closed!");
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addIncome(String cardNumber, String pin) {
        String url = "jdbc:sqlite:card.s3db";
        String sql = "UPDATE card SET balance = balance + ? WHERE number = ? AND pin = ?";

        System.out.println();
        System.out.println("Enter income:");
        int accBalance = scanner.nextInt();
        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, accBalance);
                preparedStatement.setString(2, cardNumber);
                preparedStatement.setString(3, pin);
                preparedStatement.executeUpdate();
                System.out.println("Income was added!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public static String getBalance(String cardNumber, String pin) {
        String url = "jdbc:sqlite:card.s3db";
        String sql = "SELECT balance FROM card WHERE number = ? AND pin = ?";
        String result = "";
        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardNumber);
                preparedStatement.setString(2, pin);
                ResultSet resultSet = preparedStatement.executeQuery();
                result = resultSet.getString("balance");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;

    }
}
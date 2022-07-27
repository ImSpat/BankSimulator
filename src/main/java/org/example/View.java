package org.example;

import static org.example.Main.getBalance;

public class View {

    public static void loggedMenu(String cardNumber, String pin) {
        while(true) {
            System.out.println("1. Balance\n" +
                    "2. Add income\n" +
                    "3. Do transfer\n" +
                    "4. Close account\n" +
                    "5. Log out\n" +
                    "0. Exit");
            int input = scanner.nextInt();
            switch (input) {
                case 1:
                    System.out.println("Balance: " + getBalance(cardNumber, pin));
                    break;
                case 2:
                    addIncome(cardNumber, pin);
                    break;
                case 3:
                    doTransfer(cardNumber, pin);
                    break;
                case 4:
                    closeAccount(cardNumber, pin);
                    return;
                case 5:
                    System.out.println("You have successfully logged out!");
                    System.out.println();
                    return;
                case 0:
                    System.out.println("Bye!");
                    System.exit(0);
            }
        }
    }
}

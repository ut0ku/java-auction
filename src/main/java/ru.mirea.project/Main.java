package ru.mirea.project;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleApp app = new ConsoleApp(scanner);
        app.run();
    }
}

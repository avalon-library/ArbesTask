import main.billing.TelephoneBillCalculator;
import main.billing.TelephoneBillCalculatorImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        String phoneLog = Files.readString(Paths.get("src/main/resources/phoneCallsLog.csv"));

        TelephoneBillCalculator calculator = new TelephoneBillCalculatorImpl();
        System.out.println(calculator.calculate(phoneLog));
    }
}
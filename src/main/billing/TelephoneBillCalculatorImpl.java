package main.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class TelephoneBillCalculatorImpl implements TelephoneBillCalculator {

    static class PhoneCall {
        private final String number;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private BigDecimal totalCost;

        public PhoneCall(String number, LocalDateTime startTime, LocalDateTime endTime) {
            this.number = number;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getNumber() {
            return number;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final LocalTime mainTarifStart = LocalTime.of(8, 0, 0);
    private final LocalTime mainTarifEnd = LocalTime.of(16, 0, 0);

    private final BigDecimal mainTarifCost = BigDecimal.valueOf(1.0);
    private final BigDecimal offTarifCost = BigDecimal.valueOf(0.5);

    private final BigDecimal longCallsDiscount = BigDecimal.valueOf(0.2);
    private final int longCallsTreshold = 5;

    @Override
    public BigDecimal calculate(String phoneLog) {
        String[] logLines = phoneLog.split(System.lineSeparator());
        List<PhoneCall> phoneCalls = new ArrayList<>();

        // Calculate tarif/off-tariff calling times
        for (String line : logLines) {
            String[] logParts = line.split(",");
            PhoneCall phoneCall = new PhoneCall(logParts[0], LocalDateTime.parse(logParts[1], formatter), LocalDateTime.parse(logParts[2], formatter));

            long phonecallLengthInSeconds = phoneCall.getStartTime().until(phoneCall.getEndTime(), ChronoUnit.SECONDS);
            long secondsOffMainTarif = 0;

            if (phoneCall.getStartTime().toLocalTime().isBefore(mainTarifStart)) {
                secondsOffMainTarif += phoneCall.getStartTime().until(mainTarifStart.isAfter(phoneCall.getEndTime().toLocalTime()) ? phoneCall.getEndTime().toLocalTime() : mainTarifStart, ChronoUnit.SECONDS);
            }

            if (phoneCall.getEndTime().toLocalTime().isAfter(mainTarifEnd)) {
                LocalTime from = mainTarifEnd.isBefore(phoneCall.getStartTime().toLocalTime()) ? phoneCall.getStartTime().toLocalTime() : mainTarifEnd;
                secondsOffMainTarif += from.until(phoneCall.getEndTime().toLocalTime(), ChronoUnit.SECONDS);
            }

            long secondsInMainTarif = phonecallLengthInSeconds - secondsOffMainTarif;

            // Account for started minutes
            long minutesStartedInMainTarif = secondsInMainTarif % 60 == 0 ? Math.divideExact(secondsInMainTarif, 60) :  Math.divideExact(secondsInMainTarif, 60) + 1;
            long minutesStartedOffMainTarif = secondsOffMainTarif % 60 == 0 ? Math.divideExact(secondsOffMainTarif, 60) :  Math.divideExact(secondsOffMainTarif, 60) + 1;

            phoneCall.setTotalCost(countTotalCost(minutesStartedInMainTarif, minutesStartedOffMainTarif));
            phoneCalls.add(phoneCall);
        }

        // Count total cost for each called number
        Map<String, BigDecimal> totalCostsByNumbers = new TreeMap<>();
        for (PhoneCall phoneCall : phoneCalls) {
            totalCostsByNumbers.put(
                    phoneCall.getNumber(), totalCostsByNumbers.containsKey(phoneCall.getNumber()) ? totalCostsByNumbers.get(phoneCall.getNumber()).add(phoneCall.getTotalCost()) : phoneCall.getTotalCost());
        }

        // Get number with greatest total cost of calls
        String freeCallsNumber = totalCostsByNumbers.entrySet().stream()
                .max(Map.Entry.comparingByValue()) // Optional<Map.Entry<Integer, Integer>> - entry
                .map(Map.Entry::getKey)            // Optional<Integer> - key
                .orElseThrow();

        // Exlcude the free calls number from list
        phoneCalls = phoneCalls.stream().filter(n -> !n.getNumber().equals(freeCallsNumber)).toList();

        // count total cost to be returned
        BigDecimal totalCost = BigDecimal.ZERO;
        for (PhoneCall phoneCall : phoneCalls) {
            totalCost = totalCost.add(phoneCall.getTotalCost());
        }

        return totalCost;
    }

    private BigDecimal countTotalCost(long minutesStartedInMainTarif, long minutesStartedOffMainTarif) {
        BigDecimal phoneCallCost = this.mainTarifCost.multiply(BigDecimal.valueOf(minutesStartedInMainTarif)).add(this.offTarifCost.multiply(BigDecimal.valueOf(minutesStartedOffMainTarif)));

        long totalCallStartedMinutes = minutesStartedInMainTarif + minutesStartedOffMainTarif;

        // Apply discounts
        if (totalCallStartedMinutes > 5) {
            BigDecimal discount = this.longCallsDiscount.multiply(new BigDecimal(totalCallStartedMinutes - longCallsTreshold));
            phoneCallCost = phoneCallCost.subtract(discount);
        }
        return phoneCallCost;
    }
}
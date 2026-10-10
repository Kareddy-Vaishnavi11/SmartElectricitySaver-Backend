package jar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ElectricityBillService {

    private final ElectricityBillRepository billRepository;

    // Constructor injection
    public ElectricityBillService(ElectricityBillRepository billRepository) {
        this.billRepository = billRepository;
    }

    // Save and return one bill
    public ElectricityBill saveBill(ElectricityBill bill) {
        return billRepository.save(bill);
    }

    // Return all saved bills
    public List<ElectricityBill> getAllBills() {
        return billRepository.findAll();
    }

    // Return the bill if found; otherwise throw an exception
    public ElectricityBill getBillById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Electricity bill not found with id: " + id));
    }

    // Delete the bill if it exists; otherwise throw an exception
    public void deleteBill(Long id) {
        if (!billRepository.existsById(id)) {
            throw new RuntimeException("Electricity bill not found with id: " + id);
        }
        billRepository.deleteById(id);
    }

    // Compare electricity consumption between two months
    public Map<String, Object> compareConsumption(String month1, Integer year1, String month2, Integer year2) {
        // Fetch bills for both months
        ElectricityBill bill1 = billRepository.findByMonthIgnoreCaseAndYear(month1, year1)
                .orElseThrow(() -> new RuntimeException("Electricity bill not found for " + month1 + " " + year1));

        ElectricityBill bill2 = billRepository.findByMonthIgnoreCaseAndYear(month2, year2)
                .orElseThrow(() -> new RuntimeException("Electricity bill not found for " + month2 + " " + year2));

        double units1 = (bill1.getUnitsConsumed() != null) ? bill1.getUnitsConsumed() : 0.0;
        double units2 = (bill2.getUnitsConsumed() != null) ? bill2.getUnitsConsumed() : 0.0;
        double unitDifference = units2 - units1;

        double percentageChange;
        String message;

        // Handle case where previous month's consumption is zero
        if (units1 == 0.0) {
            if (units2 == 0.0) {
                percentageChange = 0.0;
                message = "Consumption stayed the same (both months are 0 units).";
            } else {
                percentageChange = 100.0;
                message = "Consumption increased from 0 units in " + month1 + " " + year1 + " to " + units2 + " units in " + month2 + " " + year2 + ".";
            }
        } else {
            percentageChange = ((units2 - units1) / units1) * 100.0;
            percentageChange = Math.round(percentageChange * 100.0) / 100.0;

            if (unitDifference > 0) {
                message = "Consumption increased by " + unitDifference + " units (" + percentageChange + "%).";
            } else if (unitDifference < 0) {
                message = "Consumption decreased by " + Math.abs(unitDifference) + " units (" + Math.abs(percentageChange) + "%).";
            } else {
                message = "Consumption stayed the same between " + month1 + " " + year1 + " and " + month2 + " " + year2 + ".";
            }
        }

        unitDifference = Math.round(unitDifference * 100.0) / 100.0;

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("month1", month1);
        comparison.put("year1", year1);
        comparison.put("month1Units", units1);
        comparison.put("month2", month2);
        comparison.put("year2", year2);
        comparison.put("month2Units", units2);
        comparison.put("unitDifference", unitDifference);
        comparison.put("percentageChange", percentageChange);
        comparison.put("message", message);

        return comparison;
    }

    // Predefined common appliances and their default power ratings in Watts
    private static final Map<String, Double> COMMON_APPLIANCES = new LinkedHashMap<>();

    static {
        COMMON_APPLIANCES.put("LED bulb", 9.0);
        COMMON_APPLIANCES.put("Ceiling fan", 75.0);
        COMMON_APPLIANCES.put("Television", 100.0);
        COMMON_APPLIANCES.put("Refrigerator", 150.0);
        COMMON_APPLIANCES.put("Air conditioner", 1500.0);
        COMMON_APPLIANCES.put("Washing machine", 500.0);
    }

    // Return the list of predefined common appliances
    public Map<String, Double> getCommonAppliances() {
        return COMMON_APPLIANCES;
    }

    // Find default rating for common appliance by name (case-insensitive)
    private Double findDefaultRating(String applianceName) {
        if (applianceName == null) return null;
        for (Map.Entry<String, Double> entry : COMMON_APPLIANCES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(applianceName.trim())) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Calculate appliance-wise electricity consumption
    public Map<String, Object> calculateApplianceConsumption(
            String applianceName,
            Double powerRatingWatts,
            Double dailyUsageHours,
            Double electricityRate) {

        // 1. Validation: Appliance name must not be blank
        if (applianceName == null || applianceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Appliance name must not be blank.");
        }
        String trimmedName = applianceName.trim();

        // 2. Validation: Daily usage must be between 0 and 24 hours
        if (dailyUsageHours == null) {
            throw new IllegalArgumentException("Daily usage hours is required.");
        }
        if (dailyUsageHours < 0 || dailyUsageHours > 24) {
            throw new IllegalArgumentException("Daily usage must be between 0 and 24 hours.");
        }

        // 3. Validation: Electricity rate must not be negative
        if (electricityRate == null) {
            throw new IllegalArgumentException("Electricity rate is required.");
        }
        if (electricityRate < 0) {
            throw new IllegalArgumentException("Electricity rate must not be negative.");
        }

        // Determine appliance type and effective power rating
        Double defaultRating = findDefaultRating(trimmedName);
        String applianceType;
        double effectivePowerRating;

        if (defaultRating != null) {
            applianceType = "Common";
            // For common appliances, use default rating unless user edited it
            if (powerRatingWatts != null) {
                if (powerRatingWatts < 0) {
                    throw new IllegalArgumentException("Power rating must not be negative.");
                }
                effectivePowerRating = powerRatingWatts;
            } else {
                effectivePowerRating = defaultRating;
            }
        } else {
            applianceType = "Custom";
            if (powerRatingWatts == null) {
                throw new IllegalArgumentException("Power rating in watts is required for custom appliance.");
            }
            if (powerRatingWatts < 0) {
                throw new IllegalArgumentException("Power rating must not be negative.");
            }
            effectivePowerRating = powerRatingWatts;
        }

        // 4. Calculations
        double dailyConsumptionKwh = (effectivePowerRating * dailyUsageHours) / 1000.0;
        double monthlyConsumptionKwh = dailyConsumptionKwh * 30.0;
        double estimatedMonthlyCost = monthlyConsumptionKwh * electricityRate;

        // Round results cleanly
        dailyConsumptionKwh = Math.round(dailyConsumptionKwh * 1000.0) / 1000.0;
        monthlyConsumptionKwh = Math.round(monthlyConsumptionKwh * 100.0) / 100.0;
        estimatedMonthlyCost = Math.round(estimatedMonthlyCost * 100.0) / 100.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applianceName", trimmedName);
        result.put("applianceType", applianceType);
        result.put("powerRatingWatts", effectivePowerRating);
        result.put("dailyUsageHours", dailyUsageHours);
        result.put("electricityRate", electricityRate);
        result.put("dailyConsumptionKwh", dailyConsumptionKwh);
        result.put("monthlyConsumptionKwh", monthlyConsumptionKwh);
        result.put("estimatedMonthlyCost", estimatedMonthlyCost);

        return result;
    }

    // Rule-based electricity saving recommendations based on monthly bills
    public Map<String, Object> getRecommendations() {
        List<ElectricityBill> bills = new ArrayList<>(billRepository.findAll());

        // Check if fewer than two bills exist
        if (bills.size() < 2) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "INSUFFICIENT_DATA");
            result.put("totalBillsFound", bills.size());
            result.put("message", "More monthly bill data is needed for comparison. Please add at least two monthly bills.");
            return result;
        }

        // Sort bills chronologically by year and month number
        bills.sort((b1, b2) -> {
            int y1 = (b1.getYear() != null) ? b1.getYear() : 0;
            int y2 = (b2.getYear() != null) ? b2.getYear() : 0;
            int yearComparison = Integer.compare(y1, y2);
            if (yearComparison != 0) {
                return yearComparison;
            }
            int m1 = getMonthNumber(b1.getMonth());
            int m2 = getMonthNumber(b2.getMonth());
            int monthComparison = Integer.compare(m1, m2);
            if (monthComparison != 0) {
                return monthComparison;
            }
            Long id1 = (b1.getId() != null) ? b1.getId() : 0L;
            Long id2 = (b2.getId() != null) ? b2.getId() : 0L;
            return Long.compare(id1, id2);
        });

        // Get the two most recent bills
        ElectricityBill previousBill = bills.get(bills.size() - 2);
        ElectricityBill currentBill = bills.get(bills.size() - 1);

        double previousUnits = (previousBill.getUnitsConsumed() != null) ? previousBill.getUnitsConsumed() : 0.0;
        double currentUnits = (currentBill.getUnitsConsumed() != null) ? currentBill.getUnitsConsumed() : 0.0;
        double unitDifference = currentUnits - previousUnits;

        double percentageChange;
        if (previousUnits == 0.0) {
            percentageChange = (currentUnits == 0.0) ? 0.0 : 100.0;
        } else {
            percentageChange = ((currentUnits - previousUnits) / previousUnits) * 100.0;
        }

        // Round values cleanly to 2 decimal places
        unitDifference = Math.round(unitDifference * 100.0) / 100.0;
        percentageChange = Math.round(percentageChange * 100.0) / 100.0;

        String recommendation;
        String consumptionTrend;

        // Rule-based recommendation conditions
        if (percentageChange > 20.0) {
            consumptionTrend = "HIGH_INCREASE";
            recommendation = "High Consumption Alert: Your electricity consumption increased by " + percentageChange
                    + "%. We recommend reducing unnecessary appliance usage, such as limiting air conditioner run-time, unplugging idle electronic devices, and using heavy appliances during off-peak hours.";
        } else if (percentageChange >= 1.0 && percentageChange <= 20.0) {
            consumptionTrend = "MODERATE_INCREASE";
            recommendation = "Moderate Increase Notice: Your electricity consumption increased slightly by " + percentageChange
                    + "%. We suggest checking your daily appliance usage, turning off unused lights and fans, and monitoring appliance run-hours to prevent further rise in your bill.";
        } else if (percentageChange < 0.0) {
            consumptionTrend = "DECREASED";
            recommendation = "Great Job! Your electricity consumption decreased by " + Math.abs(unitDifference)
                    + " units (" + Math.abs(percentageChange) + "%). Keep up your energy-saving habits and continue switching off appliances when not in use.";
        } else {
            // percentageChange >= 0.0 && percentageChange < 1.0 (consumption stayed the same)
            consumptionTrend = "STABLE";
            recommendation = "Consistent Consumption: Your electricity usage stayed approximately the same. Simple ways to reduce usage include switching to energy-efficient LED bulbs, cleaning AC and refrigerator coils, and utilizing natural daylight during the day.";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("previousMonth", previousBill.getMonth() + " " + previousBill.getYear());
        response.put("previousUnits", previousUnits);
        response.put("currentMonth", currentBill.getMonth() + " " + currentBill.getYear());
        response.put("currentUnits", currentUnits);
        response.put("unitDifference", unitDifference);
        response.put("percentageChange", percentageChange);
        response.put("consumptionTrend", consumptionTrend);
        response.put("recommendation", recommendation);

        return response;
    }

    // Helper method to convert month name to number (1-12) for chronological sorting
    private int getMonthNumber(String month) {
        if (month == null) return 0;
        switch (month.trim().toLowerCase()) {
            case "january": case "jan": return 1;
            case "february": case "feb": return 2;
            case "march": case "mar": return 3;
            case "april": case "apr": return 4;
            case "may": return 5;
            case "june": case "jun": return 6;
            case "july": case "jul": return 7;
            case "august": case "aug": return 8;
            case "september": case "sep": case "sept": return 9;
            case "october": case "oct": return 10;
            case "november": case "nov": return 11;
            case "december": case "dec": return 12;
            default: return 0;
        }
    }
}

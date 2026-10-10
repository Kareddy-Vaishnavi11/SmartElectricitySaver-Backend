package jar;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bills")
public class ElectricityBillController {

    private final ElectricityBillService billService;

    // Constructor injection
    public ElectricityBillController(ElectricityBillService billService) {
        this.billService = billService;
    }

    // POST /api/bills : Save a new electricity bill
    @PostMapping
    public ResponseEntity<ElectricityBill> saveBill(@RequestBody ElectricityBill bill) {
        ElectricityBill savedBill = billService.saveBill(bill);
        return new ResponseEntity<>(savedBill, HttpStatus.CREATED);
    }

    // GET /api/bills : Return all electricity bills
    @GetMapping
    public ResponseEntity<List<ElectricityBill>> getAllBills() {
        List<ElectricityBill> bills = billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    // GET /api/bills/{id} : Return one bill by ID
    @GetMapping("/{id}")
    public ResponseEntity<ElectricityBill> getBillById(@PathVariable Long id) {
        ElectricityBill bill = billService.getBillById(id);
        return ResponseEntity.ok(bill);
    }

    // DELETE /api/bills/{id} : Delete a bill by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBill(@PathVariable Long id) {
        billService.deleteBill(id);
        return ResponseEntity.ok("Electricity bill deleted successfully with id: " + id);
    }

    // PUT /api/bills/{id} : Update an existing bill by ID
    @PutMapping("/{id}")
    public ResponseEntity<ElectricityBill> updateBill(
            @PathVariable Long id,
            @RequestBody ElectricityBill bill) {
        bill.setId(id);
        ElectricityBill updated = billService.saveBill(bill);
        return ResponseEntity.ok(updated);
    }

    // GET /api/bills/compare?month1=September&year1=2026&month2=October&year2=2026
    @GetMapping("/compare")
    public ResponseEntity<?> compareConsumption(
            @RequestParam String month1,
            @RequestParam Integer year1,
            @RequestParam String month2,
            @RequestParam Integer year2) {
        try {
            Map<String, Object> comparison = billService.compareConsumption(month1, year1, month2, year2);
            return ResponseEntity.ok(comparison);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // GET /api/bills/appliances : Get list of common appliances with default power ratings
    @GetMapping("/appliances")
    public ResponseEntity<Map<String, Double>> getCommonAppliances() {
        return ResponseEntity.ok(billService.getCommonAppliances());
    }

    // POST /api/bills/calculate-appliance : Calculate appliance-wise electricity consumption
    @PostMapping({"/calculate-appliance", "/appliance-calculator"})
    public ResponseEntity<?> calculateAppliance(@RequestBody Map<String, Object> request) {
        try {
            String applianceName = request.get("applianceName") != null ? request.get("applianceName").toString() : null;
            Double powerRatingWatts = parseDouble(request.get("powerRatingWatts"));
            Double dailyUsageHours = parseDouble(request.get("dailyUsageHours"));
            Double electricityRate = parseDouble(request.get("electricityRate"));

            Map<String, Object> result = billService.calculateApplianceConsumption(
                    applianceName, powerRatingWatts, dailyUsageHours, electricityRate);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // GET /api/bills/recommendations : Get electricity-saving recommendations based on bills
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations() {
        Map<String, Object> recommendations = billService.getRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    // Helper method to safely parse numeric values from request payload
    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + val);
        }
    }
}

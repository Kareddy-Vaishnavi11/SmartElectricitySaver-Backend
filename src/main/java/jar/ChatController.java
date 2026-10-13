package jar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ElectricityBillService billService;

    @Value("${ai.api.key:}")
    private String apiKey;

    public ChatController(ElectricityBillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, Object> payload) {
        String userMessage = (String) payload.getOrDefault("message", "");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty."));
        }

        String lower = userMessage.trim().toLowerCase();
        String reply;

        // Context-aware electricity domain intelligence + RAG from bills database
        List<ElectricityBill> bills = billService.getAllBills();
        int totalBills = bills.size();

        if (lower.contains("bill") && (lower.contains("how many") || lower.contains("total") || lower.contains("count"))) {
            reply = "You currently have " + totalBills + " electricity bill(s) recorded in the database.";
        } else if (lower.contains("latest") || lower.contains("recent") || lower.contains("last month")) {
            if (bills.isEmpty()) {
                reply = "No bills have been recorded yet. You can add one via the 'Add Bill' page!";
            } else {
                ElectricityBill latest = bills.get(bills.size() - 1);
                reply = "Your latest recorded bill is for " + latest.getMonth() + " " + latest.getYear() + 
                        " with " + latest.getUnitsConsumed() + " kWh units and a total of ₹" + latest.getBillAmount() + ".";
            }
        } else if (lower.contains("ac") || lower.contains("air conditioner")) {
            reply = "Air Conditioners typically consume 1500W to 2000W (~1.5 to 2.0 kWh per hour). Setting your AC temperature to 24°C–26°C can reduce energy consumption by up to 24%!";
        } else if (lower.contains("refrigerator") || lower.contains("fridge")) {
            reply = "Refrigerators consume around 150W to 200W continuously. Keeping coils clean and doors sealed properly saves substantial electricity.";
        } else if (lower.contains("reduce") || lower.contains("save") || lower.contains("tips")) {
            reply = "Top energy-saving recommendations:\n" +
                    "1. Replace fluorescent tubes with LED bulbs (saves ~80% lighting energy).\n" +
                    "2. Run air conditioners at 24°C instead of 18°C.\n" +
                    "3. Turn off electronics at the wall switch to eliminate vampire power.\n" +
                    "4. Clean air filters and refrigerator condenser coils regularly.";
        } else if (lower.contains("appliance") || lower.contains("highest") || lower.contains("most power")) {
            reply = "Air Conditioners, geysers/water heaters, and heavy washing machines use the most power. Check out our 'Appliance Calculator' tab to calculate individual costs!";
        } else {
            reply = "I'm your Smart Electricity AI assistant! I can help you analyze your bills, estimate appliance power consumption, and recommend tips to lower your monthly electricity expenses. Try asking: 'What was my latest bill?' or 'How can I save electricity?'";
        }

        Map<String, String> response = new HashMap<>();
        response.put("reply", reply);
        return ResponseEntity.ok(response);
    }
}

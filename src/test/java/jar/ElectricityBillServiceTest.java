package jar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElectricityBillServiceTest {

    @Mock
    private ElectricityBillRepository billRepository;

    @InjectMocks
    private ElectricityBillService billService;

    private ElectricityBill bill1;
    private ElectricityBill bill2;

    @BeforeEach
    void setUp() {
        bill1 = new ElectricityBill("September", 2026, 150.0, 1050.0);
        bill1.setId(1L);
        bill2 = new ElectricityBill("October", 2026, 180.0, 1260.0);
        bill2.setId(2L);
    }

    @Test
    void testGetAllBills() {
        when(billRepository.findAll()).thenReturn(Arrays.asList(bill1, bill2));

        List<ElectricityBill> result = billService.getAllBills();

        assertEquals(2, result.size());
        verify(billRepository, times(1)).findAll();
    }

    @Test
    void testGetBillById_Success() {
        when(billRepository.findById(1L)).thenReturn(Optional.of(bill1));

        ElectricityBill result = billService.getBillById(1L);

        assertNotNull(result);
        assertEquals("September", result.getMonth());
    }

    @Test
    void testCalculateApplianceConsumption() {
        Map<String, Object> result = billService.calculateApplianceConsumption(
                "Air conditioner", 1500.0, 4.0, 7.0);

        assertNotNull(result);
        assertEquals("Air conditioner", result.get("applianceName"));
        assertEquals(6.0, (Double) result.get("dailyConsumptionKwh"), 0.01);
        assertEquals(180.0, (Double) result.get("monthlyConsumptionKwh"), 0.01);
        assertEquals(1260.0, (Double) result.get("estimatedMonthlyCost"), 0.01);
    }
}

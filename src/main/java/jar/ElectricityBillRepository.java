package jar;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElectricityBillRepository extends JpaRepository<ElectricityBill, Long> {
    // Custom finder method to locate a bill by month (case-insensitive) and year
    Optional<ElectricityBill> findByMonthIgnoreCaseAndYear(String month, Integer year);
}

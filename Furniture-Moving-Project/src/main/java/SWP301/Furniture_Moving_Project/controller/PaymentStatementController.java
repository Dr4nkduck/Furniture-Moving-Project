package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.BankTransactionDTO;
import SWP301.Furniture_Moving_Project.service.PaymentReconcileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment/statement")
public class PaymentStatementController {

    private final PaymentReconcileService paymentReconcileService;

    public PaymentStatementController(PaymentReconcileService paymentReconcileService) {
        this.paymentReconcileService = paymentReconcileService;
    }

    /**
     * Nhận danh sách giao dịch ngân hàng và tự động đối soát
     */
    @PostMapping("/import")
    public ResponseEntity<String> importStatement(@RequestBody List<BankTransactionDTO> txns) {
        paymentReconcileService.reconcile(txns);
        return ResponseEntity.ok("Imported & reconciled " + txns.size() + " transactions");
    }
}

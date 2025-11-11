package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;

public interface PaymentService {
    PaymentInitResponse initPayment(Integer serviceRequestId);
    PaymentStatusResponse getPaymentStatus(Integer serviceRequestId);
}

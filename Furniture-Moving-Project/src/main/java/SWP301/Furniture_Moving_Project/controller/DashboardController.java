package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/operationadmin/dashboard")
    public String getDashboardPage() {
        return "operationadmin/dashboard"; // looks for src/main/resources/templates/admin/dashboard.html
    }

//    @GetMapping("/operationadmin/orders")
//    public String getOrdersPage() {
//        return "operationadmin/orders"; // src/main/resources/templates/admin/orders.html
//    }
//
//    @GetMapping("/operationadmin/products")
//    public String getProductsPage() {
//        return "operationadmin/products"; // src/main/resources/templates/admin/products.html
//    }
}

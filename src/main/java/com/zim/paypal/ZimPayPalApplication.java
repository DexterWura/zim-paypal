package com.zim.paypal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Zim PayPal Clone
 * 
 * @author Zim Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class ZimPayPalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZimPayPalApplication.class, args);
    }
}


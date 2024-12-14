package com.pgeasy.config;

import com.pgeasy.www.PgPaymentService;
import com.pgeasy.www.PgPaymentServiceImpl;

/**
 * @author Theo
 * @since 2024/12/14
 */
@COnfiguration
public class TossPaymentPgEasy {

    @Bean
    public PgPaymentService pgPaymentService(
            @Value("${pg.payment.secretKey}") String secretKey,
            @Value("${pg.payment.timeout}") int timeout
    ) {
        return new PgPaymentServiceImpl()
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .configure("secretKey", secretKey);
    }
}

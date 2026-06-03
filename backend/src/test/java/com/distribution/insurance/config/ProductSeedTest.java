package com.distribution.insurance.config;

import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProductSeedTest {

    @Test
    void 상품이_없으면_시드한다() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(productRepo.count()).thenReturn(0L);

        new DataSeeder(userRepo, new BCryptPasswordEncoder(), productRepo).run();

        verify(productRepo, atLeast(1)).save(any());
    }

    @Test
    void 상품이_이미_있으면_시드하지_않는다() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(productRepo.count()).thenReturn(6L);

        new DataSeeder(userRepo, new BCryptPasswordEncoder(), productRepo).run();

        verify(productRepo, never()).save(any());
    }
}

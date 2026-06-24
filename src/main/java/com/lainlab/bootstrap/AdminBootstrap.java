package com.lainlab.bootstrap;

import com.lainlab.controller.TokenAdminController;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;

@Singleton
public class AdminBootstrap implements ApplicationEventListener<ServerStartupEvent> {

    @Inject
    TokenRepository tokens;

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {

        // Проверяем, есть ли хотя бы один админ
        long adminCount = tokens.countAdmins();
        if (adminCount > 0) {
            return;
        }

        // Генерируем токен
        String tokenValue = TokenAdminController.generateApiKey(true);

        Token admin = new Token();
        admin.setToken(tokenValue);
        admin.setLicenseNo(0);
        admin.setLicenseOrg("Admin");
        admin.setEmail("root@localhost");
        admin.setAdmin(true);
        admin.setActive(true);
        admin.setBalance(0);
        admin.setTotal(0);
        admin.setCreatedAt(LocalDateTime.now());

        tokens.save(admin);

        System.out.println("===============================================");
        System.out.println(" FIRST ADMIN CREATED ");
        System.out.println(" ADMIN TOKEN: " + tokenValue);
        System.out.println(" SAVE THIS TOKEN — IT WILL NOT BE SHOWN AGAIN ");
        System.out.println("===============================================");
    }
}

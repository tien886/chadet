package com.tienvm.trade.repository;

import java.util.UUID;

import com.tienvm.trade.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {
}

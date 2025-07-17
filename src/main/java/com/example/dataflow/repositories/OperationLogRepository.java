package com.example.dataflow.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, UUID> {
}
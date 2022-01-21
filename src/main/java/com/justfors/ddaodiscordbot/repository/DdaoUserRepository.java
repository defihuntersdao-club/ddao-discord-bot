package com.justfors.ddaodiscordbot.repository;

import com.justfors.ddaodiscordbot.model.DdaoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DdaoUserRepository extends JpaRepository<DdaoUser, Long> {

	@Query("select e from DdaoUser e where e.discordId = :discrodId")
	DdaoUser getByDiscrodID(Long discrodId);
}

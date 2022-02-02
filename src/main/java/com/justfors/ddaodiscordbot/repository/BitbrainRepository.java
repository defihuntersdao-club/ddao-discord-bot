package com.justfors.ddaodiscordbot.repository;

import com.justfors.ddaodiscordbot.model.Bitbrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BitbrainRepository extends JpaRepository<Bitbrain, Long> {

	@Query("select b from Bitbrain b where b.code = :code")
	Bitbrain findByCode(String code);

	@Modifying
	@Query("update Bitbrain b set b.status = 1 where b.id = :bitbrainId")
	void exprireStatus(Long bitbrainId);
}

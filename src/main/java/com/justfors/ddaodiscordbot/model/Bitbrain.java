package com.justfors.ddaodiscordbot.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bitbrain")
@Getter
@Setter
public class Bitbrain {

	@Id
	@GeneratedValue
	private long id;

	private String code;
	private long status;

}

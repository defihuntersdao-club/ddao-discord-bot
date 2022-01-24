package com.justfors.ddaodiscordbot.controller;

import com.justfors.ddaodiscordbot.service.DiscordService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RequestMapping("/discord")
@RestController
@AllArgsConstructor
public class DiscordController {

	private final DiscordService discordService;

	@GetMapping("/tg/bind/{id}")
	public ResponseEntity bindTG(@PathVariable Long id) {
		discordService.sendTGBindMessage(id);
		return ResponseEntity.ok().build();
	}

}

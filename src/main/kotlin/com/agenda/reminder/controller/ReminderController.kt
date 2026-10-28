package com.agenda.reminder.controller

import com.agenda.reminder.DTO.CreateReminderRequest
import com.agenda.reminder.DTO.ReminderResponse
import com.agenda.reminder.DTO.UpdateReminderRequest
import com.agenda.reminder.model.ReminderStatus
import com.agenda.reminder.service.ReminderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reminders")
class ReminderController(private val service: ReminderService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateReminderRequest, authentication: Authentication): ReminderResponse =
        ReminderResponse.from(service.create(request.toEntity(), authentication.name))

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "ACTIVE") status: ReminderStatus,
        authentication: Authentication,
    ): List<ReminderResponse> = service.list(status, authentication.name).map(ReminderResponse::from)

    @PatchMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun complete(@PathVariable id: String, authentication: Authentication) = service.complete(id, authentication.name)

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReminderRequest,
        authentication: Authentication,
    ): ReminderResponse = ReminderResponse.from(service.update(id, request, authentication.name))
}

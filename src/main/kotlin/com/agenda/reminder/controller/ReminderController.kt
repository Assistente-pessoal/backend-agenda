package com.agenda.reminder.controller

import com.agenda.reminder.DTO.CreateReminderRequest
import com.agenda.reminder.DTO.ReminderResponse
import com.agenda.reminder.DTO.UpdateReminderRequest
import com.agenda.reminder.model.ReminderStatus
import com.agenda.reminder.service.ReminderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reminders")
class ReminderController(
    private val service: ReminderService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateReminderRequest): ReminderResponse =
        ReminderResponse.from(service.create(request.toEntity()))
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "ACTIVE") status: ReminderStatus
    ): List<ReminderResponse> =
        service.list(status).map { ReminderResponse.from(it) }
    @PatchMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun complete(@PathVariable id: String) = service.complete(id)

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReminderRequest
    ): ReminderResponse = ReminderResponse.from(service.update(id, request))
}

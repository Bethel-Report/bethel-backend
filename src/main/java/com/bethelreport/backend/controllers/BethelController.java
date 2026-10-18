
package com.bethelreport.backend.controllers;

import com.bethelreport.backend.dtos.bethel.CreateBethelRequest;
import com.bethelreport.backend.dtos.bethel.UpdateBethelRequest;
import com.bethelreport.backend.dtos.bethel.BethelResponse;
import com.bethelreport.backend.services.BethelService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bethels")
public class BethelController {

    private final BethelService bethelService;

    public BethelController(BethelService bethelService) {
        this.bethelService = bethelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BethelResponse create(
            @Valid @RequestBody CreateBethelRequest request) {

        return bethelService.create(request);
    }

    @GetMapping
    public List<BethelResponse> findAll(
            @RequestParam(required = false) String name) {

        return bethelService.findAll(name);
    }

    @GetMapping("/{id}")
    public BethelResponse findById(@PathVariable Long id) {
        return bethelService.findById(id);
    }

    @PutMapping("/{id}")
    public BethelResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBethelRequest request) {

        return bethelService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public BethelResponse activate(@PathVariable Long id) {
        return bethelService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public BethelResponse deactivate(@PathVariable Long id) {
        return bethelService.deactivate(id);
    }
}

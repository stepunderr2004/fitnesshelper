package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.DiaryEntry;
import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.repository.DiaryEntryRepository;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/diary-entries")
public class DiaryEntryController {

    private final DiaryEntryRepository repo;
    private final UserService userService;

    public DiaryEntryController(DiaryEntryRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @GetMapping
    public List<DiaryEntry> getAll(HttpServletRequest request) {
        return repo.findByUserIdOrderByDateDesc(getUserId(request));
    }

    @PostMapping
    public DiaryEntry create(@RequestBody DiaryEntry entry, HttpServletRequest request) {
        entry.setUser(userService.findById(getUserId(request)));
        return repo.save(entry);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id, HttpServletRequest request) {
        DiaryEntry e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (e.getUser().getId() != getUserId(request)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        repo.delete(e);
    }

    private int getUserId(HttpServletRequest request) {
        return (int) request.getAttribute("userId");
    }
}
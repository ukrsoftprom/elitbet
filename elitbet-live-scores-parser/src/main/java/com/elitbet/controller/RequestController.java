package com.elitbet.controller;

import com.elitbet.model.Request;
import com.elitbet.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class RequestController {

    @Autowired
    RequestService requestService;

    @PostMapping
    public ResponseEntity updateRequestFixtures(@RequestBody List<Request> requestList) throws Exception {
        requestService.updateRequestList(requestList);
        return ResponseEntity.ok("Success");
    }

}

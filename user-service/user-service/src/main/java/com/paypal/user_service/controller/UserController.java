package com.paypal.user_service.controller;

import com.paypal.user_service.entity.User;
import com.paypal.user_service.service.UserService;
import com.paypal.user_service.service.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @PostMapping
    public ResponseEntity<User>createUser(@RequestBody User user){
       User savedUser=userService.createUser(user);
       return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    @GetMapping("/{id}")
    public ResponseEntity<User>getUserById(@PathVariable Long id){
        return userService.getUserById((id)).map(ResponseEntity::ok) // if we found id return status OK
                .orElse(ResponseEntity.notFound().build());
    }

   @GetMapping
    public ResponseEntity<List<User>>getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
   }
}

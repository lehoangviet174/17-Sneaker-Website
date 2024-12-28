package com.vuhien.application.controller.admin;

import com.vuhien.application.entity.User;
import com.vuhien.application.model.request.CreateUserRequest;
import com.vuhien.application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/admin/users")
    public String homePages(Model model,
                            @RequestParam(defaultValue = "", required = false) String fullName,
                            @RequestParam(defaultValue = "", required = false) String phone,
                            @RequestParam(defaultValue = "", required = false) String email,
                            @RequestParam(defaultValue = "", required = false) String address,
                            @RequestParam(defaultValue = "1", required = false) Integer page) {
        Page<User> users = userService.adminListUserPages(fullName, phone, email, page);
        model.addAttribute("users", users.getContent());
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("currentPage", users.getPageable().getPageNumber() + 1);
        return "admin/user/list";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam("fullName") String fullName,
                             @RequestParam("email") String email,
                             @RequestParam("phone") String phone,
                             @RequestParam("address") String address){
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setFullName(fullName);
        createUserRequest.setEmail(email);
        createUserRequest.setPhone(phone);
        createUserRequest.setAddress(address);
//        default password when create user
        createUserRequest.setPassword("123456");
        userService.createUser(createUserRequest);
        return "redirect:/admin/users";
    }

    @GetMapping("/api/admin/users/list")
    public ResponseEntity<Object> getListUserPages(@RequestParam(defaultValue = "", required = false) String fullName,
                                                   @RequestParam(defaultValue = "", required = false) String phone,
                                                   @RequestParam(defaultValue = "", required = false) String email,
                                                   @RequestParam(defaultValue = "", required = false) String address,
                                                   @RequestParam(defaultValue = "1", required = false) Integer page) {
        Page<User> users = userService.adminListUserPages(fullName, phone, email, page);
        return ResponseEntity.ok(users);
    }
}
